package com.eb.javafx.events;

import com.eb.javafx.util.Validation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Serial, completion-gated event runner that guarantees ordering when it has to.
 *
 * <p>Unlike {@link GameEventBus} (fire-and-forget) and {@link GameEventQueue} (drain-all FIFO), the
 * sequencer runs exactly ONE {@link SequencedEvent} at a time and does not start the next until the
 * current one signals {@link Control#complete()}. So an interactive event (a dialog or scene that
 * finishes across many frames and user inputs) still blocks everything behind it until it is truly
 * done — no "everything fires at once".</p>
 *
 * <p>Three ordering tools:</p>
 * <ul>
 *   <li>{@link #enqueue(SequencedEvent)} appends work in FIFO order.</li>
 *   <li>{@link Control#spawn(SequencedEvent)} inserts a "bonus"/sub-event that runs after the events
 *       already queued in the current phase and before the next barrier — breadth-first, so an event
 *       finishes (or plays out over time) and only then is its bonus handed out, rather than the
 *       bonus interrupting the rest of the phase. Bonuses spawned by those bonuses queue after them,
 *       generation by generation.</li>
 *   <li>{@link #enqueueBarrier(Runnable)} marks a phase boundary; the barrier's action runs only
 *       once everything ahead of it (including spawned bonus events) has completed.</li>
 * </ul>
 *
 * <p>Assemble a phase with {@link #enqueue}/{@link #enqueueBarrier} and then call {@link #run()} to
 * process it — enqueue is lazy so the whole phase is present before ordering is decided (that is what
 * makes bonus placement breadth-first rather than eager). Once running, the completion of an
 * in-progress event, and any enqueues a barrier action performs, continue processing automatically.</p>
 *
 * <p>The turn/bonus-event use: at end of turn, enqueue the turn's events (which may
 * {@link Control#spawn spawn} bonus events), then {@code enqueueBarrier(startNextTurn)}, then
 * {@link #run()}. Because every bonus event queues before the barrier, the next turn cannot begin
 * until every bonus event in the phase has completed.</p>
 *
 * <p>A {@link SequencedEvent} whose {@link SequencedEvent#run run} throws is logged, skipped (its
 * pending bonus events discarded), and the sequence advances — one bad event never wedges the loop.
 * An event that never calls {@code complete()} intentionally halts the sequence (that is the
 * completion-gate); use {@link #isBusy()} to diagnose a stall.</p>
 *
 * <p><b>Single-threaded</b> — drive it from one thread (e.g. the JavaFX application thread). It is
 * not synchronized.</p>
 */
public final class EventSequencer {

    /** Finish/spawn handle passed to a running {@link SequencedEvent}. */
    public interface Control {

        /** Signals that this event has fully finished; the sequencer then advances. Call exactly once. */
        void complete();

        /**
         * Inserts a bonus/sub-event to run after the events already queued in the current phase and
         * before the next barrier (breadth-first). Multiple spawns from one event run in spawn order.
         */
        void spawn(SequencedEvent child);

        /** Whether {@link #complete()} has already been called for this event. */
        boolean isCompleted();
    }

    private interface Entry {
    }

    private record EventEntry(SequencedEvent event) implements Entry {
    }

    private record BarrierEntry(Runnable action) implements Entry {
    }

    private final Deque<Entry> queue = new ArrayDeque<>();
    private boolean busy;
    private boolean pumping;
    private ControlImpl current;

    /**
     * Appends an event to the back of the sequence. Lazy — call {@link #run()} to process. Enqueuing
     * while the sequencer is already running (e.g. from a barrier action) is picked up automatically.
     */
    public EventSequencer enqueue(SequencedEvent event) {
        queue.addLast(new EventEntry(Validation.requireNonNull(event, "Sequenced event is required.")));
        return this;
    }

    /**
     * Appends a phase barrier. {@code action} runs only after all prior work — including bonus
     * events spawned by earlier events — has completed. The action typically enqueues the next
     * phase's events (e.g. the next turn). Lazy — call {@link #run()} to process.
     */
    public EventSequencer enqueueBarrier(Runnable action) {
        queue.addLast(new BarrierEntry(Validation.requireNonNull(action, "Barrier action is required.")));
        return this;
    }

    /**
     * Processes queued events until one blocks (an event that has not yet called
     * {@link Control#complete()}) or the queue empties. Safe to call repeatedly and safe to call
     * while an event is in progress (a no-op then — completion resumes processing on its own).
     */
    public void run() {
        pump();
    }

    /** True when no event is running and nothing is queued. */
    public boolean isIdle() {
        return !busy && queue.isEmpty();
    }

    /** True while an event has started and not yet called {@code complete()}. */
    public boolean isBusy() {
        return busy;
    }

    /** Entries still queued, including the one currently running (if any). */
    public int queued() {
        return queue.size();
    }

    /** Removes all queued work and clears running state — e.g. on new game / hard reset. */
    public void clear() {
        queue.clear();
        busy = false;
        current = null;
    }

    private void pump() {
        if (pumping) {
            return;
        }
        pumping = true;
        try {
            while (!busy && !queue.isEmpty()) {
                Entry head = queue.peekFirst();
                if (head instanceof BarrierEntry barrier) {
                    queue.removeFirst();
                    runBarrier(barrier);
                    continue;
                }
                EventEntry eventEntry = (EventEntry) head;
                busy = true;
                current = new ControlImpl();
                runEvent(eventEntry, current);
            }
        } finally {
            pumping = false;
        }
    }

    private void runEvent(EventEntry entry, ControlImpl control) {
        try {
            entry.event().run(control);
        } catch (RuntimeException ex) {
            System.err.println("[EventSequencer] Sequenced event threw — skipping it and its pending "
                    + "bonus events; the sequence continues. " + ex);
            control.children.clear();
            if (busy && current == control) {
                queue.pollFirst();
                busy = false;
                current = null;
            }
        }
    }

    /**
     * Queues {@code children} after the current phase's pending events but before the first barrier
     * (or at the back if none), preserving spawn order — the breadth-first bonus placement.
     */
    private void insertBeforeFirstBarrier(List<SequencedEvent> children) {
        if (children.isEmpty()) {
            return;
        }
        Deque<Entry> phaseHead = new ArrayDeque<>();
        while (!queue.isEmpty() && !(queue.peekFirst() instanceof BarrierEntry)) {
            phaseHead.addLast(queue.pollFirst());
        }
        for (SequencedEvent child : children) {
            phaseHead.addLast(new EventEntry(child));
        }
        while (!phaseHead.isEmpty()) {
            queue.addFirst(phaseHead.pollLast());
        }
    }

    private void runBarrier(BarrierEntry barrier) {
        try {
            barrier.action().run();
        } catch (RuntimeException ex) {
            System.err.println("[EventSequencer] Barrier action threw — the sequence continues. " + ex);
        }
    }

    private final class ControlImpl implements Control {
        private final List<SequencedEvent> children = new ArrayList<>();
        private boolean completed;

        @Override
        public void complete() {
            if (this != current) {
                throw new IllegalStateException("complete() called on a stale sequencer control.");
            }
            if (completed) {
                throw new IllegalStateException("complete() called more than once for one event.");
            }
            completed = true;
            queue.pollFirst();   // remove the finished event from the head
            // Queue spawned bonus events after the current phase's pending events but before the next
            // barrier (breadth-first): the event finishes first, then its bonus is handed out — and a
            // new turn (past the barrier) still cannot start until every bonus in the phase completes.
            insertBeforeFirstBarrier(children);
            busy = false;
            current = null;
            pump();
        }

        @Override
        public void spawn(SequencedEvent child) {
            if (this != current || completed) {
                throw new IllegalStateException("spawn() called on a finished or stale sequencer control.");
            }
            children.add(Validation.requireNonNull(child, "Spawned event is required."));
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }
    }
}
