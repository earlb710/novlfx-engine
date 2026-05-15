package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

/**
 * UI-neutral description of one HUD overlay screen placed on top of the main app layout.
 *
 * <p>One overlay maps one {@link ScreenDesignBlock} (in a {@link ScreenLayoutType#MAIN_APP_LAYOUT}
 * design) to a placement on the overlay layer. The {@code screenId} is resolved by the application
 * through {@link MainAppScreenResolver} when the renderer builds the JavaFX tree, so overlay
 * content stays decoupled from the layout that hosts it.</p>
 *
 * <p>The placement coordinates are interpreted according to {@link #mode()}:</p>
 * <ul>
 *   <li>{@link MainAppLayoutPlacementMode#ALIGNMENT}: {@link #anchor()} chooses a nine-grid
 *       position and {@link #offsetX()}/{@link #offsetY()} add pixel offsets from that anchor.</li>
 *   <li>{@link MainAppLayoutPlacementMode#PIXELS}: {@link #offsetX()}/{@link #offsetY()} are
 *       absolute pixel coordinates from the layout's top-left.</li>
 *   <li>{@link MainAppLayoutPlacementMode#PERCENT}: {@link #offsetX()}/{@link #offsetY()} are
 *       fractions (0.0–1.0) of layout width/height.</li>
 * </ul>
 *
 * <p>Size hints are optional. When a hint is {@code null} the overlay sizes itself to its
 * content's preferred size.</p>
 */
public record MainAppLayoutOverlay(
        String id,
        String screenId,
        MainAppLayoutPlacementMode mode,
        MainAppLayoutAnchor anchor,
        String anchorBlockId,
        double offsetX,
        double offsetY,
        Double preferredWidth,
        Double preferredHeight,
        double opacity,
        boolean visible) {
    public MainAppLayoutOverlay {
        id = Validation.requireNonBlank(id, "Main app layout overlay id is required.");
        screenId = Validation.requireNonBlank(screenId, "Main app layout overlay screenId is required.");
        mode = Validation.requireNonNull(mode, "Main app layout overlay placement mode is required.");
        if (mode == MainAppLayoutPlacementMode.ALIGNMENT) {
            if (anchor == null) {
                anchor = MainAppLayoutAnchor.TOP_LEFT;
            } else if (anchor.isRelative()) {
                throw new IllegalArgumentException(
                        "Main app layout overlay '" + id + "' uses ALIGNMENT placement; anchor must be a nine-grid value, not "
                                + anchor + ".");
            }
        }
        if (mode == MainAppLayoutPlacementMode.RELATIVE) {
            if (anchor == null) {
                anchor = MainAppLayoutAnchor.RIGHT;
            } else if (!anchor.isRelative()) {
                throw new IllegalArgumentException(
                        "Main app layout overlay '" + id + "' uses RELATIVE placement; anchor must be ABOVE, BELOW, LEFT, or RIGHT, not "
                                + anchor + ".");
            }
            if (anchorBlockId == null || anchorBlockId.isBlank()) {
                throw new IllegalArgumentException(
                        "Main app layout overlay '" + id + "' uses RELATIVE placement; overlayAnchorField is required.");
            }
            if (anchorBlockId.equals(id)) {
                throw new IllegalArgumentException(
                        "Main app layout overlay '" + id + "' cannot reference itself as its relative anchor.");
            }
        } else if (anchorBlockId != null && anchorBlockId.isBlank()) {
            // Normalise blank values to null for non-relative modes; carry through any non-blank
            // value so the designer can preserve it when the author toggles placement modes.
            anchorBlockId = null;
        }
        if (mode == MainAppLayoutPlacementMode.PERCENT) {
            Validation.requireBetween(offsetX, 0.0, 1.0,
                    "Main app layout overlay percent x must be between 0.0 and 1.0.");
            Validation.requireBetween(offsetY, 0.0, 1.0,
                    "Main app layout overlay percent y must be between 0.0 and 1.0.");
        }
        if (preferredWidth != null && preferredWidth <= 0.0) {
            throw new IllegalArgumentException("Main app layout overlay preferred width must be positive.");
        }
        if (preferredHeight != null && preferredHeight <= 0.0) {
            throw new IllegalArgumentException("Main app layout overlay preferred height must be positive.");
        }
        opacity = Validation.requireUnitInterval(opacity, "Main app layout overlay opacity must be between 0 and 1.");
    }
}
