package com.eb.javafx.display;

import javafx.animation.Interpolator;

/**
 * Supported interpolation curves for migrated ATL-style animations.
 */
public enum DisplayInterpolation {
    /** Constant-speed interpolation mapped to {@link Interpolator#LINEAR}. */
    LINEAR,
    /** Smooth acceleration and deceleration mapped to {@link Interpolator#EASE_BOTH}. */
    EASE_BOTH,
    /** Smooth acceleration mapped to {@link Interpolator#EASE_IN}. */
    EASE_IN,
    /** Smooth deceleration mapped to {@link Interpolator#EASE_OUT}. */
    EASE_OUT,
    /** Stepwise transition mapped to {@link Interpolator#DISCRETE}. */
    DISCRETE;

    /** Returns the JavaFX interpolator used when building animation key values. */
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
