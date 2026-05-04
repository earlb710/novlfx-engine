package com.eb.javafx.events;

import com.eb.javafx.util.Validation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/** FIFO queue for decoupling event production from processing. */
public final class GameEventQueue {
    private final Queue<GameEvent> events = new ArrayDeque<>();

    public void enqueue(GameEvent event) {
        events.add(Validation.requireNonNull(event, "Game event is required."));
    }

    public List<GameEvent> drain() {
        List<GameEvent> drained = new ArrayList<>();
        while (!events.isEmpty()) {
            drained.add(events.remove());
        }
        return List.copyOf(drained);
    }

    public List<GameEvent> events() {
        return List.copyOf(events);
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }
}
