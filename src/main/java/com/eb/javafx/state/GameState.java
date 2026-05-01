package com.eb.javafx.state;

/**
 * Minimal mutable game state placeholder for the JavaFX bootstrap slice.
 *
 * <p>Ren'Py default variables are per-save values. This object represents the
 * JavaFX replacement location for those mutable defaults once gameplay systems are
 * migrated into Java domain classes.</p>
 */
public final class GameState {
    private final String startupRoute;

    /** Creates placeholder mutable state with the route selected by static content. */
    public GameState(String startupRoute) {
        this.startupRoute = startupRoute;
    }

    /**
     * Returns the route a new game should enter after startup.
     *
     * <p>Future mutable gameplay fields should live alongside this value so save
     * data remains explicit rather than hidden in globals.</p>
     */
    public String startupRoute() {
        return startupRoute;
    }
}
