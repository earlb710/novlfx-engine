package com.eb.javafx.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScaledStylesheetTest {

    @Test
    void nullSourceReturnsNull() {
        assertNull(ScaledStylesheet.scaledUri(null, 2.0));
    }

    @Test
    void unitScaleReturnsSourceUnchanged(@TempDir Path dir) throws IOException {
        Path css = dir.resolve("a.css");
        Files.writeString(css, ".x { -fx-font-size: 10px; }");
        String src = css.toUri().toString();
        assertEquals(src, ScaledStylesheet.scaledUri(src, 1.0));
    }

    @Test
    void scalingRewritesFontSizesIntoACachedTempFile(@TempDir Path dir) throws IOException {
        Path css = dir.resolve("b.css");
        Files.writeString(css, ".x { -fx-font-size: 10px; }");
        String src = css.toUri().toString();

        String scaled = ScaledStylesheet.scaledUri(src, 2.0);
        assertNotEquals(src, scaled);
        assertTrue(scaled.startsWith("file:"));

        Path scaledFile = Path.of(java.net.URI.create(scaled));
        assertTrue(Files.readString(scaledFile).contains("20"));

        // Same (source, scale) is cached — returns the identical URI.
        assertEquals(scaled, ScaledStylesheet.scaledUri(src, 2.0));
    }
}
