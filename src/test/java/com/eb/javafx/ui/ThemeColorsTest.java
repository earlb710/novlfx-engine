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
    void lightenBlendsEachChannelTowardWhiteAndClampsToHex() {
        assertEquals("#808080", ThemeColors.lighten("#000000", 0.5));
        assertEquals("#ffffff", ThemeColors.lighten("#000000", 1.0));
        assertEquals("#888888", ThemeColors.lighten("#888888", 0.0));
    }

    @Test
    void lightenReturnsInputUnchangedOnBadHexAndNullOnNull() {
        assertEquals("not-a-color", ThemeColors.lighten("not-a-color", 0.5));
        assertNull(ThemeColors.lighten(null, 0.5));
    }

    @Test
    void toCssRgbFormatsChannelsZeroToTwoFiftyFive() {
        assertEquals("rgb(255,255,255)", ThemeColors.toCssRgb(Color.WHITE));
        assertEquals("rgb(0,0,0)", ThemeColors.toCssRgb(Color.BLACK));
        assertEquals("rgb(255,0,0)", ThemeColors.toCssRgb(Color.color(1, 0, 0)));
    }

    @Test
    void isLightThemeFalseForNullTheme() {
        assertFalse(ThemeColors.isLightTheme(null));
    }
}
