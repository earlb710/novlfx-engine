package com.eb.javafx.bootstrap;

import com.eb.javafx.resources.ResourceCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
                .withDefaultAppBackgroundColor("#101820")
                .withDefaultAppBackgroundImage("backgrounds/app.png")
                .withDefaultAppBackgroundImageTransparency("0.2")
                .withDefaultPreferencesScreenBackgroundColor("#203040")
                .withDefaultPreferencesScreenBackgroundImage("backgrounds/preferences.png")
                .withDefaultPreferencesScreenBackgroundImageTransparency("0.35")
                .withDefaultSaveLoadScreenBackgroundColor("#304050")
                .withDefaultSaveLoadScreenBackgroundImage("backgrounds/save-load.png")
                .withDefaultSaveLoadScreenBackgroundImageTransparency("0.5")
                .putResource("backgrounds", "assets/backgrounds")
                .putResource("portraits", "assets/portraits");
        Path output = tempDir.resolve("config.json");

        original.save(output);
        ApplicationResourceConfig reloaded = ApplicationResourceConfig.load(output);

        assertFalse(reloaded.debug());
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
                tempDir.resolve("assets/backgrounds").normalize(),
                reloaded.resolveResource(tempDir, "backgrounds").orElseThrow());
        assertTrue(Files.readString(output).contains("\"debug\": false"));
        assertTrue(Files.readString(output).contains("\"defaultAppBackgroundColor\": \"#101820\""));
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

        assertTrue(config.debug());
        assertEquals("", config.defaultAppBackgroundColor());
        assertEquals("", config.defaultPreferencesScreenBackgroundImage());
        assertEquals("", config.defaultSaveLoadScreenBackgroundImageTransparency());
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
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.of(Map.of("images", " ")));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{\"debug\":\"true\"}",
                "inline"));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson(
                "{\"defaultAppBackgroundColor\":false}",
                "inline"));
    }

    @Test
    void resourceRootsParsePerCategoryAndPreserveOrder() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "resourceRoots": {
                    "ui": ["classpath:/com/eb/javafx/ui", "app/ui"],
                    "fonts": ["classpath:/com/eb/javafx/fonts"]
                  }
                }
                """, "inline");

        assertEquals(List.of("classpath:/com/eb/javafx/ui", "app/ui"),
                config.resourceRoots(ResourceCategory.UI));
        assertEquals(List.of("classpath:/com/eb/javafx/fonts"),
                config.resourceRoots(ResourceCategory.FONTS));
        assertTrue(config.resourceRoots(ResourceCategory.IMAGES).isEmpty());
    }

    @Test
    void resourceRootsRejectUnknownCategory() {
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson("""
                {
                  "resourceRoots": {
                    "unknown": ["something"]
                  }
                }
                """, "inline"));
    }

    @Test
    void resourceRootsRejectBlankEntries() {
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.fromJson("""
                {
                  "resourceRoots": {
                    "ui": [" "]
                  }
                }
                """, "inline"));
    }

    @Test
    void resourceRootsRoundTripThroughJson() throws Exception {
        ApplicationResourceConfig original = ApplicationResourceConfig.defaults()
                .withResourceRoots(Map.of(
                        ResourceCategory.UI, List.of("classpath:/com/eb/javafx/ui", "app/ui"),
                        ResourceCategory.IMAGES, List.of("app/images")));
        Path output = tempDir.resolve("config-with-roots.json");
        original.save(output);

        ApplicationResourceConfig reloaded = ApplicationResourceConfig.load(output);
        assertEquals(List.of("classpath:/com/eb/javafx/ui", "app/ui"),
                reloaded.resourceRoots(ResourceCategory.UI));
        assertEquals(List.of("app/images"), reloaded.resourceRoots(ResourceCategory.IMAGES));
        String body = Files.readString(output);
        assertTrue(body.contains("\"resourceRoots\""));
    }

    @Test
    void withAdditionalResourceRootAppendsInOrder() {
        ApplicationResourceConfig config = ApplicationResourceConfig.defaults()
                .withResourceRoots(Map.of(ResourceCategory.SUPPORT, List.of("first")))
                .withAdditionalResourceRoot(ResourceCategory.SUPPORT, "second");

        assertEquals(List.of("first", "second"), config.resourceRoots(ResourceCategory.SUPPORT));
    }
}
