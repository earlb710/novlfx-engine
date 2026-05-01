package com.lr2alt.javafx.gamesupport;

import com.lr2alt.javafx.random.GameRandomService;
import com.lr2alt.javafx.state.GameState;

import java.util.Objects;

/** Runtime services passed to generic game-support action requirements and effects. */
public final class ActionContext {
    private final GameState gameState;
    private final GameRandomService randomService;
    private final GameClock gameClock;

    public ActionContext(GameState gameState, GameRandomService randomService, GameClock gameClock) {
        this.gameState = Objects.requireNonNull(gameState, "gameState");
        this.randomService = Objects.requireNonNull(randomService, "randomService");
        this.gameClock = Objects.requireNonNull(gameClock, "gameClock");
    }

    /** Returns the mutable per-save state owned by the current game session. */
    public GameState gameState() {
        return gameState;
    }

    /** Returns deterministic gameplay randomness for support systems that need random choices. */
    public GameRandomService randomService() {
        return randomService;
    }

    /** Returns the generic clock/time-slot model for scheduling support. */
    public GameClock gameClock() {
        return gameClock;
    }
}
