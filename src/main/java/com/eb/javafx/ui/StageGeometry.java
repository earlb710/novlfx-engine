package com.eb.javafx.ui;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Generic JavaFX window-geometry helpers: clamp a {@link Stage} so it stays within the visible
 * bounds of whatever screen it currently overlaps.
 *
 * <p>This is pure platform plumbing with no game-domain coupling — every host needs to keep a
 * restored / configured window from spilling off-screen (e.g. when a saved size from a larger
 * monitor is reloaded on a smaller one).</p>
 */
public final class StageGeometry {

    private StageGeometry() {
    }

    /** Clamps {@code stage} to the visual bounds of the screen it currently overlaps most. */
    public static void clampToVisibleScreen(Stage stage) {
        clampToBounds(stage, visualBoundsFor(stage));
    }

    /** The visual bounds (excludes OS taskbar / menu bar) of the screen the stage overlaps, or the
     *  primary screen's bounds if the stage is off every screen. */
    public static Rectangle2D visualBoundsFor(Stage stage) {
        return Screen.getScreensForRectangle(
                        stage.getX(),
                        stage.getY(),
                        Math.max(1.0, stage.getWidth()),
                        Math.max(1.0, stage.getHeight()))
                .stream()
                .findFirst()
                .orElseGet(Screen::getPrimary)
                .getVisualBounds();
    }

    /** Shrinks / repositions {@code stage} so it fits entirely inside {@code bounds}.
     *  @return {@code true} if any of width/height/x/y actually changed. */
    public static boolean clampToBounds(Stage stage, Rectangle2D bounds) {
        double clampedWidth = clampDimension(stage.getWidth(), bounds.getWidth());
        double clampedHeight = clampDimension(stage.getHeight(), bounds.getHeight());
        double clampedX = clampCoordinate(stage.getX(), bounds.getMinX(), bounds.getMaxX() - clampedWidth);
        double clampedY = clampCoordinate(stage.getY(), bounds.getMinY(), bounds.getMaxY() - clampedHeight);
        boolean changed = differs(stage.getWidth(), clampedWidth)
                || differs(stage.getHeight(), clampedHeight)
                || differs(stage.getX(), clampedX)
                || differs(stage.getY(), clampedY);
        if (!changed) {
            return false;
        }
        stage.setWidth(clampedWidth);
        stage.setHeight(clampedHeight);
        stage.setX(clampedX);
        stage.setY(clampedY);
        return true;
    }

    private static double clampDimension(double value, double maximum) {
        return Math.max(1.0, Math.min(value, maximum));
    }

    private static double clampCoordinate(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    private static boolean differs(double left, double right) {
        return Math.abs(left - right) > 0.5;
    }
}
