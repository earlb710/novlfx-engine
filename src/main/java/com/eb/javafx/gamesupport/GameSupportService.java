package com.eb.javafx.gamesupport;

/** Owns generic section 2 support systems without registering authored game content. */
public final class GameSupportService {
    private final ActionRegistry actionRegistry = new ActionRegistry();
    private final GameClock gameClock = new GameClock();
    private boolean initialized;

    public void initialize() {
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public ActionRegistry actionRegistry() {
        ensureInitialized();
        return actionRegistry;
    }

    public GameClock gameClock() {
        ensureInitialized();
        return gameClock;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Game support service has not been initialized.");
        }
    }
}
