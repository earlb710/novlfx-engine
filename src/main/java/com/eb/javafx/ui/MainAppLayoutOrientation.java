package com.eb.javafx.ui;

import java.util.Locale;

/** How the story area and dialog area share the central frame in the main app layout. */
public enum MainAppLayoutOrientation {
    /** Story area sits above the dialog area; the ratio is the story share of total height. */
    VERTICAL,
    /** Story area sits to the left of the dialog area; the ratio is the story share of total width. */
    HORIZONTAL;

    /** Returns {@link #VERTICAL} for {@code null} or blank input; accepts case-insensitive names. */
    public static MainAppLayoutOrientation parse(String token) {
        if (token == null || token.isBlank()) {
            return VERTICAL;
        }
        String normalized = token.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "VERTICAL", "V", "STACKED" -> VERTICAL;
            case "HORIZONTAL", "H", "SIDE_BY_SIDE", "SIDE-BY-SIDE" -> HORIZONTAL;
            default -> {
                try {
                    yield MainAppLayoutOrientation.valueOf(normalized);
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException(
                            "Unknown main-app-layout orientation: " + token, exception);
                }
            }
        };
    }
}
