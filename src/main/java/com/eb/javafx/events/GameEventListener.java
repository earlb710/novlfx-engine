package com.eb.javafx.events;

/** Listener interface for reusable event bus adapters. */
@FunctionalInterface
public interface GameEventListener {
    void onEvent(GameEvent event);
}
