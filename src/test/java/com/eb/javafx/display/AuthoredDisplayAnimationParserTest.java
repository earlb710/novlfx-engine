package com.eb.javafx.display;

import javafx.animation.Animation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuthoredDisplayAnimationParserTest {
    @Test
    void parsesMultipleAnimationBlocksWithReusableVisualNovelCommands() {
        List<AuthoredDisplayAnimation> animations = AuthoredDisplayAnimationParser.parseDocument("""
                animation character.enter
                repeat 2
                autoreverse true
                pause 150
                move 500 translateX 12 translateY -4 ease_out
                fade 300 opacity 0.75 linear
                scale 250 scaleX 1.1 scaleY 1.2 ease_both
                rotate 125 15 ease_in
                end

                animation character.idle
                repeat indefinite
                scale 120 1.05
                step 80 pauseBefore 20 opacity 1 scaleX 1 scaleY 1 translateX 0 translateY 0 rotate -5 discrete
                end
                """, "animations.atl");

        assertEquals(2, animations.size());
        AuthoredDisplayAnimation enter = animations.get(0);
        assertEquals("character.enter", enter.id());
        assertEquals("animations.atl", enter.sourceName());
        assertEquals(1, enter.lineNumber());
        assertEquals(2, enter.repeatCount());
        assertEquals(true, enter.autoReverse());
        assertEquals(5, enter.steps().size());

        DisplayAnimation compiled = enter.compile();
        assertEquals(2, compiled.repeatCount());
        assertEquals(true, compiled.autoReverse());
        assertEquals(150, compiled.steps().get(0).pauseBeforeMillis());
        assertEquals(12.0, compiled.steps().get(1).targetTranslateX());
        assertEquals(0.75, compiled.steps().get(2).targetOpacity());
        assertEquals(1.2, compiled.steps().get(3).targetScaleY());
        assertEquals(DisplayInterpolation.EASE_BOTH, compiled.steps().get(3).interpolation());
        assertEquals(15.0, compiled.steps().get(4).targetRotate());
        assertEquals(DisplayInterpolation.EASE_IN, compiled.steps().get(4).interpolation());

        assertEquals(Animation.INDEFINITE, animations.get(1).repeatCount());
        assertEquals(DisplayInterpolation.DISCRETE, animations.get(1).compile().steps().get(1).interpolation());
        assertEquals(-5.0, animations.get(1).compile().steps().get(1).targetRotate());
    }

    @Test
    void parsesSingleJsonAnimationBodyWithoutAnimationBlock() {
        AuthoredDisplayAnimation animation = AuthoredDisplayAnimationParser.parseAnimation(
                "speaker.fade",
                List.of("pause 50", "fade 100 opacity 0.25 ease-in"),
                "display.json",
                4,
                3,
                false);

        assertEquals("speaker.fade", animation.id());
        assertEquals(4, animation.lineNumber());
        assertEquals(2, animation.steps().size());
        assertEquals(5, animation.steps().get(1).lineNumber());
        assertEquals(DisplayInterpolation.EASE_IN, animation.compile().steps().get(1).interpolation());
    }

    @Test
    void rejectsMalformedCommandsWithSourceDiagnostics() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                AuthoredDisplayAnimationParser.parseDocument("""
                        animation bad
                        wobble 10
                        """, "bad.atl"));

        assertEquals("bad.atl:2: unknown authored animation command: wobble", exception.getMessage());
    }

    @Test
    void rejectsInvalidValuesAndUnsupportedInterpolations() {
        IllegalArgumentException negativeDuration = assertThrows(IllegalArgumentException.class, () ->
                AuthoredDisplayAnimationParser.parseDocument("""
                        animation bad
                        pause -1
                        """, "bad.atl"));
        assertTrue(negativeDuration.getMessage().contains("pause duration must be zero or positive"));

        IllegalArgumentException interpolation = assertThrows(IllegalArgumentException.class, () ->
                AuthoredDisplayAnimationParser.parseDocument("""
                        animation bad
                        fade 10 opacity 0.5 springy
                        """, "bad.atl"));
        assertEquals("bad.atl:2: unsupported interpolation: springy", interpolation.getMessage());
    }

    @Test
    void parsesNamedRotateCommandForm() {
        AuthoredDisplayAnimation animation = AuthoredDisplayAnimationParser.parseAnimation(
                "speaker.turn",
                List.of("rotate 90 rotation 45 ease-out"),
                "display.json",
                7,
                1,
                false);

        assertEquals(45.0, animation.compile().steps().get(0).targetRotate());
        assertEquals(DisplayInterpolation.EASE_OUT, animation.compile().steps().get(0).interpolation());
    }

    @Test
    void parsesEachAtlCommandForm() {
        AuthoredDisplayAnimation animation = AuthoredDisplayAnimationParser.parseDocument("""
                animation speaker.full
                repeat 3
                autoreverse false
                pause 25
                fade 50 opacity 0.4 linear
                move 60 translateX 10 translateY -5 ease_out
                scale 70 1.25 ease_in
                scale 80 scaleX 1.5 scaleY 0.75 ease_both
                rotate 90 33 linear
                rotate 100 rotation -12 ease_out
                clip 110 x 1 y 2 width 30 height 40 discrete
                viewport 120 x 5 y 6 width 70 height 80 ease_in
                blur 130 7 linear
                blur 140 radius 8 ease_out
                dropShadow 150 radius 9 offsetX 3 offsetY -4 ease_both
                colorAdjust 160 hue 0.1 saturation 0.2 brightness -0.3 contrast 0.4 discrete
                step 170 pauseBeforeMillis 15 opacity 0.6 scaleX 1.1 scaleY 0.9 translateX 12 translateY -8 rotation 18 clipX 11 clipY 12 clipWidth 31 clipHeight 41 viewportX 15 viewportY 16 viewportWidth 71 viewportHeight 81 dropShadowRadius 10 dropShadowOffsetX 4 dropShadowOffsetY -6 colorAdjustHue -0.1 colorAdjustSaturation -0.2 colorAdjustBrightness 0.3 colorAdjustContrast -0.4 interpolation ease_in blurRadius 6
                end
                """, "all-commands.atl").get(0);

        DisplayAnimation compiled = animation.compile();
        assertEquals(3, compiled.repeatCount());
        assertEquals(false, compiled.autoReverse());
        assertEquals(14, compiled.steps().size());

        assertEquals(25, compiled.steps().get(0).pauseBeforeMillis());
        assertEquals(0.4, compiled.steps().get(1).targetOpacity());
        assertEquals(10.0, compiled.steps().get(2).targetTranslateX());
        assertEquals(-5.0, compiled.steps().get(2).targetTranslateY());
        assertEquals(1.25, compiled.steps().get(3).targetScaleX());
        assertEquals(0.75, compiled.steps().get(4).targetScaleY());
        assertEquals(33.0, compiled.steps().get(5).targetRotate());
        assertEquals(-12.0, compiled.steps().get(6).targetRotate());
        assertEquals(30.0, compiled.steps().get(7).targetClipBounds().width());
        assertEquals(80.0, compiled.steps().get(8).targetViewportBounds().height());
        assertEquals(7.0, compiled.steps().get(9).targetEffects().blurRadius());
        assertEquals(8.0, compiled.steps().get(10).targetEffects().blurRadius());
        assertEquals(9.0, compiled.steps().get(11).targetEffects().dropShadowRadius());
        assertEquals(-4.0, compiled.steps().get(11).targetEffects().dropShadowOffsetY());
        assertEquals(0.4, compiled.steps().get(12).targetEffects().colorAdjustContrast());

        DisplayAnimationStep step = compiled.steps().get(13);
        assertEquals(15, step.pauseBeforeMillis());
        assertEquals(0.6, step.targetOpacity());
        assertEquals(18.0, step.targetRotate());
        assertEquals(31.0, step.targetClipBounds().width());
        assertEquals(81.0, step.targetViewportBounds().height());
        assertEquals(6.0, step.targetEffects().blurRadius());
        assertEquals(10.0, step.targetEffects().dropShadowRadius());
        assertEquals(-6.0, step.targetEffects().dropShadowOffsetY());
        assertEquals(-0.4, step.targetEffects().colorAdjustContrast());
        assertEquals(DisplayInterpolation.EASE_IN, step.interpolation());
    }

    @Test
    void parsesClipViewportAndEffectCommands() {
        AuthoredDisplayAnimation animation = AuthoredDisplayAnimationParser.parseAnimation(
                "speaker.fx",
                List.of(
                        "clip 50 x 1 y 2 width 30 height 40 linear",
                        "viewport 60 x 5 y 6 width 70 height 80 ease_in",
                        "blur 70 radius 8 ease_out",
                        "dropShadow 80 radius 5 offsetX 2 offsetY -3 ease_both",
                        "colorAdjust 90 hue 0.1 saturation 0.2 brightness -0.1 contrast 0.3 linear",
                        "step 100 clipX 3 clipY 4 clipWidth 32 clipHeight 42 viewportX 7 viewportY 8 viewportWidth 72 viewportHeight 82 blurRadius 4 shadowRadius 6 shadowOffsetX -1 shadowOffsetY 3 hue 0.2 saturation 0.3 brightness 0.1 contrast 0.4 discrete"),
                "display.json",
                11,
                1,
                false);

        DisplayAnimation compiled = animation.compile();
        assertEquals(6, compiled.steps().size());
        assertEquals(30.0, compiled.steps().get(0).targetClipBounds().width());
        assertEquals(5.0, compiled.steps().get(1).targetViewportBounds().x());
        assertEquals(8.0, compiled.steps().get(2).targetEffects().blurRadius());
        assertEquals(-3.0, compiled.steps().get(3).targetEffects().dropShadowOffsetY());
        assertEquals(0.3, compiled.steps().get(4).targetEffects().colorAdjustContrast());
        assertEquals(32.0, compiled.steps().get(5).targetClipBounds().width());
        assertEquals(82.0, compiled.steps().get(5).targetViewportBounds().height());
        assertEquals(4.0, compiled.steps().get(5).targetEffects().blurRadius());
        assertEquals(6.0, compiled.steps().get(5).targetEffects().dropShadowRadius());
        assertEquals(0.4, compiled.steps().get(5).targetEffects().colorAdjustContrast());
        assertEquals(DisplayInterpolation.DISCRETE, compiled.steps().get(5).interpolation());
    }

    @Test
    void rejectsDuplicateIdsAndEmptyAnimations() {
        IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class, () ->
                AuthoredDisplayAnimationParser.parseDocument("""
                        animation same
                        pause 1
                        end
                        animation same
                        pause 1
                        """, "dup.atl"));
        assertEquals("dup.atl:4: duplicate authored animation id: same", duplicate.getMessage());

        IllegalArgumentException empty = assertThrows(IllegalArgumentException.class, () ->
                AuthoredDisplayAnimationParser.parseDocument("""
                        animation empty
                        end
                        """, "empty.atl"));
        assertEquals("Authored animation steps are required.", empty.getMessage());
    }
}
