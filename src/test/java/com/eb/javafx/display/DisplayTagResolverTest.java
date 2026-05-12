package com.eb.javafx.display;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DisplayTagResolverTest {
    private DisplayTagRegistry tagRegistry;
    private DisplayTagResolver resolver;

    @BeforeEach
    void setUp() {
        tagRegistry = new DisplayTagRegistry();
        tagRegistry.register(new DisplayTagDefinition("hero_happy", "char_hero_happy", DisplayLayer.CHARACTER, null));
        tagRegistry.register(new DisplayTagDefinition("hero_left", "char_hero_neutral", DisplayLayer.CHARACTER, "pos_left"));
        resolver = new DisplayTagResolver(tagRegistry);
    }

    @Test
    void resolvesTagToDisplayIdAndLayer() {
        DisplayTagResolution result = resolver.resolve("hero_happy");
        assertEquals("char_hero_happy", result.displayId());
        assertEquals(DisplayLayer.CHARACTER, result.layer());
        assertTrue(result.transformPresetName().isEmpty());
    }

    @Test
    void resolvesTagWithTransformPreset() {
        DisplayTagResolution result = resolver.resolve("hero_left");
        assertEquals("pos_left", result.transformPresetName().get());
    }

    @Test
    void throwsForUnknownTag() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> resolver.resolve("ghost_npc"));
        assertTrue(ex.getMessage().contains("ghost_npc"));
    }
}
