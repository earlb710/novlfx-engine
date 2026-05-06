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
                end

                animation character.idle
                repeat indefinite
                scale 120 1.05
                step 80 pauseBefore 20 opacity 1 scaleX 1 scaleY 1 translateX 0 translateY 0 discrete
                end
                """, "animations.atl");

        assertEquals(2, animations.size());
        AuthoredDisplayAnimation enter = animations.get(0);
        assertEquals("character.enter", enter.id());
        assertEquals("animations.atl", enter.sourceName());
        assertEquals(1, enter.lineNumber());
        assertEquals(2, enter.repeatCount());
        assertEquals(true, enter.autoReverse());
        assertEquals(4, enter.steps().size());

        DisplayAnimation compiled = enter.compile();
        assertEquals(2, compiled.repeatCount());
        assertEquals(true, compiled.autoReverse());
        assertEquals(150, compiled.steps().get(0).pauseBeforeMillis());
        assertEquals(12.0, compiled.steps().get(1).targetTranslateX());
        assertEquals(0.75, compiled.steps().get(2).targetOpacity());
        assertEquals(1.2, compiled.steps().get(3).targetScaleY());
        assertEquals(DisplayInterpolation.EASE_BOTH, compiled.steps().get(3).interpolation());

        assertEquals(Animation.INDEFINITE, animations.get(1).repeatCount());
        assertEquals(DisplayInterpolation.DISCRETE, animations.get(1).compile().steps().get(1).interpolation());
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
