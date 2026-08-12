package com.eb.javafx.diagnostics;

import java.util.function.Supplier;

/**
 * A tiny stopwatch for debugging timing issues.  Start it and it reports elapsed wall-clock time to the
 * engine log (stdout, teed into {@code log.txt} by {@link EngineLogFile}) under a {@code [Timer]} tag,
 * matching the {@code [Class] …} convention the rest of the engine logs with.
 *
 * <p>Three ways to use it:</p>
 * <pre>
 *   // 1. Scoped — logs "[Timer] build figure: 123 ms" when the block exits:
 *   try (EngineTimer t = EngineTimer.start("build figure")) {
 *       ...
 *   }
 *
 *   // 2. Manual — read elapsed as you go, log phase splits, stop when done:
 *   EngineTimer t = EngineTimer.start("load save");
 *   ...; t.lap("parsed json");
 *   ...; t.lap("applied sections");
 *   t.stop();
 *
 *   // 3. One-shot — wrap a block or an expression (the Supplier form returns the value):
 *   EngineTimer.time("warm caches", () -> warmCaches());
 *   var image = EngineTimer.time("decode", () -> decode(bytes));
 * </pre>
 *
 * <p>Diagnostics only.  {@link #setEnabled(boolean)} mutes ALL timer output at once (elapsed is still
 * measured and returned — nothing is printed), so probes can be left in the code and switched off; a
 * per-timer {@link #warnOver(long)} suppresses the line unless the run was slow enough to care about.
 * A single instance is not thread-safe (use one timer per thread); the global enable flag is volatile.</p>
 */
public final class EngineTimer implements AutoCloseable {

    private static volatile boolean enabled = true;

    private final String label;
    private final long startNanos;
    private long lastLapNanos;
    private long warnThresholdMillis = -1;   // -1 = always log
    private long stoppedNanos = -1;          // -1 while running; frozen end time once stopped

    private EngineTimer(String label) {
        this.label = label == null || label.isBlank() ? "timer" : label.trim();
        this.startNanos = System.nanoTime();
        this.lastLapNanos = this.startNanos;
    }

    /** Starts and returns a running timer labelled {@code label}. */
    public static EngineTimer start(String label) {
        return new EngineTimer(label);
    }

    /** Turns ALL timer logging on / off globally.  Measurement still works when off — only the printing
     *  is suppressed — so probes can be left in place and muted in normal runs. */
    public static void setEnabled(boolean on) {
        enabled = on;
    }

    /** Whether timer logging is currently enabled. */
    public static boolean isEnabled() {
        return enabled;
    }

    /** Elapsed since {@link #start} in whole milliseconds — live while running, frozen once stopped. */
    public long elapsedMillis() {
        return elapsedNanos() / 1_000_000L;
    }

    /** Elapsed since {@link #start} in nanoseconds — live while running, frozen once stopped. */
    public long elapsedNanos() {
        long end = stoppedNanos >= 0 ? stoppedNanos : System.nanoTime();
        return end - startNanos;
    }

    /** Only log on {@link #stop} / {@link #close} when the total run took at least {@code thresholdMillis}
     *  — for probes that are only interesting when something is slow.  Chainable. */
    public EngineTimer warnOver(long thresholdMillis) {
        this.warnThresholdMillis = thresholdMillis;
        return this;
    }

    /** Logs an intermediate split — total elapsed plus the delta since the previous lap / start — and
     *  marks a new lap point.  Returns this so calls can chain.  Records the lap even when logging is off. */
    public EngineTimer lap(String note) {
        long now = System.nanoTime();
        long total = (now - startNanos) / 1_000_000L;
        long delta = (now - lastLapNanos) / 1_000_000L;
        lastLapNanos = now;
        if (enabled) {
            System.out.println("[Timer] " + label
                    + (note == null || note.isBlank() ? "" : " · " + note.trim())
                    + ": " + total + " ms (+" + delta + " ms)");
        }
        return this;
    }

    /** Stops the timer and logs the total elapsed (once — later calls are no-ops), respecting the global
     *  enable flag and any {@link #warnOver} threshold.  Returns the elapsed milliseconds. */
    public long stop() {
        if (stoppedNanos >= 0) {
            return elapsedMillis();   // already stopped — return the frozen elapsed
        }
        stoppedNanos = System.nanoTime();
        long total = elapsedMillis();
        if (enabled && (warnThresholdMillis < 0 || total >= warnThresholdMillis)) {
            System.out.println("[Timer] " + label + ": " + total + " ms");
        }
        return total;
    }

    /** Equivalent to {@link #stop()} — supports try-with-resources. */
    @Override
    public void close() {
        stop();
    }

    /** Runs {@code body}, logging how long it took under {@code label}. */
    public static void time(String label, Runnable body) {
        EngineTimer t = start(label);
        try {
            body.run();
        } finally {
            t.stop();
        }
    }

    /** Runs {@code body}, logging how long it took under {@code label}, and returns its result. */
    public static <T> T time(String label, Supplier<T> body) {
        EngineTimer t = start(label);
        try {
            return body.get();
        } finally {
            t.stop();
        }
    }
}
