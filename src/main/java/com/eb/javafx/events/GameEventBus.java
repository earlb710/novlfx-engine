package com.eb.javafx.events;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Lightweight event bus with deterministic in-memory history for diagnostics and tests. */
public final class GameEventBus {
    private final Map<String, List<Consumer<GameEvent>>> listeners = new LinkedHashMap<>();
    private final List<GameEvent> history = new ArrayList<>();

    public Runnable subscribe(String eventType, Consumer<GameEvent> listener) {
        String checkedType = Validation.requireNonBlank(eventType, "Game event type is required.");
        Consumer<GameEvent> checkedListener = Validation.requireNonNull(listener, "Game event listener is required.");
        listeners.computeIfAbsent(checkedType, ignored -> new ArrayList<>()).add(checkedListener);
        return () -> listeners.getOrDefault(checkedType, List.of()).remove(checkedListener);
    }

    public void publish(GameEvent event) {
        GameEvent checkedEvent = Validation.requireNonNull(event, "Game event is required.");
        history.add(checkedEvent);
        for (Consumer<GameEvent> listener : listeners.getOrDefault(checkedEvent.type(), List.of())) {
            listener.accept(checkedEvent);
        }
    }

    public List<GameEvent> history() {
        return Collections.unmodifiableList(history);
    }

    public List<GameEvent> history(String eventType) {
        return history.stream().filter(event -> event.type().equals(eventType)).toList();
    }
}
