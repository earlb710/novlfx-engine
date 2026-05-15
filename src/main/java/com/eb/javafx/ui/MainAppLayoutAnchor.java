package com.eb.javafx.ui;

import javafx.geometry.Pos;

import java.util.Locale;

/**
 * Nine-grid anchor positions used to place HUD overlay screens within the main app layout.
 *
 * <p>The values mirror the screen-relative anchors offered by JavaFX {@link Pos} so renderers can
 * forward the choice to {@code StackPane.setAlignment} without translation tables. Names accept
 * the JavaFX form ({@code TOP_CENTER}) and the shorter alias ({@code TOP}) when parsed from
 * authoring metadata.</p>
 */
public enum MainAppLayoutAnchor {
    TOP_LEFT(Pos.TOP_LEFT, false),
    TOP_CENTER(Pos.TOP_CENTER, false),
    TOP_RIGHT(Pos.TOP_RIGHT, false),
    CENTER_LEFT(Pos.CENTER_LEFT, false),
    CENTER(Pos.CENTER, false),
    CENTER_RIGHT(Pos.CENTER_RIGHT, false),
    BOTTOM_LEFT(Pos.BOTTOM_LEFT, false),
    BOTTOM_CENTER(Pos.BOTTOM_CENTER, false),
    BOTTOM_RIGHT(Pos.BOTTOM_RIGHT, false),
    /** Relative anchor: this overlay is placed above its referenced sibling. */
    ABOVE(null, true),
    /** Relative anchor: this overlay is placed below its referenced sibling. */
    BELOW(null, true),
    /** Relative anchor: this overlay is placed to the left of its referenced sibling. */
    LEFT(null, true),
    /** Relative anchor: this overlay is placed to the right of its referenced sibling. */
    RIGHT(null, true);

    private final Pos javafxPos;
    private final boolean relative;

    MainAppLayoutAnchor(Pos javafxPos, boolean relative) {
        this.javafxPos = javafxPos;
        this.relative = relative;
    }

    /**
     * Returns the corresponding nine-grid {@link Pos} for an {@link MainAppLayoutPlacementMode#ALIGNMENT} anchor,
     * or {@code null} for relative anchors. Callers selecting alignment mode should always receive a non-null Pos.
     */
    public Pos toJavaFxPos() {
        return javafxPos;
    }

    /** Whether this anchor names a position relative to another block (ABOVE/BELOW/LEFT/RIGHT). */
    public boolean isRelative() {
        return relative;
    }

    /**
     * Accepts JavaFX nine-grid names ({@code TOP_CENTER}), the {@code TOP}/{@code BOTTOM} short aliases for the
     * nine-grid centre row/column, and the relative anchors ({@code ABOVE}, {@code BELOW}, {@code LEFT}, {@code RIGHT}).
     * Case-insensitive; hyphens and spaces are normalised to underscores.
     */
    public static MainAppLayoutAnchor parse(String token) {
        if (token == null) {
            return null;
        }
        String normalized = token.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "TOP" -> TOP_CENTER;
            case "BOTTOM" -> BOTTOM_CENTER;
            default -> {
                try {
                    yield MainAppLayoutAnchor.valueOf(normalized);
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("Unknown main-app-layout overlay anchor: " + token, exception);
                }
            }
        };
    }
}
