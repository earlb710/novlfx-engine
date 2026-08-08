package com.eb.javafx.events;

/**
 * A single unit of work processed one-at-a-time by {@link EventSequencer}.
 *
 * <p>An event may finish immediately (call {@link EventSequencer.Control#complete() complete()}
 * before {@link #run} returns) or later (hold the {@link EventSequencer.Control} and call
 * {@code complete()} when an interactive dialog/scene finishes across many frames). The sequencer
 * treats the event as in-progress — and refuses to start the next one — until {@code complete()}
 * is called. While running, an event may {@link EventSequencer.Control#spawn spawn} bonus/sub-events
 * that must finish before the sequence advances past it.</p>
 */
@FunctionalInterface
public interface SequencedEvent {

    /**
     * Runs this event. Use {@code control} to finish it and to spawn bonus sub-events.
     *
     * @param control finish/spawn handle for this event; valid only until {@code complete()} is called
     */
    void run(EventSequencer.Control control);
}
