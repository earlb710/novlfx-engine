package com.lr2alt.javafx.display;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JavaFX timelines from reusable ATL-style animation profiles.
 */
public final class DisplayAnimationPlayer {

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
