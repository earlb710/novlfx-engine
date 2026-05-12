package com.eb.javafx.gallery;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

final class GalleryRegistryTest {

    @Test
    void registersAndFindsGallery() {
        GalleryRegistry registry = new GalleryRegistry();
        registry.register(new GalleryDefinition("main", "gallery.title", List.of(
            new GalleryEntry("cg_01", "img/cg_01.png", "gallery.cg_01", "unlock_cg_01")
        )));
        Optional<GalleryDefinition> found = registry.find("main");
        assertTrue(found.isPresent());
        assertEquals(1, found.get().entries().size());
    }

    @Test
    void returnsEmptyForUnknownGallery() {
        GalleryRegistry registry = new GalleryRegistry();
        assertTrue(registry.find("unknown").isEmpty());
    }

    @Test
    void rejectsDuplicateGalleryId() {
        GalleryRegistry registry = new GalleryRegistry();
        registry.register(new GalleryDefinition("g", "title", List.of()));
        assertThrows(IllegalArgumentException.class,
            () -> registry.register(new GalleryDefinition("g", "title2", List.of())));
    }
}
