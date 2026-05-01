package com.eb.javafx.ui;

/** Runtime exception carrying a startup failure category for player-facing reporting. */
public final class StartupFailureException extends RuntimeException {
    private final StartupFailureCategory category;

    public StartupFailureException(StartupFailureCategory category, String message) {
        super(message);
        this.category = category;
    }

    public StartupFailureException(StartupFailureCategory category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    public StartupFailureCategory category() {
        return category;
    }
}
