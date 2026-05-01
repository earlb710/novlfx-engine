package com.lr2alt.javafx.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ContentRegistryTest {

    @Test
    void registerBaseContentProvidesRequiredDefinitions() {
        ContentRegistry registry = new ContentRegistry();

        registry.registerBaseContent();
        registry.validateRules();

        assertEquals("Alternative World JavaFX Port", registry.definition("application.name"));
        assertEquals("main-menu", registry.definition("startup.route"));
        assertEquals("Main Menu", registry.definition("ui.mainMenu.title"));
        assertEquals("Tooltip", registry.definition("ui.tooltip.title"));
        assertEquals("Display Bindings Preview", registry.definition("ui.displayBindings.title"));
        assertEquals("JAVAFX_PLAN 1.1 through 1.4 - lifecycle, screens, styles, and display bindings",
                registry.definition("migration.slice"));
    }

    @Test
    void definitionsCannotBeMutatedByCallers() {
        ContentRegistry registry = new ContentRegistry();
        registry.registerBaseContent();

        assertThrows(UnsupportedOperationException.class, () ->
                registry.definitions().put("startup.route", "changed"));
        assertEquals("main-menu", registry.definition("startup.route"));
    }

    @Test
    void definitionThrowsForMissingDefinition() {
        ContentRegistry registry = new ContentRegistry();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                registry.definition("missing"));

        assertEquals("Missing required content definition: missing", exception.getMessage());
    }
}
