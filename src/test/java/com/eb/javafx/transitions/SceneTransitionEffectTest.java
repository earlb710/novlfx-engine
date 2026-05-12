package com.eb.javafx.transitions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SceneTransitionEffectTest {

    @Test
    void noneEffectIsInstant() {
        assertTrue(SceneTransitionEffect.NONE.isInstant());
    }

    @Test
    void nonInstantEffectHasDuration() {
        assertFalse(SceneTransitionEffect.DISSOLVE.isInstant());
        assertFalse(SceneTransitionEffect.FADE_BLACK.isInstant());
    }
}
