package com.eb.javafx.util;

import javafx.scene.text.Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FontResourcesTest {
    @Test
    void listsPackagedFontFileNames() {
        assertTrue(FontResources.fontFileNames().contains("Alien.ttf"));
        assertTrue(FontResources.fontFileNames().contains("Library 3 am.otf"));
        assertTrue(FontResources.fontFileNames().contains("supragen.fon"));
        assertThrows(UnsupportedOperationException.class, () -> FontResources.fontFileNames().add("other.ttf"));
    }

    @Test
    void buildsResourcePathsForPackagedFonts() {
        assertEquals("/com/eb/javafx/fonts/Alien.ttf", FontResources.resourcePath("Alien.ttf"));
        assertEquals("/com/eb/javafx/fonts/Library 3 am.otf", FontResources.resourcePath("Library 3 am.otf"));
        assertTrue(FontResources.isPackagedFont("Alien.ttf"));
        assertFalse(FontResources.isPackagedFont("missing.ttf"));

        assertThrows(IllegalArgumentException.class, () -> FontResources.resourcePath(" "));
        assertThrows(IllegalArgumentException.class, () -> FontResources.resourcePath("nested/Alien.ttf"));
        assertThrows(IllegalArgumentException.class, () -> FontResources.resourcePath("missing.ttf"));
    }

    @Test
    void opensPackagedFontResources() throws Exception {
        assertTrue(FontResources.resourceUrl("Alien.ttf").isPresent());
        assertNotNull(FontResources.requireResourceUrl("Alien.ttf"));

        try (InputStream inputStream = FontResources.open("Alien.ttf")) {
            assertTrue(inputStream.readNBytes(4).length > 0);
        }
    }

    @Test
    void loadsJavaFxFontsFromPackagedResources() {
        Font font = FontResources.load("Alien.ttf", 14.0);

        assertNotNull(font);
        assertEquals(14.0, font.getSize());
        assertThrows(IllegalArgumentException.class, () -> FontResources.load("Alien.ttf", 0.0));
        assertThrows(IllegalArgumentException.class, () -> FontResources.load("Alien.ttf", -1.0));
    }

    // ----- Config-driven font modding hooks ---------------------------------------------------

    @Test
    void loadsFontFromArbitraryClasspathResource() {
        // Off-whitelist classpath path, both with and without a leading slash.
        Font withoutSlash = FontResources.loadResource("com/eb/javafx/fonts/Alien.ttf", 18.0);
        assertNotNull(withoutSlash);
        assertEquals(18.0, withoutSlash.getSize());

        Font withSlash = FontResources.loadResource("/com/eb/javafx/fonts/Alien.ttf", 18.0);
        assertNotNull(withSlash);
    }

    @Test
    void loadResourceRejectsMissingOrInvalidArguments() {
        assertThrows(IllegalStateException.class,
                () -> FontResources.loadResource("com/eb/javafx/fonts/NoSuchFont.ttf", 12.0));
        assertThrows(IllegalArgumentException.class, () -> FontResources.loadResource(" ", 12.0));
        assertThrows(IllegalArgumentException.class,
                () -> FontResources.loadResource("com/eb/javafx/fonts/Alien.ttf", 0.0));
    }

    @Test
    void moduleAwareLoadResolvesAgainstSuppliedClassLoader() {
        ClassLoader loader = FontResourcesTest.class.getClassLoader();
        // Leading slash optional; resolved via the supplied loader (ClassLoader uses no leading slash).
        Font withoutSlash = FontResources.loadResource("com/eb/javafx/fonts/Alien.ttf", 18.0, loader);
        assertNotNull(withoutSlash);
        assertEquals(18.0, withoutSlash.getSize());
        assertNotNull(FontResources.loadResource("/com/eb/javafx/fonts/Alien.ttf", 18.0, loader));

        // Null loader falls back to the engine's own class loader (same as the two-arg form).
        assertNotNull(FontResources.loadResource("com/eb/javafx/fonts/Alien.ttf", 18.0, null));

        assertThrows(IllegalStateException.class,
                () -> FontResources.loadResource("com/eb/javafx/fonts/NoSuchFont.ttf", 12.0, loader));
        assertThrows(IllegalArgumentException.class,
                () -> FontResources.loadResource("com/eb/javafx/fonts/Alien.ttf", 0.0, loader));
    }

    @Test
    void loadsFontFromFile(@TempDir Path tempDir) throws Exception {
        Path fontFile = tempDir.resolve("My-Modded-Font.ttf");
        try (InputStream inputStream = FontResources.open("Alien.ttf")) {
            Files.copy(inputStream, fontFile);
        }

        Font font = FontResources.loadFile(fontFile, 20.0);
        assertNotNull(font);
        assertEquals(20.0, font.getSize());
    }

    @Test
    void loadFileRejectsMissingOrInvalidArguments(@TempDir Path tempDir) {
        assertThrows(IllegalStateException.class,
                () -> FontResources.loadFile(tempDir.resolve("absent.ttf"), 12.0));
        assertThrows(IllegalArgumentException.class, () -> FontResources.loadFile(null, 12.0));
    }
}
