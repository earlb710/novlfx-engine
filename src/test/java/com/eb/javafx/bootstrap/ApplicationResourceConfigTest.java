package com.eb.javafx.bootstrap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ApplicationResourceConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void configRoundTripsThroughJsonAndResolvesNamedResources() throws Exception {
        ApplicationResourceConfig original = ApplicationResourceConfig.defaults()
                .withDebug(false)
                .withCategoryCodeTablesPath("data/categories/en.json")
                .withImageAssetRoot("assets/images")
                .withDefaultAppBackgroundColor("#101820")
                .withDefaultAppBackgroundImage("backgrounds/app.png")
                .withDefaultAppBackgroundImageTransparency("0.2")
                .withDefaultPreferencesScreenBackgroundColor("#203040")
                .withDefaultPreferencesScreenBackgroundImage("backgrounds/preferences.png")
                .withDefaultPreferencesScreenBackgroundImageTransparency("0.35")
                .withDefaultSaveLoadScreenBackgroundColor("#304050")
                .withDefaultSaveLoadScreenBackgroundImage("backgrounds/save-load.png")
                .withDefaultSaveLoadScreenBackgroundImageTransparency("0.5")
                .withJsonResourceRoot("resources/json")
                .putResource("backgrounds", "assets/backgrounds")
                .putResource("portraits", "assets/portraits");
        Path output = tempDir.resolve("config.json");

        original.save(output);
        ApplicationResourceConfig reloaded = ApplicationResourceConfig.load(output);

        assertFalse(reloaded.debug());
        assertEquals("data/categories/en.json", reloaded.categoryCodeTablesPath());
        assertEquals("assets/images", reloaded.imageAssetRoot());
        assertEquals("#101820", reloaded.defaultAppBackgroundColor());
        assertEquals("backgrounds/app.png", reloaded.defaultAppBackgroundImage());
        assertEquals("0.2", reloaded.defaultAppBackgroundImageTransparency());
        assertEquals("#203040", reloaded.defaultPreferencesScreenBackgroundColor());
        assertEquals("backgrounds/preferences.png", reloaded.defaultPreferencesScreenBackgroundImage());
        assertEquals("0.35", reloaded.defaultPreferencesScreenBackgroundImageTransparency());
        assertEquals("#304050", reloaded.defaultSaveLoadScreenBackgroundColor());
        assertEquals("backgrounds/save-load.png", reloaded.defaultSaveLoadScreenBackgroundImage());
        assertEquals("0.5", reloaded.defaultSaveLoadScreenBackgroundImageTransparency());
        assertEquals(
                tempDir.resolve("data/categories/en.json").normalize(),
                reloaded.resolveCategoryCodeTables(tempDir));
        assertEquals(
                tempDir.resolve("assets/images").normalize(),
                reloaded.resolveImageAssetRoot(tempDir));
        assertEquals(
                tempDir.resolve("resources/json").normalize(),
                reloaded.resolveJsonResourceRoot(tempDir));
        assertEquals(
                tempDir.resolve("assets/backgrounds").normalize(),
                reloaded.resolveResource(tempDir, "backgrounds").orElseThrow());
        assertTrue(Files.readString(output).contains("\"debug\": false"));
        assertTrue(Files.readString(output).contains("\"imageAssetRoot\": \"assets/images\""));
    }

    @Test
    void configUsesDefaultsForOmittedFieldsAndCanRemoveNamedResources() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "resources": {
                    "backgrounds": "assets/backgrounds"
                  }
                }
                """, "inline");

        assertEquals("config/category-code-tables.en.json", config.categoryCodeTablesPath());
        assertEquals("game", config.imageAssetRoot());
        assertTrue(config.debug());
        assertEquals("", config.defaultAppBackgroundColor());
        assertEquals("", config.defaultPreferencesScreenBackgroundImage());
        assertEquals("", config.defaultSaveLoadScreenBackgroundImageTransparency());
        assertEquals("resources/json", config.jsonResourceRoot());
        assertTrue(config.resourcePath("backgrounds").isPresent());
        assertFalse(config.removeResource("backgrounds").resourcePath("backgrounds").isPresent());
        assertThrows(IllegalArgumentException.class, () -> config.removeResource("missing"));
    }

    @Test
    void configParsesExplicitDebugFlag() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "debug": false
                }
                """, "inline");

        assertFalse(config.debug());
        assertTrue(ApplicationResourceConfig.defaults().debug());
    }

    @Test
    void parsesApplicationJsonLoadDefinition() {
        ApplicationJsonLoadDefinition definition = ApplicationJsonLoadDefinition.fromJson("""
                {
                  "loads": [
                    {"type": "display", "path": "display"},
                    {"type": "scene", "path": "scenes", "fileName": "intro.json"}
                  ]
                }
                """, "inline");

        assertEquals(2, definition.loads().size());
        assertEquals(ApplicationJsonLoadType.DISPLAY, definition.loads().get(0).type());
        assertEquals("display", definition.loads().get(0).path());
        assertEquals("", definition.loads().get(0).fileName());
        assertEquals("intro.json", definition.loads().get(1).fileName());
    }

    @Test
    void configParsesExplicitBackgroundDefaults() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "defaultAppBackgroundColor": "#0f172a",
                  "defaultAppBackgroundImage": "images/app.png",
                  "defaultAppBackgroundImageTransparency": "0.15",
                  "defaultPreferencesScreenBackgroundColor": "#112233",
                  "defaultPreferencesScreenBackgroundImage": "images/preferences.png",
                  "defaultPreferencesScreenBackgroundImageTransparency": "0.25",
                  "defaultSaveLoadScreenBackgroundColor": "#445566",
                  "defaultSaveLoadScreenBackgroundImage": "images/save-load.png",
                  "defaultSaveLoadScreenBackgroundImageTransparency": "0.4"
                }
                """, "inline");

        assertEquals("#0f172a", config.defaultAppBackgroundColor());
        assertEquals("images/app.png", config.defaultAppBackgroundImage());
        assertEquals("0.15", config.defaultAppBackgroundImageTransparency());
        assertEquals("#112233", config.defaultPreferencesScreenBackgroundColor());
        assertEquals("images/preferences.png", config.defaultPreferencesScreenBackgroundImage());
        assertEquals("0.25", config.defaultPreferencesScreenBackgroundImageTransparency());
        assertEquals("#445566", config.defaultSaveLoadScreenBackgroundColor());
        assertEquals("images/save-load.png", config.defaultSaveLoadScreenBackgroundImage());
        assertEquals("0.4", config.defaultSaveLoadScreenBackgroundImageTransparency());
    }

    @Test
    void factoryRejectsBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.of(" ", "game", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.of("config.json", " ", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.of(
                "config.json",
                "game",
                Map.of("images", " ")));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{\"debug\":\"true\"}",
                "inline"));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{\"defaultAppBackgroundColor\":false}",
                "inline"));
    }
}
