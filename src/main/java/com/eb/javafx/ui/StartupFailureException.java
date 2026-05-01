package com.eb.javafx.ui;

/**
 * Runtime exception carrying a startup failure category for player-facing reporting.
 *
 * <p>Startup code can throw this exception when it knows which player-facing
 * category applies. {@link StartupErrorReporter} uses the category to select the
 * dialog heading while preserving the message and optional cause for diagnostics.</p>
 */
public final class StartupFailureException extends RuntimeException {
    private final StartupFailureCategory category;

    /** Creates a categorized startup failure with a player/debug-facing message. */
    public StartupFailureException(StartupFailureCategory category, String message) {
        super(message);
        this.category = category;
    }

    /** Creates a categorized startup failure while preserving the underlying cause. */
    public StartupFailureException(StartupFailureCategory category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    /** Returns the category used by startup error reporting dialogs. */
    public StartupFailureCategory category() {
        return category;
    }
}
