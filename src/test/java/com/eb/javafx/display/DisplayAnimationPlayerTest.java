package com.eb.javafx.display;

import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.scene.Group;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DisplayAnimationPlayerTest {

    @Test
    void buildTimelineCreatesRepeatableAnimatedKeyFrames() {
        DisplayAnimation animation = new DisplayAnimation(
                "pulse",
                List.of(
                        new DisplayAnimationStep(120, 0, 0.8, 1.0, 1.0, 0.0, 0.0, DisplayInterpolation.LINEAR),
                        new DisplayAnimationStep(180, 60, 1.0, 1.1, 1.1, 3.0, -2.0, DisplayInterpolation.EASE_BOTH)),
                Animation.INDEFINITE,
                true);

        Timeline timeline = new DisplayAnimationPlayer().buildTimeline(new Group(), animation, false);

        assertEquals(3, timeline.getKeyFrames().size());
        assertEquals(Animation.INDEFINITE, timeline.getCycleCount());
        assertEquals(true, timeline.isAutoReverse());
    }

    @Test
    void reducedMotionAppliesFinalStateWithoutAnimatedDuration() {
        Group node = new Group();
        DisplayAnimation animation = new DisplayAnimation(
                "reduced",
                List.of(new DisplayAnimationStep(120, 30, 0.6, 1.2, 1.2, 4.0, -1.0, DisplayInterpolation.EASE_OUT)),
                1,
                false);

        Timeline timeline = new DisplayAnimationPlayer().buildTimeline(node, animation, true);

        assertEquals(0.6, node.getOpacity());
        assertEquals(1.2, node.getScaleX());
        assertEquals(4.0, node.getTranslateX());
        assertEquals(1, timeline.getKeyFrames().size());
    }
}
