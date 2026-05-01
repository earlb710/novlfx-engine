package com.eb.javafx.ui;

/** Categories for boot failures that replace Ren'Py's generic startup error surface. */
public enum StartupFailureCategory {
    MISSING_ASSET("Missing asset"),
    INVALID_CONTENT("Invalid content"),
    INCOMPATIBLE_SAVE("Incompatible save"),
    PROGRAMMING_ERROR("Programming error");

    private final String displayName;

    StartupFailureCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
