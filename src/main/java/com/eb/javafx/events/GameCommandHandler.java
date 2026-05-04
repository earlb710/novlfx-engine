package com.eb.javafx.events;

import java.util.List;

/** Handles a reusable command and returns emitted events. */
@FunctionalInterface
public interface GameCommandHandler {
    List<GameEvent> handle(GameCommand command);
}
