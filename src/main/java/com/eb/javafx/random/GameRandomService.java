package com.eb.javafx.random;

import java.util.Random;

/**
 * Separates deterministic gameplay randomness from non-persistent UI effects.
 *
 * <p>Ren'Py's random helpers are globally available. The JavaFX port keeps random
 * values behind this service so gameplay seeds can later be serialized without
 * making UI animation randomness part of save-game state.</p>
 */
public final class GameRandomService {
    private long gameplaySeed;
    private Random gameplayRandom;
    private Random uiRandom;

    /**
     * Creates deterministic gameplay random state and separate UI random state.
     *
     * <p>The current placeholder seed is fixed at {@code 1L} so tests and future
     * save migration can reproduce gameplay choices.</p>
     */
    public void initialize() {
        gameplaySeed = 1L;
        gameplayRandom = new Random(gameplaySeed);
        uiRandom = new Random();
    }

    /** Returns the persisted seed for the current new-game placeholder state. */
    public long gameplaySeed() {
        assertInitialized();
        return gameplaySeed;
    }

    /**
     * Returns the next gameplay value that should be reproducible after loading.
     *
     * @param bound exclusive upper bound passed to {@link Random#nextInt(int)}
     */
    public int nextGameplayInt(int bound) {
        assertInitialized();
        return gameplayRandom.nextInt(bound);
    }

    /**
     * Returns the next UI-only value that should not be written to save data.
     *
     * @param bound exclusive upper bound passed to {@link Random#nextInt(int)}
     */
    public int nextUiInt(int bound) {
        assertInitialized();
        return uiRandom.nextInt(bound);
    }

    private void assertInitialized() {
        if (gameplayRandom == null || uiRandom == null) {
            throw new IllegalStateException("Random service used before initialization.");
        }
    }
}
