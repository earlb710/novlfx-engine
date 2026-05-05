package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DisplayDefaultsTest {
    @Test
    void bundledDefaultsExposeScreenBlockAndRoleDefaults() {
        DisplayDefaults defaults = DisplayDefaults.defaults();

        assertEquals("#0a1426", defaults.screen().get("backgroundColor"));
        assertEquals("#143869", defaults.block().get("backgroundColor"));
        assertEquals("28", defaults.itemDefaults(DisplayDefaults.ROLE_HEADING).get("fontSize"));
        assertEquals("bold", defaults.labelDefaults(DisplayDefaults.ROLE_FIELD_LABEL).get("fontStyle"));
    }

    @Test
    void defaultsCanBeLoadedFromJson() {
        DisplayDefaults defaults = DisplayDefaults.fromJson("""
                {
                  "screen": {"fontSize": "20"},
                  "block": {"backgroundColor": "#123456"},
                  "items": {"heading": {"fontStyle": "bold"}},
                  "labels": {"fieldLabel": {"color": "#eeeeee"}}
                }
                """, "inline");

        assertEquals("20", defaults.screen().get("fontSize"));
        assertEquals("#123456", defaults.block().get("backgroundColor"));
        assertEquals("bold", defaults.itemDefaults(DisplayDefaults.ROLE_HEADING).get("fontStyle"));
        assertEquals("#eeeeee", defaults.labelDefaults(DisplayDefaults.ROLE_FIELD_LABEL).get("color"));
        assertTrue(defaults.itemDefaults(DisplayDefaults.ROLE_TEXT).isEmpty());
    }
}
