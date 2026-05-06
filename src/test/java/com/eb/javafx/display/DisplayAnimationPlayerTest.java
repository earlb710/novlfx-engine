package com.eb.javafx.display;

import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DisplayAnimationPlayerTest {

    @Test
    void buildTimelineCreatesRepeatableAnimatedKeyFrames() {
        DisplayAnimation animation = new DisplayAnimation(
                "pulse",
                List.of(
                        new DisplayAnimationStep(120, 0, 0.8, 1.0, 1.0, 0.0, 0.0, 0.0, DisplayInterpolation.LINEAR),
                        new DisplayAnimationStep(180, 60, 1.0, 1.1, 1.1, 3.0, -2.0, 12.0, DisplayInterpolation.EASE_BOTH)),
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
                List.of(new DisplayAnimationStep(120, 30, 0.6, 1.2, 1.2, 4.0, -1.0, 18.0, DisplayInterpolation.EASE_OUT)),
                1,
                false);

        Timeline timeline = new DisplayAnimationPlayer().buildTimeline(node, animation, true);

        assertEquals(0.6, node.getOpacity());
        assertEquals(1.2, node.getScaleX());
        assertEquals(4.0, node.getTranslateX());
        assertEquals(18.0, node.getRotate());
        assertEquals(1, timeline.getKeyFrames().size());
    }

    @Test
    void buildTimelineAnimatesClipViewportAndEffects() {
        ImageView node = new ImageView(new WritableImage(100, 100));
        DisplayAnimation animation = new DisplayAnimation(
                "fx",
                List.of(new DisplayAnimationStep(
                        120,
                        0,
                        1.0,
                        1.0,
                        1.0,
                        0.0,
                        0.0,
                        0.0,
                        new DisplayRectangleBounds(1, 2, 30, 40),
                        new DisplayRectangleBounds(5, 6, 70, 80),
                        new DisplayEffectTargets(true, 8.0, true, 5.0, 2.0, -3.0, true, 0.1, 0.2, -0.1, 0.3),
                        DisplayInterpolation.EASE_BOTH)),
                1,
                false);

        Timeline timeline = new DisplayAnimationPlayer().buildTimeline(node, animation, false);

        assertEquals(2, timeline.getKeyFrames().size());
        assertEquals(Rectangle.class, node.getClip().getClass());
        assertEquals(5.0, node.getViewport().getMinX());
        assertEquals(GaussianBlur.class, node.getEffect().getClass());
    }
}
