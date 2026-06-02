package com.eb.javafx.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the global text-size font-size rewriter. */
final class FontScalingTest {

    @Test
    void scalesPxPtAndEmFontSizes() {
        String css = ".a { -fx-font-size: 20px; } .b { -fx-font-size: 12pt; } .c { -fx-font-size: 1.5em; }";
        String scaled = FontScaling.scale(css, 1.5);
        assertTrue(scaled.contains("-fx-font-size: 30px;"), scaled);
        assertTrue(scaled.contains("-fx-font-size: 18pt;"), scaled);
        assertTrue(scaled.contains("-fx-font-size: 2.3em;"), scaled); // 1.5 * 1.5 = 2.25 -> 2.3
    }

    @Test
    void leavesNonFontSizesUntouched() {
        String css = ".a { -fx-background-radius: 18px; -fx-font-size: 10px; -fx-padding: 8px; }";
        String scaled = FontScaling.scale(css, 2.0);
        assertTrue(scaled.contains("-fx-background-radius: 18px;"));
        assertTrue(scaled.contains("-fx-padding: 8px;"));
        assertTrue(scaled.contains("-fx-font-size: 20px;"));
    }

    @Test
    void scaleOfOneIsIdentity() {
        String css = ".a { -fx-font-size: 13px; }";
        assertEquals(css, FontScaling.scale(css, 1.0));
    }

    @Test
    void nullAndEmptyAreSafe() {
        assertEquals(null, FontScaling.scale(null, 1.5));
        assertEquals("", FontScaling.scale("", 1.5));
    }

    @Test
    void dropsTrailingZeroDecimal() {
        // 16 * 0.85 = 13.6 (keeps decimal); 10 * 0.85 = 8.5 (keeps); 20 * 0.85 = 17.0 -> "17"
        assertTrue(FontScaling.scale(".a { -fx-font-size: 20px; }", 0.85).contains("17px"));
    }
}
