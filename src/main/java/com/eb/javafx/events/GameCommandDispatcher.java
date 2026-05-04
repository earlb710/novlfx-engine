package com.eb.javafx.events;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Type-keyed command dispatcher that can publish handler events to a bus. */
public final class GameCommandDispatcher {
    private final Map<String, GameCommandHandler> handlers = new LinkedHashMap<>();

    public void register(String commandType, GameCommandHandler handler) {
        handlers.put(
                Validation.requireNonBlank(commandType, "Game command type is required."),
                Validation.requireNonNull(handler, "Game command handler is required."));
    }

    public List<GameEvent> dispatch(GameCommand command) {
        GameCommand checkedCommand = Validation.requireNonNull(command, "Game command is required.");
        GameCommandHandler handler = handlers.get(checkedCommand.type());
        if (handler == null) {
            throw new IllegalArgumentException("No command handler registered for: " + checkedCommand.type());
        }
        return List.copyOf(handler.handle(checkedCommand));
    }

    public List<GameEvent> dispatch(GameCommand command, GameEventBus eventBus) {
        GameEventBus checkedBus = Validation.requireNonNull(eventBus, "Game event bus is required.");
        List<GameEvent> events = dispatch(command);
        events.forEach(checkedBus::publish);
        return events;
    }
}
