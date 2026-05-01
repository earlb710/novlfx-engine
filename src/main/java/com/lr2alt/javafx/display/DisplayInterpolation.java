package com.lr2alt.javafx.display;

import javafx.animation.Interpolator;

/**
 * Supported interpolation curves for migrated ATL-style animations.
 */
public enum DisplayInterpolation {
    LINEAR,
    EASE_BOTH,
    EASE_IN,
    EASE_OUT,
    DISCRETE;

    public Interpolator toJavaFxInterpolator() {
        return switch (this) {
            case LINEAR -> Interpolator.LINEAR;
            case EASE_BOTH -> Interpolator.EASE_BOTH;
            case EASE_IN -> Interpolator.EASE_IN;
            case EASE_OUT -> Interpolator.EASE_OUT;
            case DISCRETE -> Interpolator.DISCRETE;
        };
    }
}
