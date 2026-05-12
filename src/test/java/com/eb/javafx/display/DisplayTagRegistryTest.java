package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

final class DisplayTagRegistryTest {

    @Test
    void registersAndFindsTag() {
        DisplayTagRegistry registry = new DisplayTagRegistry();
        registry.register(new DisplayTagDefinition("hero_happy", "char_hero_happy", DisplayLayer.CHARACTER, null));
        Optional<DisplayTagDefinition> found = registry.find("hero_happy");
        assertTrue(found.isPresent());
        assertEquals("char_hero_happy", found.get().displayId());
    }

    @Test
    void returnsEmptyForUnknownTag() {
        DisplayTagRegistry registry = new DisplayTagRegistry();
        assertTrue(registry.find("unknown").isEmpty());
    }

    @Test
    void rejectsDuplicateTag() {
        DisplayTagRegistry registry = new DisplayTagRegistry();
        registry.register(new DisplayTagDefinition("hero", "char_hero_a", DisplayLayer.CHARACTER, null));
        assertThrows(IllegalArgumentException.class,
            () -> registry.register(new DisplayTagDefinition("hero", "char_hero_b", DisplayLayer.CHARACTER, null)));
    }

    @Test
    void requireThrowsForUnknownTag() {
        DisplayTagRegistry registry = new DisplayTagRegistry();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> registry.require("ghost"));
        assertTrue(ex.getMessage().contains("ghost"));
    }
}
