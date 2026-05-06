package com.eb.javafx.display;

import com.eb.javafx.util.Validation;

/** JavaFX effect targets used by authored display animation steps. */
public record DisplayEffectTargets(
        boolean blurEnabled,
        double blurRadius,
        boolean dropShadowEnabled,
        double dropShadowRadius,
        double dropShadowOffsetX,
        double dropShadowOffsetY,
        boolean colorAdjustEnabled,
        double colorAdjustHue,
        double colorAdjustSaturation,
        double colorAdjustBrightness,
        double colorAdjustContrast) {
    public static final DisplayEffectTargets NONE = new DisplayEffectTargets(
            false, 0.0,
            false, 0.0, 0.0, 0.0,
            false, 0.0, 0.0, 0.0, 0.0);

    public DisplayEffectTargets {
        if (blurEnabled) {
            blurRadius = Validation.requireBetween(blurRadius, 0.0, 63.0, "Animation blur radius must be between 0 and 63.");
        }
        if (dropShadowEnabled) {
            dropShadowRadius = Validation.requireBetween(dropShadowRadius, 0.0, 127.0, "Animation drop shadow radius must be between 0 and 127.");
        }
        if (colorAdjustEnabled) {
            colorAdjustHue = requireColorAdjust(colorAdjustHue, "hue");
            colorAdjustSaturation = requireColorAdjust(colorAdjustSaturation, "saturation");
            colorAdjustBrightness = requireColorAdjust(colorAdjustBrightness, "brightness");
            colorAdjustContrast = requireColorAdjust(colorAdjustContrast, "contrast");
        }
    }

    public boolean hasAnyEffect() {
        return blurEnabled || dropShadowEnabled || colorAdjustEnabled;
    }

    private static double requireColorAdjust(double value, String property) {
        return Validation.requireBetween(value, -1.0, 1.0, "Animation color adjustment " + property + " must be between -1 and 1.");
    }
}
