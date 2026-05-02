package com.eb.javafx.ui;

/**
 * Categories for boot failures surfaced during JavaFX startup.
 *
 * <p>The enum separates player-facing startup problems from programming errors so the error reporter can
 * display a concise label while preserving a more specific exception message.</p>
 */
public enum StartupFailureCategory {
    /** A required stylesheet, image, audio file, or other asset could not be found. */
    MISSING_ASSET("Missing asset"),
    /** Static content or route/display definitions failed validation. */
    INVALID_CONTENT("Invalid content"),
    /** Save data exists but does not match the supported schema. */
    INCOMPATIBLE_SAVE("Incompatible save"),
    /** Unexpected code or wiring failure not covered by a player-facing category. */
    PROGRAMMING_ERROR("Programming error");

    private final String displayName;

    StartupFailureCategory(String displayName) {
        this.displayName = displayName;
    }

    /** Returns the short player-facing category label for startup dialogs. */
    public String displayName() {
        return displayName;
    }
}
