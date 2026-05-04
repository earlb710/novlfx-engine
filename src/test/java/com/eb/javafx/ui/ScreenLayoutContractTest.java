package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenLayoutContractTest {
    @Test
    void loadsDefaultJsonLayoutContract() {
        ScreenLayoutContract contract = ScreenLayoutContract.defaultContract();

        assertEquals("com/eb/javafx/ui/default.css", contract.stylesheet());
        assertTrue(contract.layoutTypes().contains("sidebar-content"));
        assertTrue(contract.stableStyleClasses().contains(ScreenShell.LAYOUT_SIDEBAR_STYLE_CLASS));
        assertTrue(contract.stableStyleClasses().contains(ScreenShell.LAYOUT_PRIMARY_ACTION_STYLE_CLASS));
    }

    @Test
    void parsesLayoutContractFromJson() {
        ScreenLayoutContract contract = ScreenLayoutContract.fromJson("""
                {
                  "stylesheet": "theme.css",
                  "layoutTypes": ["dialogue"],
                  "stableStyleClasses": ["layout-dialogue"]
                }
                """, "test");

        assertEquals("theme.css", contract.stylesheet());
        assertEquals(List.of("dialogue"), contract.layoutTypes());
        assertEquals(List.of("layout-dialogue"), contract.stableStyleClasses());
    }

    @Test
    void validatesLayoutContractJson() {
        assertThrows(IllegalArgumentException.class, () -> ScreenLayoutContract.fromJson("""
                {
                  "stylesheet": "theme.css",
                  "layoutTypes": [],
                  "stableStyleClasses": ["layout-dialogue"]
                }
                """, "test"));
        assertThrows(IllegalArgumentException.class, () -> ScreenLayoutContract.fromJson("""
                {
                  "layoutTypes": ["dialogue"],
                  "stableStyleClasses": ["layout-dialogue"]
                }
                """, "test"));
    }
}
