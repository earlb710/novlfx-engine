package com.eb.javafx.assets;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Loads assets in the background, best first, until the heap reaches a limit — and gets out of the way
 * the moment the game needs an asset itself.
 *
 * <h2>What this owns, and what it doesn't</h2>
 * The engine owns the <b>mechanism</b>: a low-priority worker, the pause/resume protocol, the heap
 * ceiling, and the progress hook. It knows nothing about what an asset IS or how one is ordered — a game
 * supplies both through {@link Source}. That split is the point: every game wants "warm the expensive
 * things while the player reads a menu, and yield instantly when something is actually needed", and no
 * two games agree on what the expensive things are or how to rank them.
 *
 * <h2>Yielding</h2>
 * {@link #pause()} stops the pass from STARTING further assets; it does not interrupt one in flight,
 * because a half-loaded asset is not something that can be safely abandoned. The foreground therefore
 * waits at most one asset rather than the remaining queue. {@link #runExclusively} wraps the whole
 * pattern and is the right entry point for "the game needs this NOW".
 *
 * <p>Pausing matters even when the underlying loader is itself synchronised: without it, a foreground
 * load finishing one asset immediately queues behind the next one this worker starts, so the game keeps
 * losing races it should win.</p>
 *
 * <h2>Stopping on memory</h2>
 * Reaching the ceiling is <b>not</b> terminal. The position is kept, so {@link #resume()} re-checks the
 * heap and carries on if memory has since been freed. Memory frees constantly as scenes change, and a
 * pass that gave up permanently the first time a spike crossed the line would leave most of the library
 * uncached for the rest of the session.
 *
 * <p>Heap readings include uncollected garbage, so a raw reading over the line does not mean the heap is
 * genuinely full. Before stopping, the service asks for a collection and re-reads — without that, a pass
 * reliably stops early having measured litter rather than live data.</p>
 *
 * <p>All public methods are safe from any thread. The worker is a daemon, so it never holds up exit.</p>
 */
public final class AssetPrecacheService {

    /**
     * The assets to warm, in the order they should be warmed — supplied by the game.
     *
     * <p>Index-based rather than an iterator or a stream because the service must be able to STOP and
     * RESUME at a position: a paused or memory-halted pass keeps its cursor and continues from it, which
     * a consumed iterator cannot express.</p>
     */
    public interface Source {

        /** How many assets there are to warm. */
        int size();

        /**
         * Loads the asset at {@code index} into whatever cache the game keeps.
         *
         * <p>Called on the pre-cache worker, never with the service's own lock held, so it may block for
         * as long as loading takes. Must not throw — a failure should be swallowed and reported as
         * {@code 0}, since one bad asset must not end the pass.</p>
         *
         * @return bytes loaded, or {@code 0} when the asset was skipped or failed
         */
        long loadOne(int index);
    }

    /** What the pass is doing. */
    public enum State {
        /** Not started yet. */                                      IDLE,
        /** Loading assets. */                                       RUNNING,
        /** Held off so the game can load something itself. */       PAUSED,
        /** Stopped at the heap ceiling; resumable if memory frees. */ HALTED_MEMORY,
        /** Every asset has been warmed. */                          COMPLETE,
        /** Shut down for good. */                                   STOPPED
    }

    /** Default ceiling: how full the heap may get before the pass stops adding to it. */
    public static final int DEFAULT_HEAP_LIMIT_PCT = 80;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition resumed = lock.newCondition();
    private final int heapLimitPct;
    private final String threadName;

    private Thread worker;
    private State state = State.IDLE;
    /** How deep into the order the pass has got — kept across pauses AND memory halts. */
    private int cursor;
    private int loaded;
    private long loadedBytes;
    /** Nested {@link #runExclusively} calls must not let the inner one resume the outer's pause. */
    private int pauseDepth;
    private Runnable progressListener;

    /** A service with the default {@value #DEFAULT_HEAP_LIMIT_PCT}% ceiling. */
    public AssetPrecacheService(String threadName) {
        this(threadName, DEFAULT_HEAP_LIMIT_PCT);
    }

    /**
     * @param threadName   name for the worker thread, so it's identifiable in a profiler or thread dump
     * @param heapLimitPct how full the heap may get before the pass stops, 1–99
     */
    public AssetPrecacheService(String threadName, int heapLimitPct) {
        this.threadName = threadName == null || threadName.isBlank() ? "asset-precache" : threadName;
        this.heapLimitPct = Math.max(1, Math.min(99, heapLimitPct));
    }

    /**
     * Starts the pass over {@code source}.
     *
     * <p>Idempotent: does nothing while a pass is running, or once it has completed or stopped, so it
     * can be wired to a screen that may be opened more than once without starting a second worker
     * competing with the first.</p>
     */
    public void start(Source source) {
        if (source == null) {
            return;
        }
        lock.lock();
        try {
            if ((worker != null && worker.isAlive()) || state == State.COMPLETE || state == State.STOPPED) {
                return;
            }
            state = State.RUNNING;
            worker = new Thread(() -> run(source), threadName);
            worker.setDaemon(true);
            // Lowest priority: this is opportunistic work and must never win CPU against rendering.
            // A stutter caused by pre-caching would defeat the entire point of pre-caching.
            worker.setPriority(Thread.MIN_PRIORITY);
            worker.start();
        } finally {
            lock.unlock();
        }
    }

    /** Stops the pass from starting further assets. Returns once no NEW asset will begin. */
    public void pause() {
        lock.lock();
        try {
            pauseDepth++;
            if (state == State.RUNNING) {
                state = State.PAUSED;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Lets the pass continue, re-checking the heap first — if it is still over the ceiling the pass
     * stays halted rather than immediately allocating past it again.
     */
    public void resume() {
        lock.lock();
        try {
            if (pauseDepth > 0) {
                pauseDepth--;
            }
            if (pauseDepth > 0) {
                return;                       // an outer runExclusively still wants us held
            }
            if (state == State.PAUSED || state == State.HALTED_MEMORY) {
                state = State.RUNNING;
                resumed.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs {@code work} with the pass held off, then resumes — for when the game needs an asset
     * immediately and must not queue behind opportunistic loading.
     *
     * <p>The resume is in a {@code finally}, so work that throws still releases the pass. A leaked pause
     * would silently disable pre-caching for the rest of the session — the kind of failure nobody
     * notices until the game is mysteriously janky.</p>
     */
    public <T> T runExclusively(Supplier<T> work) {
        pause();
        try {
            return work.get();
        } finally {
            resume();
        }
    }

    /** {@link #runExclusively(Supplier)} for work with no result. */
    public void runExclusively(Runnable work) {
        runExclusively(() -> {
            work.run();
            return null;
        });
    }

    /** Shuts the pass down permanently. */
    public void stop() {
        lock.lock();
        try {
            state = State.STOPPED;
            resumed.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sets the callback run after EVERY asset — for a progress readout.
     *
     * <p>Invoked on the pre-cache worker, NOT the UI thread: a listener touching a scene graph must hop
     * across itself. It runs on a worker whose whole purpose is to stay out of the way, so it must be
     * cheap — anything slow here directly slows pre-caching.</p>
     */
    public void setProgressListener(Runnable listener) {
        lock.lock();
        try {
            progressListener = listener;
        } finally {
            lock.unlock();
        }
    }

    /** What the pass is currently doing. */
    public State state() {
        lock.lock();
        try {
            return state;
        } finally {
            lock.unlock();
        }
    }

    /** How many assets have been warmed. */
    public int loadedCount() {
        lock.lock();
        try {
            return loaded;
        } finally {
            lock.unlock();
        }
    }

    /** How many bytes have been warmed. */
    public long loadedBytes() {
        lock.lock();
        try {
            return loadedBytes;
        } finally {
            lock.unlock();
        }
    }

    /** The heap ceiling this service stops at, as a percentage. */
    public int heapLimitPct() {
        return heapLimitPct;
    }

    /** Live heap use as a percentage of the maximum the JVM may grow to. */
    public static double heapUsedPct() {
        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        if (max <= 0) {
            return 100.0;                     // unknown ceiling: assume full rather than fill it
        }
        return 100.0 * (rt.totalMemory() - rt.freeMemory()) / (double) max;
    }

    // ---------------------------------------------------------------------------------------------
    // The worker
    // ---------------------------------------------------------------------------------------------

    private void run(Source source) {
        int size;
        try {
            size = source.size();
        } catch (RuntimeException | Error sourceFailure) {
            setState(State.STOPPED);
            return;
        }
        while (true) {
            int index;
            lock.lock();
            try {
                // Park while paused. The heap is NOT checked here but after waking, so a long pause
                // during which memory filled up can't slip an asset through on a stale reading.
                while (state == State.PAUSED) {
                    resumed.awaitUninterruptibly();
                }
                if (state == State.STOPPED) {
                    return;
                }
                if (cursor >= size) {
                    state = State.COMPLETE;
                    return;
                }
                if (heapOverLimit()) {
                    // Hold rather than exit — memory frees as scenes change, and resume() re-checks.
                    state = State.HALTED_MEMORY;
                    while (state == State.HALTED_MEMORY) {
                        resumed.awaitUninterruptibly();
                    }
                    continue;                 // re-evaluate everything after waking
                }
                index = cursor++;
            } finally {
                lock.unlock();
            }

            // Loaded OUTSIDE the lock: this is the slow part, and holding the lock across it would make
            // pause() block for a whole asset load — exactly the stall it exists to prevent.
            long bytes;
            try {
                bytes = source.loadOne(index);
            } catch (RuntimeException | Error assetFailure) {
                bytes = 0;                    // one bad asset must not end the pass
            }

            Runnable listener;
            lock.lock();
            try {
                if (bytes > 0) {
                    loaded++;
                    loadedBytes += bytes;
                }
                listener = progressListener;
            } finally {
                lock.unlock();
            }
            // Notified OUTSIDE the lock: the listener typically hops to the UI thread, and calling it
            // under the service lock would let a UI update deadlock against a pause() from that thread.
            if (listener != null) {
                try {
                    listener.run();
                } catch (RuntimeException | Error listenerFailure) {
                    // A broken progress display must never stop the pre-cache.
                }
            }
        }
    }

    private void setState(State end) {
        lock.lock();
        try {
            state = end;
        } finally {
            lock.unlock();
        }
    }

    /**
     * True when the heap is genuinely at the ceiling.
     *
     * <p>Confirms with a collection hint before saying yes: {@code totalMemory - freeMemory} counts
     * unreclaimed garbage, so a first reading over the line usually means the heap merely needs
     * collecting. Treating that as "full" stops the pass after a handful of assets.</p>
     */
    private boolean heapOverLimit() {
        if (heapUsedPct() < heapLimitPct) {
            return false;
        }
        System.gc();                          // a hint, not a guarantee — the re-read is what decides
        return heapUsedPct() >= heapLimitPct;
    }
}
