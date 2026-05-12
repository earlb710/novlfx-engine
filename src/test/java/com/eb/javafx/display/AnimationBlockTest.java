package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class AnimationBlockTest {

    private static DisplayAnimationStep step() {
        return new DisplayAnimationStep(500L, 0L, 1.0, 1.0, 1.0, 0.0, 0.0, DisplayInterpolation.LINEAR);
    }

    private static DisplayAnimation animation(String id) {
        return new DisplayAnimation(id, List.of(step()), 1, false);
    }

    @Test
    void animationBlockTypeValues() {
        assertEquals(3, AnimationBlockType.values().length);
        assertNotNull(AnimationBlockType.valueOf("IDLE"));
        assertNotNull(AnimationBlockType.valueOf("TALKING"));
        assertNotNull(AnimationBlockType.valueOf("EVENT"));
    }

    @Test
    void animationEventTriggerValues() {
        assertNotNull(AnimationEventTrigger.valueOf("SHOW"));
        assertNotNull(AnimationEventTrigger.valueOf("HIDE"));
        assertNotNull(AnimationEventTrigger.valueOf("CLICK"));
    }

    @Test
    void constructsAnimationBlock() {
        AnimationBlock block = new AnimationBlock("walk-cycle", AnimationBlockType.IDLE, animation("walk"), null);
        assertEquals("walk-cycle", block.id());
        assertEquals(AnimationBlockType.IDLE, block.type());
        assertEquals("walk", block.animation().id());
        assertTrue(block.trigger().isEmpty());
    }

    @Test
    void constructsAnimationBlockWithTrigger() {
        AnimationBlock block = new AnimationBlock("show-anim", AnimationBlockType.EVENT, animation("fade-in"), AnimationEventTrigger.SHOW);
        assertTrue(block.trigger().isPresent());
        assertEquals(AnimationEventTrigger.SHOW, block.trigger().get());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
            () -> new AnimationBlock("", AnimationBlockType.IDLE, animation("a"), null));
    }

    @Test
    void rejectsNullType() {
        assertThrows(IllegalArgumentException.class,
            () -> new AnimationBlock("id", null, animation("a"), null));
    }

    @Test
    void rejectsNullAnimation() {
        assertThrows(IllegalArgumentException.class,
            () -> new AnimationBlock("id", AnimationBlockType.IDLE, null, null));
    }
}
