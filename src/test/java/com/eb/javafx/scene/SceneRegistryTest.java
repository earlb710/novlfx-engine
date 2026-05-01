package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SceneRegistryTest {

    @Test
    void registersScenesInImmutableStartupRegistry() {
        SceneRegistry registry = new SceneRegistry();

        registry.register(SceneDefinition.of("intro", List.of(SceneStep.narration("line", "intro.line"))));

        assertFalse(registry.isEmpty());
        assertTrue(registry.scene("intro").isPresent());
        assertThrows(UnsupportedOperationException.class, () -> registry.scenes().clear());
    }

    @Test
    void duplicateSceneIdsAreRejected() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("intro", List.of(SceneStep.narration("line", "intro.line"))));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                registry.register(SceneDefinition.of("intro", List.of(SceneStep.narration("other", "intro.other")))));

        assertEquals("Duplicate scene id 'intro' cannot be registered. Scene IDs must be unique across all scene modules.", exception.getMessage());
    }

    @Test
    void validationCatchesMissingJumpTarget() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("intro", List.of(
                SceneStep.transition("missing", SceneTransition.jump("missing-scene")))));

        IllegalStateException exception = assertThrows(IllegalStateException.class, registry::validateScenes);

        assertEquals("Scene 'intro' step 'missing' has JUMP transition to missing scene 'missing-scene'. Register that scene or change the transition target.", exception.getMessage());
    }

    @Test
    void validationCatchesDuplicateChoiceIds() {
        SceneRegistry registry = new SceneRegistry();
        registry.register(SceneDefinition.of("choice-scene", List.of(SceneStep.choice("choice", List.of(
                SceneChoice.of("same", "choice.one", SceneTransition.complete()),
                SceneChoice.of("same", "choice.two", SceneTransition.complete()))))));

        IllegalStateException exception = assertThrows(IllegalStateException.class, registry::validateScenes);

        assertEquals("Duplicate choice id 'same' in scene 'choice-scene' step 'choice'. Choice IDs must be unique within a choice step.", exception.getMessage());
    }
}
