package com.eb.javafx.display;

/**
 * One ATL-style animation step for a JavaFX node.
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

    public DisplayAnimationStep(
            long durationMillis,
            long pauseBeforeMillis,
            double targetOpacity,
            double targetScaleX,
            double targetScaleY,
            double targetTranslateX,
            double targetTranslateY,
            DisplayInterpolation interpolation) {
        if (durationMillis < 0 || pauseBeforeMillis < 0) {
            throw new IllegalArgumentException("Animation durations must be zero or positive.");
        }
        if (targetOpacity < 0.0 || targetOpacity > 1.0) {
            throw new IllegalArgumentException("Animation opacity must be between 0 and 1.");
        }
        if (targetScaleX <= 0.0 || targetScaleY <= 0.0) {
            throw new IllegalArgumentException("Animation scale must be positive.");
        }
        this.durationMillis = durationMillis;
        this.pauseBeforeMillis = pauseBeforeMillis;
        this.targetOpacity = targetOpacity;
        this.targetScaleX = targetScaleX;
        this.targetScaleY = targetScaleY;
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
