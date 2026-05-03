package com.eb.javafx.state;

import com.eb.javafx.text.DialogHistory;
import com.eb.javafx.util.Validation;

/**
 * Minimal mutable game state placeholder for the JavaFX bootstrap slice.
 *
 * <p>Visual novel default variables are per-save values. This object represents the
 * JavaFX replacement location for those mutable defaults once gameplay systems are
 * migrated into Java domain classes.</p>
 */
public final class GameState {
    private final String startupRoute;
    private final DialogHistory conversationHistory;

    /** Creates placeholder mutable state with the route selected by static content. */
    public GameState(String startupRoute) {
        this(startupRoute, new DialogHistory());
    }

    /** Creates placeholder mutable state with explicit reusable conversation history. */
    public GameState(String startupRoute, DialogHistory conversationHistory) {
        this.startupRoute = startupRoute;
        this.conversationHistory = Validation.requireNonNull(conversationHistory, "Conversation history is required.");
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

    /** Returns the per-save conversation history used by helper screens and authored UI. */
    public DialogHistory conversationHistory() {
        return conversationHistory;
    }
}
