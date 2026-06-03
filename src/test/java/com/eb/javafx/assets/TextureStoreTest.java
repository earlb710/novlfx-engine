package com.eb.javafx.assets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextureStoreTest {

    private static byte[] pngBytes(int marker) {
        // Minimal PNG magic header + a payload byte that varies per marker.
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, (byte) marker};
    }

    @Test
    void storesAndReturnsRelativePathUnderCategory(@TempDir Path dir) throws Exception {
        TextureStore store = new TextureStore(dir);
        String rel = store.store(pngBytes(1), "skin");
        assertTrue(rel.startsWith("textures/skin/"), "Path under textures/<category>/.");
        assertTrue(rel.endsWith(".png"), "PNG magic → .png extension.");
        assertTrue(Files.isRegularFile(dir.resolve(rel)), "File written to disk.");
    }

    @Test
    void deduplicatesIdenticalContent(@TempDir Path dir) throws Exception {
        TextureStore store = new TextureStore(dir);
        String a = store.store(pngBytes(7), "skin");
        String b = store.store(pngBytes(7), "skin"); // identical bytes
        assertEquals(a, b, "Same content → same path.");
        assertEquals(1, store.distinctCount(), "Stored only once.");
    }

    @Test
    void differentContentGetsDifferentPaths(@TempDir Path dir) throws Exception {
        TextureStore store = new TextureStore(dir);
        String a = store.store(pngBytes(1), "skin");
        String b = store.store(pngBytes(2), "skin");
        assertNotEquals(a, b);
        assertEquals(2, store.distinctCount());
    }

    @Test
    void detectsJpegExtension(@TempDir Path dir) throws Exception {
        TextureStore store = new TextureStore(dir);
        byte[] jpg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};
        assertTrue(store.store(jpg, "fabric").endsWith(".jpg"));
    }

    @Test
    void rejectsEmptyBytes(@TempDir Path dir) {
        TextureStore store = new TextureStore(dir);
        assertThrows(IllegalArgumentException.class, () -> store.store(new byte[0], "skin"));
    }
}
