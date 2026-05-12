package com.eb.javafx.gallery;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class GalleryDefinitionTest {

    @Test
    void constructsWithValidFields() {
        GalleryEntry entry = new GalleryEntry("cg_01", "img/cg_01.png", "gallery.cg_01.caption", "unlock_cg_01");
        GalleryDefinition def = new GalleryDefinition("main-gallery", "gallery.title", List.of(entry));
        assertEquals("main-gallery", def.id());
        assertEquals("gallery.title", def.titleTextKey());
        assertEquals(1, def.entries().size());
    }

    @Test
    void entryExposesFields() {
        GalleryEntry entry = new GalleryEntry("cg_02", "img/cg_02.png", "gallery.cg_02.caption", "unlock_cg_02");
        assertEquals("cg_02", entry.id());
        assertEquals("img/cg_02.png", entry.imageRef());
        assertEquals("gallery.cg_02.caption", entry.captionTextKey());
        assertEquals("unlock_cg_02", entry.requiredUnlockId());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
            () -> new GalleryDefinition("", "title", List.of()));
    }

    @Test
    void entriesListIsImmutable() {
        GalleryDefinition def = new GalleryDefinition("g", "title", List.of());
        assertThrows(UnsupportedOperationException.class, () -> def.entries().add(
            new GalleryEntry("x", "img/x.png", "cap", "unlock_x")));
    }
}
