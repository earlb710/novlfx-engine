package com.eb.javafx.resources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceRegistryTest {

    @Test
    void filesystemRootIsIndexedRecursively(@TempDir Path tmp) throws IOException {
        Path uiRoot = Files.createDirectory(tmp.resolve("ui"));
        Files.writeString(uiRoot.resolve("main.json"), "{}");
        Path screens = Files.createDirectory(uiRoot.resolve("screens"));
        Files.writeString(screens.resolve("hud.json"), "{}");

        ResourceRegistry registry = ResourceRegistry.builder()
                .addFilesystemRoot(ResourceCategory.UI, uiRoot)
                .build();

        assertEquals(List.of("main.json", "screens/hud.json"), registry.names(ResourceCategory.UI));
        assertTrue(registry.find(ResourceCategory.UI, "screens/hud.json").isPresent());
    }

    @Test
    void appRootWinsWhenLibraryRootHasSameRelativeName(@TempDir Path tmp) throws IOException {
        Path libRoot = Files.createDirectory(tmp.resolve("lib"));
        Files.writeString(libRoot.resolve("conflict.json"), "library");
        Path appRoot = Files.createDirectory(tmp.resolve("app"));
        Files.writeString(appRoot.resolve("conflict.json"), "app");

        ResourceRegistry registry = ResourceRegistry.builder()
                .addFilesystemRoot(ResourceCategory.UI, appRoot)
                .addFilesystemRoot(ResourceCategory.UI, libRoot)
                .build();

        URL resolved = registry.require(ResourceCategory.UI, "conflict.json");
        String body;
        try (InputStream stream = resolved.openStream()) {
            body = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals("app", body, "App roots must take precedence over library roots.");
    }

    @Test
    void categoriesAreStrictlyIsolated(@TempDir Path tmp) throws IOException {
        Path uiRoot = Files.createDirectory(tmp.resolve("ui"));
        Files.writeString(uiRoot.resolve("only-in-ui.json"), "{}");
        Path imagesRoot = Files.createDirectory(tmp.resolve("images"));
        Files.writeString(imagesRoot.resolve("only-in-images.png"), "img");

        ResourceRegistry registry = ResourceRegistry.builder()
                .addFilesystemRoot(ResourceCategory.UI, uiRoot)
                .addFilesystemRoot(ResourceCategory.IMAGES, imagesRoot)
                .build();

        assertTrue(registry.find(ResourceCategory.UI, "only-in-ui.json").isPresent());
        assertFalse(registry.find(ResourceCategory.IMAGES, "only-in-ui.json").isPresent(),
                "UI files must not appear under IMAGES lookups.");
        assertTrue(registry.find(ResourceCategory.IMAGES, "only-in-images.png").isPresent());
        assertFalse(registry.find(ResourceCategory.UI, "only-in-images.png").isPresent(),
                "Image files must not appear under UI lookups.");
    }

    @Test
    void missingFilesystemRootIsSilentlySkipped(@TempDir Path tmp) {
        Path missing = tmp.resolve("does-not-exist");

        ResourceRegistry registry = ResourceRegistry.builder()
                .addFilesystemRoot(ResourceCategory.UI, missing)
                .build();

        assertTrue(registry.names(ResourceCategory.UI).isEmpty());
        assertFalse(registry.hasAny(ResourceCategory.UI));
    }

    @Test
    void classpathRootResolvesEngineBundledFonts() {
        ResourceRegistry registry = ResourceRegistry.builder()
                .addClasspathRoot(ResourceCategory.FONTS, "/com/eb/javafx/fonts", getClass().getClassLoader())
                .build();

        // The engine bundles at least one font in the fonts directory.
        assertTrue(registry.hasAny(ResourceCategory.FONTS),
                "Classpath walk should find at least one engine-bundled font.");
        Optional<URL> avara = registry.find(ResourceCategory.FONTS, "Avara.ttf");
        assertTrue(avara.isPresent(), "Avara.ttf is a known engine-bundled font.");
    }

    @Test
    void addRootDispatchesByPrefix(@TempDir Path tmp) throws IOException {
        Path appBase = Files.createDirectory(tmp.resolve("app-base"));
        Path supportDir = Files.createDirectory(appBase.resolve("support"));
        Files.writeString(supportDir.resolve("notes.txt"), "n");

        ResourceRegistry registry = ResourceRegistry.builder()
                .addRoot(ResourceCategory.SUPPORT, "support", appBase, getClass().getClassLoader())
                .addRoot(ResourceCategory.FONTS, "classpath:/com/eb/javafx/fonts", appBase,
                        getClass().getClassLoader())
                .build();

        assertTrue(registry.find(ResourceCategory.SUPPORT, "notes.txt").isPresent());
        assertTrue(registry.hasAny(ResourceCategory.FONTS));
    }

    @Test
    void lookupKeysNormalizeSeparators(@TempDir Path tmp) throws IOException {
        Path uiRoot = Files.createDirectory(tmp.resolve("ui"));
        Path nested = Files.createDirectory(uiRoot.resolve("screens"));
        Files.writeString(nested.resolve("main.json"), "{}");

        ResourceRegistry registry = ResourceRegistry.builder()
                .addFilesystemRoot(ResourceCategory.UI, uiRoot)
                .build();

        // Either separator style and leading-slash form must resolve to the same entry.
        assertTrue(registry.find(ResourceCategory.UI, "screens/main.json").isPresent());
        assertTrue(registry.find(ResourceCategory.UI, "screens\\main.json").isPresent());
        assertTrue(registry.find(ResourceCategory.UI, "/screens/main.json").isPresent());
    }

    @Test
    void emptyRegistryReportsEmptyForAllCategories() {
        ResourceRegistry registry = ResourceRegistry.builder().build();
        for (ResourceCategory category : ResourceCategory.values()) {
            assertFalse(registry.hasAny(category), category.configKey() + " should be empty.");
            assertTrue(registry.names(category).isEmpty());
            assertTrue(registry.list(category).isEmpty());
        }
    }

    @Test
    void resolvedClasspathUrlCanBeRead() throws IOException {
        ResourceRegistry registry = ResourceRegistry.builder()
                .addClasspathRoot(ResourceCategory.UI, "/com/eb/javafx/ui", getClass().getClassLoader())
                .build();
        URL defaultCss = registry.require(ResourceCategory.UI, "default.css");
        try (InputStream stream = defaultCss.openStream()) {
            byte[] bytes = stream.readAllBytes();
            assertTrue(bytes.length > 0, "Engine default.css should be readable through the registry.");
        }
    }
}
