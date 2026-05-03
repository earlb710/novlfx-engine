package com.eb.javafx.util;

import javafx.scene.text.Font;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

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
}
