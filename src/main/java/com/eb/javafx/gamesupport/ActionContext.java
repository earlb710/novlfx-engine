package com.eb.javafx.gamesupport;

import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.state.GameState;

import java.util.Objects;

/**
 * Runtime services passed to generic game-support action requirements and effects.
 *
 * <p>The context provides the mutable per-save state plus deterministic gameplay
 * services needed while checking or applying an action. Constructor arguments are
 * required and fail fast with {@link NullPointerException} when omitted.</p>
 */
public final class ActionContext {
    private final GameState gameState;
    private final GameRandomService randomService;
    private final GameClock gameClock;

    /**
     * Creates an action context for requirement checks and effect application.
     *
     * @param gameState mutable session state
     * @param randomService deterministic gameplay random service
     * @param gameClock current scheduling/time-slot service
     */
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
