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
                .withCategoryCodeTablesPath("data/categories/en.json")
                .withImageAssetRoot("assets/images")
                .putResource("backgrounds", "assets/backgrounds")
                .putResource("portraits", "assets/portraits");
        Path output = tempDir.resolve("config.json");

        original.save(output);
        ApplicationResourceConfig reloaded = ApplicationResourceConfig.load(output);

        assertEquals("data/categories/en.json", reloaded.categoryCodeTablesPath());
        assertEquals("assets/images", reloaded.imageAssetRoot());
        assertEquals(
                tempDir.resolve("data/categories/en.json").normalize(),
                reloaded.resolveCategoryCodeTables(tempDir));
        assertEquals(
                tempDir.resolve("assets/images").normalize(),
                reloaded.resolveImageAssetRoot(tempDir));
        assertEquals(
                tempDir.resolve("assets/backgrounds").normalize(),
                reloaded.resolveResource(tempDir, "backgrounds").orElseThrow());
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
        assertTrue(config.resourcePath("backgrounds").isPresent());
        assertFalse(config.removeResource("backgrounds").resourcePath("backgrounds").isPresent());
        assertThrows(IllegalArgumentException.class, () -> config.removeResource("missing"));
    }

    @Test
    void factoryRejectsBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.of(" ", "game", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.of("config.json", " ", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> ApplicationResourceConfig.of(
                "config.json",
                "game",
                Map.of("images", " ")));
    }
}
