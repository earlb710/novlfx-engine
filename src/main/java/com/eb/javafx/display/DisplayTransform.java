package com.eb.javafx.display;

import com.eb.javafx.util.Validation;

/**
 * First-pass Java model for visual novel transforms used by migrated images.
 *
 * <p>Transforms define fit dimensions, opacity, alignment metadata, and a render-order integer
 * that the display registry can apply when creating JavaFX image nodes. Fit sizes must be
 * positive, opacity is {@code 0.0..1.0}, and alignment values are normalized
 * {@code 0.0..1.0} anchors. Zorder defaults to 0; adapters sort render lists ascending.</p>
 */
public final class DisplayTransform {
    private final String id;
    private final int fitWidth;
    private final int fitHeight;
    private final double opacity;
    private final double xAlign;
    private final double yAlign;
    private final int zorder;

    public DisplayTransform(String id, int fitWidth, int fitHeight, double opacity, double xAlign, double yAlign) {
        this(id, fitWidth, fitHeight, opacity, xAlign, yAlign, 0);
    }

    public DisplayTransform(String id, int fitWidth, int fitHeight, double opacity, double xAlign, double yAlign, int zorder) {
        this.id = Validation.requireNonBlank(id, "Display transform id is required.");
        this.fitWidth = Validation.requirePositive(fitWidth, "Display transform size must be positive.");
        this.fitHeight = Validation.requirePositive(fitHeight, "Display transform size must be positive.");
        this.opacity = Validation.requireUnitInterval(opacity, "Display transform opacity must be between 0 and 1.");
        this.xAlign = Validation.requireUnitInterval(xAlign, "Display transform alignment must be between 0 and 1.");
        this.yAlign = Validation.requireUnitInterval(yAlign, "Display transform alignment must be between 0 and 1.");
        this.zorder = zorder;
    }

    public String id() { return id; }
    public int fitWidth() { return fitWidth; }
    public int fitHeight() { return fitHeight; }
    public double opacity() { return opacity; }
    public double xAlign() { return xAlign; }
    public double yAlign() { return yAlign; }
    public int zorder() { return zorder; }

    public DisplayTransform withZorder(int zorder) {
        return new DisplayTransform(id, fitWidth, fitHeight, opacity, xAlign, yAlign, zorder);
    }
}
