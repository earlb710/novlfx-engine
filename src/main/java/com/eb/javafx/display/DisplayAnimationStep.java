package com.eb.javafx.display;

import com.eb.javafx.util.Validation;

/**
 * One ATL-style animation step for a JavaFX node.
 *
 * <p>Each step can pause, then transition a node toward target opacity, scale,
 * and translation values using a supported interpolation curve. Durations are
 * non-negative milliseconds, opacity is {@code 0.0..1.0}, and scale values must
 * remain positive.</p>
 */
public final class DisplayAnimationStep {
    private final long durationMillis;
    private final long pauseBeforeMillis;
    private final double targetOpacity;
    private final double targetScaleX;
    private final double targetScaleY;
    private final double targetTranslateX;
    private final double targetTranslateY;
    private final DisplayInterpolation interpolation;

    /**
     * Creates a validated animation step.
     *
     * @param durationMillis transition duration in milliseconds, zero or positive
     * @param pauseBeforeMillis delay before this transition, zero or positive
     * @param targetOpacity final opacity from {@code 0.0} to {@code 1.0}
     * @param targetScaleX final horizontal scale, greater than zero
     * @param targetScaleY final vertical scale, greater than zero
     * @param targetTranslateX final horizontal translation in pixels
     * @param targetTranslateY final vertical translation in pixels
     * @param interpolation interpolation curve; linear when null
     */
    public DisplayAnimationStep(
            long durationMillis,
            long pauseBeforeMillis,
            double targetOpacity,
            double targetScaleX,
            double targetScaleY,
            double targetTranslateX,
            double targetTranslateY,
            DisplayInterpolation interpolation) {
        this.durationMillis = Validation.requireZeroOrPositive(durationMillis, "Animation durations must be zero or positive.");
        this.pauseBeforeMillis = Validation.requireZeroOrPositive(pauseBeforeMillis, "Animation durations must be zero or positive.");
        this.targetOpacity = Validation.requireUnitInterval(targetOpacity, "Animation opacity must be between 0 and 1.");
        this.targetScaleX = Validation.requirePositive(targetScaleX, "Animation scale must be positive.");
        this.targetScaleY = Validation.requirePositive(targetScaleY, "Animation scale must be positive.");
        this.targetTranslateX = targetTranslateX;
        this.targetTranslateY = targetTranslateY;
        this.interpolation = interpolation == null ? DisplayInterpolation.LINEAR : interpolation;
    }

    public long durationMillis() {
        return durationMillis;
    }

    public long pauseBeforeMillis() {
        return pauseBeforeMillis;
    }

    public double targetOpacity() {
        return targetOpacity;
    }

    public double targetScaleX() {
        return targetScaleX;
    }

    public double targetScaleY() {
        return targetScaleY;
    }

    public double targetTranslateX() {
        return targetTranslateX;
    }

    public double targetTranslateY() {
        return targetTranslateY;
    }

    public DisplayInterpolation interpolation() {
        return interpolation;
    }
}
