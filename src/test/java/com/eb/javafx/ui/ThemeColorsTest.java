package com.eb.javafx.ui;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ThemeColorsTest {

    @Test
    void relativeLuminanceMatchesRec601() {
        assertEquals(1.0, ThemeColors.relativeLuminance(Color.WHITE), 1.0e-9);
        assertEquals(0.0, ThemeColors.relativeLuminance(Color.BLACK), 1.0e-9);
        assertEquals(0.587, ThemeColors.relativeLuminance(Color.color(0, 1, 0)), 1.0e-9);
    }

    @Test
    void darkenScalesEachChannelAndClampsToHex() {
        assertEquals("#808080", ThemeColors.darken("#ffffff", 0.5));
        assertEquals("#444444", ThemeColors.darken("#888888", 0.5));
        assertEquals("#000000", ThemeColors.darken("#ffffff", 0.0));
    }

    @Test
    void darkenReturnsInputUnchangedOnBadHexAndNullOnNull() {
        assertEquals("not-a-color", ThemeColors.darken("not-a-color", 0.5));
        assertNull(ThemeColors.darken(null, 0.5));
    }

    @Test
    void isLightThemeFalseForNullTheme() {
        assertFalse(ThemeColors.isLightTheme(null));
    }
}
