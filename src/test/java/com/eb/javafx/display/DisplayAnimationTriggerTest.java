package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class DisplayAnimationTriggerTest {

    private static DisplayAnimationStep step() {
        return new DisplayAnimationStep(500L, 0L, 1.0, 1.0, 1.0, 0.0, 0.0, DisplayInterpolation.LINEAR);
    }

    @Test
    void defaultAnimationHasNoTrigger() {
        DisplayAnimation anim = new DisplayAnimation("idle", List.of(step()), 1, false);
        assertTrue(anim.trigger().isEmpty());
    }

    @Test
    void withTriggerReturnsCopyWithTrigger() {
        DisplayAnimation anim = new DisplayAnimation("fade", List.of(step()), 1, false);
        DisplayAnimation triggered = anim.withTrigger(AnimationEventTrigger.SHOW);
        assertTrue(triggered.trigger().isPresent());
        assertEquals(AnimationEventTrigger.SHOW, triggered.trigger().get());
        assertTrue(anim.trigger().isEmpty());
        assertEquals("fade", triggered.id());
    }

    @Test
    void forTriggerFiltersFromList() {
        DisplayAnimationStep s = step();
        DisplayAnimation showAnim = new DisplayAnimation("show", List.of(s), 1, false).withTrigger(AnimationEventTrigger.SHOW);
        DisplayAnimation hideAnim = new DisplayAnimation("hide", List.of(s), 1, false).withTrigger(AnimationEventTrigger.HIDE);
        DisplayAnimation untriggered = new DisplayAnimation("idle", List.of(s), 1, false);

        List<DisplayAnimation> all = List.of(showAnim, hideAnim, untriggered);

        List<DisplayAnimation> showOnly = DisplayAnimation.forTrigger(all, AnimationEventTrigger.SHOW);
        assertEquals(1, showOnly.size());
        assertEquals("show", showOnly.get(0).id());
    }
}
