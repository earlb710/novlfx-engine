package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

final class LayeredImageRegistryTest {

    private LayeredImageDefinition sampleDef() {
        return new LayeredImageDefinition("hero-composite", "hero_tag", List.of(
            new LayeredImageLayer("body", List.of(new LayeredImageVariant("body/default.png", null)))
        ));
    }

    @Test
    void registersAndFindsDefinition() {
        LayeredImageRegistry registry = new LayeredImageRegistry();
        registry.register(sampleDef());
        Optional<LayeredImageDefinition> found = registry.find("hero-composite");
        assertTrue(found.isPresent());
        assertEquals("hero_tag", found.get().displayTagId());
    }

    @Test
    void returnsEmptyForUnknownId() {
        LayeredImageRegistry registry = new LayeredImageRegistry();
        assertTrue(registry.find("unknown").isEmpty());
    }

    @Test
    void rejectsDuplicateId() {
        LayeredImageRegistry registry = new LayeredImageRegistry();
        registry.register(sampleDef());
        assertThrows(IllegalArgumentException.class, () -> registry.register(sampleDef()));
    }

    @Test
    void requireThrowsForUnknownId() {
        LayeredImageRegistry registry = new LayeredImageRegistry();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> registry.require("ghost"));
        assertTrue(ex.getMessage().contains("ghost"));
    }

    @Test
    void definitionHasImmutableLayers() {
        LayeredImageDefinition def = sampleDef();
        assertThrows(UnsupportedOperationException.class, () -> def.layers().add(
            new LayeredImageLayer("hair", List.of(new LayeredImageVariant("hair/default.png", null)))));
    }

    @Test
    void rejectsBlankDefinitionId() {
        assertThrows(IllegalArgumentException.class,
            () -> new LayeredImageDefinition("", "hero_tag", List.of()));
    }
}
