package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DisplayTagDefinitionTest {

    @Test
    void constructsWithRequiredFields() {
        DisplayTagDefinition tag = new DisplayTagDefinition("hero_happy", "char_hero_happy", DisplayLayer.CHARACTER, null);
        assertEquals("hero_happy", tag.tag());
        assertEquals("char_hero_happy", tag.displayId());
        assertEquals(DisplayLayer.CHARACTER, tag.layer());
        assertTrue(tag.transformPresetName().isEmpty());
    }

    @Test
    void constructsWithOptionalTransformPreset() {
        DisplayTagDefinition tag = new DisplayTagDefinition("hero_left", "char_hero", DisplayLayer.CHARACTER, "pos_left");
        assertEquals("pos_left", tag.transformPresetName().get());
    }

    @Test
    void rejectsBlankTag() {
        assertThrows(IllegalArgumentException.class,
            () -> new DisplayTagDefinition("", "char_hero", DisplayLayer.CHARACTER, null));
    }

    @Test
    void rejectsBlankDisplayId() {
        assertThrows(IllegalArgumentException.class,
            () -> new DisplayTagDefinition("hero", "", DisplayLayer.CHARACTER, null));
    }
}
