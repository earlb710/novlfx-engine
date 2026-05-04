package com.eb.javafx.gamesupport;

/** Hook invoked around generic game-clock advancement. */
public interface TimeAdvanceHook {
    default void beforeAdvance(GameDateTime currentTime) {
    }

    default void afterAdvance(GameDateTime previousTime, GameDateTime currentTime) {
    }
}
