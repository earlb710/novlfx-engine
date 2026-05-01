package com.lr2alt.javafx.display;

/**
 * First-pass Java model for Ren'Py ATL transforms used by migrated images.
 */
public final class DisplayTransform {
    private final String id;
    private final int fitWidth;
    private final int fitHeight;
    private final double opacity;
    private final double xAlign;
    private final double yAlign;

    public DisplayTransform(String id, int fitWidth, int fitHeight, double opacity, double xAlign, double yAlign) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Display transform id is required.");
        }
        if (fitWidth <= 0 || fitHeight <= 0) {
            throw new IllegalArgumentException("Display transform size must be positive.");
        }
        if (opacity < 0.0 || opacity > 1.0) {
            throw new IllegalArgumentException("Display transform opacity must be between 0 and 1.");
        }
        if (xAlign < 0.0 || xAlign > 1.0 || yAlign < 0.0 || yAlign > 1.0) {
            throw new IllegalArgumentException("Display transform alignment must be between 0 and 1.");
        }
        this.id = id;
        this.fitWidth = fitWidth;
        this.fitHeight = fitHeight;
        this.opacity = opacity;
        this.xAlign = xAlign;
        this.yAlign = yAlign;
    }

    public String id() {
        return id;
    }

    public int fitWidth() {
        return fitWidth;
    }

    public int fitHeight() {
        return fitHeight;
    }

    public double opacity() {
        return opacity;
    }

    public double xAlign() {
        return xAlign;
    }

    public double yAlign() {
        return yAlign;
    }
}
