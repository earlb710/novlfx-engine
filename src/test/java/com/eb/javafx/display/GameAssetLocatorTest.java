package com.eb.javafx.display;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GameAssetLocatorTest {
    @TempDir
    Path tempDir;

    @Test
    void locatorResolvesAssetsFromConfiguredRoot() throws Exception {
        Path repoRoot = tempDir.resolve("app");
        Path customImages = repoRoot.resolve("assets/images");
        Files.createDirectories(customImages.resolve("bg"));
        Path image = Files.writeString(customImages.resolve("bg/title.png"), "png");
        GameAssetLocator locator = new GameAssetLocator(repoRoot, customImages);

        Path resolved = locator.resolve("bg/title.png").orElseThrow();

        assertEquals(image.normalize(), resolved);
    }

    @Test
    void registryCanUseConfiguredImageRoot() throws Exception {
        Path repoRoot = tempDir.resolve("app");
        Path customImages = repoRoot.resolve("assets/images");
        Files.createDirectories(customImages.resolve("characters"));
        Files.writeString(customImages.resolve("characters/hero.png"), "png");
        ImageDisplayRegistry registry = new ImageDisplayRegistry(repoRoot, customImages);
        registry.registerImage(new ImageAssetDefinition("hero", "characters/hero.png", null, DisplayLayer.BACKGROUND));

        assertTrue(registry.resolveAssetPath("hero").isPresent());
    }
}
