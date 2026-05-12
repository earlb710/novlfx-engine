package com.eb.javafx.transitions;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

final class VisualTransitionRegistryTest {

    @Test
    void registersAndFindsDefinition() {
        VisualTransitionRegistry registry = new VisualTransitionRegistry();
        registry.register(new VisualTransitionDefinition("fade", SceneTransitionEffect.FADE_BLACK, 500));
        Optional<VisualTransitionDefinition> found = registry.find("fade");
        assertTrue(found.isPresent());
        assertEquals(SceneTransitionEffect.FADE_BLACK, found.get().effect());
    }

    @Test
    void returnsEmptyForUnknownId() {
        VisualTransitionRegistry registry = new VisualTransitionRegistry();
        assertTrue(registry.find("unknown").isEmpty());
    }

    @Test
    void rejectsDuplicateId() {
        VisualTransitionRegistry registry = new VisualTransitionRegistry();
        registry.register(new VisualTransitionDefinition("t", SceneTransitionEffect.DISSOLVE, 300));
        assertThrows(IllegalArgumentException.class,
            () -> registry.register(new VisualTransitionDefinition("t", SceneTransitionEffect.FADE_BLACK, 200)));
    }
}
