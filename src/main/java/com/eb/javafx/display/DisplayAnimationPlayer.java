package com.eb.javafx.display;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JavaFX timelines from reusable ATL-style animation profiles.
 *
 * <p>The player converts validated display animation steps into JavaFX key frames.
 * Reduced-motion callers receive a zero-duration timeline after the node is moved
 * directly to the final state.</p>
 */
public final class DisplayAnimationPlayer {

    /**
     * Builds a JavaFX timeline for the supplied node and animation profile.
     *
     * @param node node whose opacity, scale, and translation properties are animated
     * @param animation validated non-empty animation profile
     * @param reducedMotion when true, skips interpolation and applies the final step immediately
     * @return timeline configured with repeat count and auto-reverse behavior
     */
    public Timeline buildTimeline(Node node, DisplayAnimation animation, boolean reducedMotion) {
        if (reducedMotion) {
            applyFinalState(node, animation.steps().get(animation.steps().size() - 1));
            return new Timeline(new KeyFrame(Duration.ZERO));
        }

        List<KeyFrame> keyFrames = new ArrayList<>();
        Duration elapsed = Duration.ZERO;
        keyFrames.add(new KeyFrame(Duration.ZERO,
                new KeyValue(node.opacityProperty(), node.getOpacity()),
                new KeyValue(node.scaleXProperty(), node.getScaleX()),
                new KeyValue(node.scaleYProperty(), node.getScaleY()),
                new KeyValue(node.translateXProperty(), node.getTranslateX()),
                new KeyValue(node.translateYProperty(), node.getTranslateY())));

        for (DisplayAnimationStep step : animation.steps()) {
            elapsed = elapsed.add(Duration.millis(step.pauseBeforeMillis()));
            elapsed = elapsed.add(Duration.millis(step.durationMillis()));
            keyFrames.add(new KeyFrame(elapsed,
                    new KeyValue(node.opacityProperty(), step.targetOpacity(), step.interpolation().toJavaFxInterpolator()),
                    new KeyValue(node.scaleXProperty(), step.targetScaleX(), step.interpolation().toJavaFxInterpolator()),
                    new KeyValue(node.scaleYProperty(), step.targetScaleY(), step.interpolation().toJavaFxInterpolator()),
                    new KeyValue(node.translateXProperty(), step.targetTranslateX(), step.interpolation().toJavaFxInterpolator()),
                    new KeyValue(node.translateYProperty(), step.targetTranslateY(), step.interpolation().toJavaFxInterpolator())));
        }

        Timeline timeline = new Timeline(keyFrames.toArray(KeyFrame[]::new));
        timeline.setCycleCount(animation.repeatCount());
        timeline.setAutoReverse(animation.autoReverse());
        return timeline;
    }

    private void applyFinalState(Node node, DisplayAnimationStep step) {
        node.setOpacity(step.targetOpacity());
        node.setScaleX(step.targetScaleX());
        node.setScaleY(step.targetScaleY());
        node.setTranslateX(step.targetTranslateX());
        node.setTranslateY(step.targetTranslateY());
    }
}
