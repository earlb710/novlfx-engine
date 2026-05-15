package com.eb.javafx.ui;

import java.util.Locale;

/** How a HUD overlay screen is positioned inside the main app layout's overlay stack. */
public enum MainAppLayoutPlacementMode {
    /** Anchored to a nine-grid position with optional pixel offsets. */
    ALIGNMENT,
    /** Absolute pixel coordinates measured from the top-left of the layout. */
    PIXELS,
    /** Coordinates expressed as a fraction (0.0–1.0) of the layout width/height. */
    PERCENT,
    /**
     * Anchored relative to another overlay block. Combined with one of the relative anchors
     * ({@link MainAppLayoutAnchor#ABOVE}, {@link MainAppLayoutAnchor#BELOW},
     * {@link MainAppLayoutAnchor#LEFT}, {@link MainAppLayoutAnchor#RIGHT}) plus the id of the
     * sibling block to anchor against. Offsets are added on top of the resolved position so
     * authors can fine-tune spacing.
     */
    RELATIVE;

    /** Accepts case-insensitive names. Returns {@link #ALIGNMENT} for {@code null} or blank input. */
    public static MainAppLayoutPlacementMode parse(String token) {
        if (token == null || token.isBlank()) {
            return ALIGNMENT;
        }
        String normalized = token.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ALIGN", "ALIGNMENT", "ANCHOR" -> ALIGNMENT;
            case "PIXEL", "PIXELS", "PX" -> PIXELS;
            case "PERCENT", "PERCENTAGE", "FRACTION", "FRACTIONAL" -> PERCENT;
            case "RELATIVE", "REL" -> RELATIVE;
            default -> {
                try {
                    yield MainAppLayoutPlacementMode.valueOf(normalized);
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException(
                            "Unknown main-app-layout overlay placement mode: " + token, exception);
                }
            }
        };
    }
}
