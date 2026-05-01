package com.eb.javafx.content;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ContentRegistryTest {

    @Test
    void registerBaseContentMarksRequiredPortableDefinitionsWithoutValues() {
        ContentRegistry registry = new ContentRegistry();

        registry.registerBaseContent();

        assertTrue(registry.requiredDefinitionIds().contains("application.name"));
        assertTrue(registry.requiredDefinitionIds().contains("startup.route"));
        assertTrue(registry.definitions().isEmpty());
        assertThrows(IllegalStateException.class, registry::validateRules);
    }

    @Test
    void registerDefinitionProvidesRequiredValues() {
        ContentRegistry registry = new ContentRegistry();

        registry.registerRequiredDefinition("startup.route");
        registry.registerDefinition("startup.route", "main-menu");
        registry.validateRules();

        assertEquals("main-menu", registry.definition("startup.route"));
    }

    @Test
    void enginePlaceholderModuleProvidesGenericDemoContent() {
        ContentRegistry registry = new ContentRegistry();
        EnginePlaceholderContentModule module = new EnginePlaceholderContentModule();

        module.register(registry, null);
        module.validate(registry, null);

        assertEquals("NovlFX Engine", registry.definition("application.name"));
        assertEquals("main-menu", registry.definition("startup.route"));
        assertEquals("Main Menu", registry.definition("ui.mainMenu.title"));
        assertEquals("Tooltip", registry.definition("ui.tooltip.title"));
        assertEquals("Display Bindings Preview", registry.definition("ui.displayBindings.title"));
        assertEquals("Reusable JavaFX engine shell is ready.", registry.definition("ui.mainMenu.status"));
        assertFalse(registry.definitions().containsKey("migration.slice"));
        assertFalse(registry.definitions().values().stream().anyMatch(value ->
                value.contains("Alternative World") || value.contains("JAVAFX_PLAN")));
    }

    @Test
    void registerDefinitionsCanReplaceDemoValuesForApplicationModules() {
        ContentRegistry registry = new ContentRegistry();
        new EnginePlaceholderContentModule().register(registry, null);

        registry.registerDefinitions(Map.of(
                "application.name", "Custom Visual Novel",
                "startup.route", "custom-start",
                "ui.mainMenu.title", "Custom Menu"));
        registry.validateRules();

        assertEquals("Custom Visual Novel", registry.definition("application.name"));
        assertEquals("custom-start", registry.definition("startup.route"));
        assertEquals("Custom Menu", registry.definition("ui.mainMenu.title"));
    }

    @Test
    void definitionsCannotBeMutatedByCallers() {
        ContentRegistry registry = new ContentRegistry();
        registry.registerDefinition("startup.route", "main-menu");

        assertThrows(UnsupportedOperationException.class, () ->
                registry.definitions().put("startup.route", "changed"));
        assertEquals("main-menu", registry.definition("startup.route"));
    }

    @Test
    void requiredDefinitionIdsCannotBeMutatedByCallers() {
        ContentRegistry registry = new ContentRegistry();
        registry.registerRequiredDefinition("startup.route");

        assertThrows(UnsupportedOperationException.class, () ->
                registry.requiredDefinitionIds().add("changed"));
    }

    @Test
    void definitionThrowsForMissingDefinition() {
        ContentRegistry registry = new ContentRegistry();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                registry.definition("missing"));

        assertEquals("Missing required content definition: missing", exception.getMessage());
    }

    @Test
    void validateRulesReportsMissingRequiredDefinition() {
        ContentRegistry registry = new ContentRegistry();
        registry.registerRequiredDefinition("missing");

        IllegalStateException exception = assertThrows(IllegalStateException.class, registry::validateRules);

        assertEquals("Missing required content definition: missing", exception.getMessage());
    }

    @Test
    void blankDefinitionIdsFailClearly() {
        ContentRegistry registry = new ContentRegistry();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                registry.registerDefinition(" ", "value"));

        assertEquals("Definition ID must not be blank.", exception.getMessage());
    }
}
