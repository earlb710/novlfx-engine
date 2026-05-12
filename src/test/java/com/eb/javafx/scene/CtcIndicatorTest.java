package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

final class CtcIndicatorTest {

    @Test
    void ctcIndicatorDefinitionStoresFields() {
        CtcIndicatorDefinition def = new CtcIndicatorDefinition(
                "arrow.png", Optional.of("anim-blink"), CtcPosition.BOTTOM_RIGHT);
        assertEquals("arrow.png", def.imageRef());
        assertEquals("anim-blink", def.animationId().orElseThrow());
        assertEquals(CtcPosition.BOTTOM_RIGHT, def.position());
    }

    @Test
    void ctcIndicatorDefinitionAllowsEmptyAnimation() {
        CtcIndicatorDefinition def = new CtcIndicatorDefinition(
                "arrow.png", Optional.empty(), CtcPosition.INLINE);
        assertTrue(def.animationId().isEmpty());
    }

    @Test
    void ctcIndicatorRegistryReturnsByMode() {
        CtcIndicatorRegistry registry = new CtcIndicatorRegistry();
        CtcIndicatorDefinition advDef = new CtcIndicatorDefinition(
                "adv_arrow.png", Optional.empty(), CtcPosition.INLINE);
        registry.register(SceneDisplayMode.ADV, advDef);
        Optional<CtcIndicatorDefinition> result = registry.forMode(SceneDisplayMode.ADV);
        assertTrue(result.isPresent());
        assertEquals("adv_arrow.png", result.get().imageRef());
    }

    @Test
    void ctcIndicatorRegistryReturnsEmptyForUnregisteredMode() {
        CtcIndicatorRegistry registry = new CtcIndicatorRegistry();
        assertTrue(registry.forMode(SceneDisplayMode.NVL).isEmpty());
    }

    @Test
    void ctcIndicatorRegistryCanRegisterBothModes() {
        CtcIndicatorRegistry registry = new CtcIndicatorRegistry();
        registry.register(SceneDisplayMode.ADV,
                new CtcIndicatorDefinition("adv.png", Optional.empty(), CtcPosition.BOTTOM_RIGHT));
        registry.register(SceneDisplayMode.NVL,
                new CtcIndicatorDefinition("nvl.png", Optional.empty(), CtcPosition.BOTTOM_CENTER));
        assertTrue(registry.forMode(SceneDisplayMode.ADV).isPresent());
        assertTrue(registry.forMode(SceneDisplayMode.NVL).isPresent());
    }

    @Test
    void sceneDialogueRowViewModelExposesCtcIndicator() {
        CtcIndicatorDefinition def = new CtcIndicatorDefinition(
                "arrow.png", Optional.empty(), CtcPosition.BOTTOM_CENTER);
        SceneDialogueRowViewModel vm = new SceneDialogueRowViewModel(
                SceneStepType.DIALOGUE, "char", "text.key", "display", Optional.of(def));
        assertTrue(vm.ctcIndicator().isPresent());
        assertEquals("arrow.png", vm.ctcIndicator().get().imageRef());
    }

    @Test
    void sceneDialogueRowViewModelAcceptsEmptyCtcIndicator() {
        SceneDialogueRowViewModel vm = new SceneDialogueRowViewModel(
                SceneStepType.NARRATION, null, "text.key", null, Optional.empty());
        assertTrue(vm.ctcIndicator().isEmpty());
    }
}
