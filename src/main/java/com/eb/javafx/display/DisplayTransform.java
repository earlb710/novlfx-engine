package com.eb.javafx.display;

import com.eb.javafx.util.Validation;

/**
 * First-pass Java model for Ren'Py ATL transforms used by migrated images.
 *
 * <p>Transforms define fit dimensions, opacity, and alignment metadata that the
 * display registry can apply when creating JavaFX image nodes. Fit sizes must be
 * positive, opacity is {@code 0.0..1.0}, and alignment values are normalized
 * {@code 0.0..1.0} anchors.</p>
 */
public final class DisplayTransform {
    private final String id;
    private final int fitWidth;
    private final int fitHeight;
    private final double opacity;
    private final double xAlign;
    private final double yAlign;

    /**
     * Creates validated image transform metadata.
     *
     * @param id non-blank transform ID
     * @param fitWidth positive JavaFX fit width
     * @param fitHeight positive JavaFX fit height
     * @param opacity image opacity from {@code 0.0} to {@code 1.0}
     * @param xAlign horizontal alignment anchor from left {@code 0.0} to right {@code 1.0}
     * @param yAlign vertical alignment anchor from top {@code 0.0} to bottom {@code 1.0}
     */
    public DisplayTransform(String id, int fitWidth, int fitHeight, double opacity, double xAlign, double yAlign) {
        this.id = Validation.requireNonBlank(id, "Display transform id is required.");
        this.fitWidth = Validation.requirePositive(fitWidth, "Display transform size must be positive.");
        this.fitHeight = Validation.requirePositive(fitHeight, "Display transform size must be positive.");
        this.opacity = Validation.requireUnitInterval(opacity, "Display transform opacity must be between 0 and 1.");
        this.xAlign = Validation.requireUnitInterval(xAlign, "Display transform alignment must be between 0 and 1.");
        this.yAlign = Validation.requireUnitInterval(yAlign, "Display transform alignment must be between 0 and 1.");
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
