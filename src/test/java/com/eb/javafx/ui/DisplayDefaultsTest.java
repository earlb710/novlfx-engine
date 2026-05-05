package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DisplayDefaultsTest {
    @Test
    void bundledDefaultsExposeScreenBlockAndRoleDefaults() {
        DisplayDefaults defaults = DisplayDefaults.defaults();

        assertEquals("", defaults.screen().get("fontFamily"));
        assertEquals("", defaults.screen().get("fontSize"));
        assertEquals("", defaults.screen().get("fontStyle"));
        assertEquals("", defaults.screen().get("color"));
        assertEquals("", defaults.screen().get("backgroundColor"));
        assertEquals("", defaults.screen().get("borderStyle"));
        assertEquals("", defaults.screen().get("borderCorner"));
        assertEquals("", defaults.screen().get("borderThickness"));
        assertEquals("", defaults.screen().get("borderColor"));
        assertEquals("#143869", defaults.block().get("backgroundColor"));
        assertEquals("0", defaults.block().get("transparency"));
        assertEquals("28", defaults.itemDefaults(DisplayDefaults.ROLE_HEADING).get("fontSize"));
        assertEquals("transparent", defaults.itemDefaults(DisplayDefaults.ROLE_TEXT).get("backgroundColor"));
        assertEquals("#0a1426", defaults.itemDefaults(DisplayDefaults.ROLE_BUTTON).get("backgroundColor"));
        assertEquals("#143869", defaults.itemDefaults(DisplayDefaults.ROLE_BUTTON).get("hoverBackgroundColor"));
        assertEquals("#0099cc", defaults.itemDefaults(DisplayDefaults.ROLE_BUTTON).get("pressedBackgroundColor"));
        assertEquals("bold", defaults.labelDefaults(DisplayDefaults.ROLE_FIELD_LABEL).get("fontStyle"));
    }

    @Test
    void defaultsCanBeLoadedFromJson() {
        DisplayDefaults defaults = DisplayDefaults.fromJson("""
                {
                  "screen": {"fontSize": "20"},
                  "block": {"backgroundColor": "#123456", "transparency": "0.4"},
                  "items": {"heading": {"fontStyle": "bold", "backgroundColor": "transparent"}},
                  "labels": {"fieldLabel": {"color": "#eeeeee"}}
                }
                """, "inline");

        assertEquals("20", defaults.screen().get("fontSize"));
        assertEquals("#123456", defaults.block().get("backgroundColor"));
        assertEquals("0.4", defaults.block().get("transparency"));
        assertEquals("bold", defaults.itemDefaults(DisplayDefaults.ROLE_HEADING).get("fontStyle"));
        assertEquals("transparent", defaults.itemDefaults(DisplayDefaults.ROLE_HEADING).get("backgroundColor"));
        assertEquals("#eeeeee", defaults.labelDefaults(DisplayDefaults.ROLE_FIELD_LABEL).get("color"));
        assertTrue(defaults.itemDefaults(DisplayDefaults.ROLE_TEXT).isEmpty());
    }
}
