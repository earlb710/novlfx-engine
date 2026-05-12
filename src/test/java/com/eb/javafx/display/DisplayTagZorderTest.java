package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DisplayTagZorderTest {

    @Test
    void defaultZorderIsZero() {
        DisplayTagDefinition tag = new DisplayTagDefinition("hero", "char_hero", DisplayLayer.CHARACTER, null);
        assertEquals(0, tag.zorder());
    }

    @Test
    void zorderConstructorSetsField() {
        DisplayTagDefinition tag = new DisplayTagDefinition("hero", "char_hero", DisplayLayer.CHARACTER, null, 2);
        assertEquals(2, tag.zorder());
    }

    @Test
    void withZorderReturnsCopyWithNewZorder() {
        DisplayTagDefinition tag = new DisplayTagDefinition("hero", "char_hero", DisplayLayer.CHARACTER, null);
        DisplayTagDefinition updated = tag.withZorder(4);
        assertEquals(4, updated.zorder());
        assertEquals(0, tag.zorder());
        assertEquals("hero", updated.tag());
        assertEquals("char_hero", updated.displayId());
    }
}
