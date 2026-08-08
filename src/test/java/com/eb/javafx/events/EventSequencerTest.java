package com.eb.javafx.events;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EventSequencerTest {

    @Test
    void instantEventsRunInFifoOrder() {
        EventSequencer sequencer = new EventSequencer();
        List<String> log = new ArrayList<>();

        sequencer.enqueue(control -> {
            log.add("a");
            control.complete();
        });
        sequencer.enqueue(control -> {
            log.add("b");
            control.complete();
        });
        sequencer.run();

        assertEquals(List.of("a", "b"), log);
        assertTrue(sequencer.isIdle());
    }

    @Test
    void asyncEventBlocksTheNextUntilItCompletes() {
        EventSequencer sequencer = new EventSequencer();
        List<String> log = new ArrayList<>();
        AtomicReference<EventSequencer.Control> pending = new AtomicReference<>();

        sequencer.enqueue(control -> {
            log.add("a-start");
            pending.set(control);   // interactive: does not complete yet
        });
        sequencer.enqueue(control -> {
            log.add("b");
            control.complete();
        });
        sequencer.run();

        assertEquals(List.of("a-start"), log, "b must not run while a is in progress");
        assertTrue(sequencer.isBusy());

        pending.get().complete();

        assertEquals(List.of("a-start", "b"), log);
        assertTrue(sequencer.isIdle());
    }

    /** The reported bug: a new turn must not begin until the previous turn's bonus events finish. */
    @Test
    void bonusEventsCompleteBeforeTheBarrierOpensTheNewTurn() {
        EventSequencer sequencer = new EventSequencer();
        List<String> log = new ArrayList<>();
        AtomicReference<EventSequencer.Control> bonus = new AtomicReference<>();

        // End-of-turn events. turnEndA spawns an interactive bonus event.
        sequencer.enqueue(control -> {
            log.add("turnEndA");
            control.spawn(bonusControl -> {
                log.add("bonus-start");
                bonus.set(bonusControl);   // interactive bonus: completes later
            });
            control.complete();
        });
        sequencer.enqueue(control -> {
            log.add("turnEndB");
            control.complete();
        });
        // Barrier -> start the next turn.
        sequencer.enqueueBarrier(() -> {
            log.add("BARRIER");
            sequencer.enqueue(control -> {
                log.add("turn2");
                control.complete();
            });
        });
        sequencer.run();

        // Breadth-first: the turn-end events finish first, then bonuses are handed out. While the
        // bonus is still in progress, nothing past it may run — especially not turn2.
        assertEquals(List.of("turnEndA", "turnEndB", "bonus-start"), log);
        assertFalse(sequencer.isIdle());

        bonus.get().complete();

        assertEquals(List.of("turnEndA", "turnEndB", "bonus-start", "BARRIER", "turn2"), log);
        assertTrue(sequencer.isIdle());
    }

    @Test
    void bonusEventsRunAfterPendingSiblingsInSpawnOrder() {
        EventSequencer sequencer = new EventSequencer();
        List<String> log = new ArrayList<>();

        sequencer.enqueue(control -> {
            log.add("p");
            control.spawn(child -> {
                log.add("b1");
                child.complete();
            });
            control.spawn(child -> {
                log.add("b2");
                child.complete();
            });
            control.complete();
        });
        sequencer.enqueue(control -> {
            log.add("q");
            control.complete();
        });
        sequencer.run();

        // p and q (same phase) run before p's bonuses; bonuses keep spawn order.
        assertEquals(List.of("p", "q", "b1", "b2"), log);
    }

    @Test
    void bonusEventsResolveBreadthFirstByGenerationBeforeTheBarrier() {
        EventSequencer sequencer = new EventSequencer();
        List<String> log = new ArrayList<>();

        // A and B each spawn a first-generation bonus; A's bonus spawns a second-generation bonus.
        sequencer.enqueue(control -> {
            log.add("A");
            control.spawn(a1 -> {
                log.add("A1");
                a1.spawn(a2 -> {
                    log.add("A2");
                    a2.complete();
                });
                a1.complete();
            });
            control.complete();
        });
        sequencer.enqueue(control -> {
            log.add("B");
            control.spawn(b1 -> {
                log.add("B1");
                b1.complete();
            });
            control.complete();
        });
        sequencer.enqueueBarrier(() -> log.add("BARRIER"));
        sequencer.run();

        // Generation order: [A,B] then [A1,B1] then [A2], all before the barrier.
        assertEquals(List.of("A", "B", "A1", "B1", "A2", "BARRIER"), log);
    }

    @Test
    void completingTwiceThrows() {
        EventSequencer sequencer = new EventSequencer();
        AtomicReference<EventSequencer.Control> ref = new AtomicReference<>();

        sequencer.enqueue(ref::set);
        sequencer.run();
        ref.get().complete();

        assertThrows(IllegalStateException.class, () -> ref.get().complete());
    }

    @Test
    void enqueueingWhileBusyDoesNotStartUntilCurrentCompletes() {
        EventSequencer sequencer = new EventSequencer();
        List<String> log = new ArrayList<>();
        AtomicReference<EventSequencer.Control> first = new AtomicReference<>();

        sequencer.enqueue(control -> {
            log.add("first-start");
            first.set(control);
        });
        sequencer.run();

        // Enqueue arrives while the first event is still in progress; picked up on completion.
        sequencer.enqueue(control -> {
            log.add("second");
            control.complete();
        });

        assertEquals(List.of("first-start"), log);

        first.get().complete();

        assertEquals(List.of("first-start", "second"), log);
        assertTrue(sequencer.isIdle());
    }
}
