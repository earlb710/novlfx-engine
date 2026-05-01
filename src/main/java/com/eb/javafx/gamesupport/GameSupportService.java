package com.eb.javafx.gamesupport;

/**
 * Owns generic section 2 support systems without registering authored game content.
 *
 * <p>The service creates reusable action and scheduling infrastructure during
 * startup, but intentionally does not register game-authored actions. Content
 * modules or later domain migrations should add those definitions after the
 * generic service has been initialized.</p>
 */
public final class GameSupportService {
    private final ActionRegistry actionRegistry = new ActionRegistry();
    private final LocationRegistry locationRegistry = new LocationRegistry();
    private final GameClock gameClock = new GameClock();
    private boolean initialized;

    /** Marks the support systems ready for access by bootstrap-created controllers. */
    public void initialize() {
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /** Returns the initialized action registry for generic or authored actions. */
    public ActionRegistry actionRegistry() {
        ensureInitialized();
        return actionRegistry;
    }

    /** Returns the initialized clock used by scheduling and action requirements. */
    public GameClock gameClock() {
        ensureInitialized();
        return gameClock;
    }

    /** Returns the initialized generic location registry for map descriptors. */
    public LocationRegistry locationRegistry() {
        ensureInitialized();
        return locationRegistry;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Game support service has not been initialized.");
        }
    }
}
