package com.eb.javafx.transitions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class VisualTransitionDefinitionTest {

    @Test
    void constructsWithValidFields() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("dissolve-fast", SceneTransitionEffect.DISSOLVE, 300);
        assertEquals("dissolve-fast", def.id());
        assertEquals(SceneTransitionEffect.DISSOLVE, def.effect());
        assertEquals(300, def.durationMs());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
            () -> new VisualTransitionDefinition("", SceneTransitionEffect.DISSOLVE, 300));
    }

    @Test
    void rejectsNegativeDuration() {
        assertThrows(IllegalArgumentException.class,
            () -> new VisualTransitionDefinition("t", SceneTransitionEffect.DISSOLVE, -1));
    }

    @Test
    void noneEffectAllowsZeroDuration() {
        VisualTransitionDefinition def = new VisualTransitionDefinition("instant", SceneTransitionEffect.NONE, 0);
        assertEquals(0, def.durationMs());
    }
}
