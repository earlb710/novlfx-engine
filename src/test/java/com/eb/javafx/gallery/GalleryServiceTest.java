package com.eb.javafx.gallery;

import com.eb.javafx.progress.ProgressTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class GalleryServiceTest {
    private GalleryRegistry registry;
    private ProgressTracker progress;
    private GalleryService service;

    @BeforeEach
    void setUp() {
        registry = new GalleryRegistry();
        registry.register(new GalleryDefinition("main", "gallery.title", List.of(
            new GalleryEntry("cg_01", "img/cg_01.png", "gallery.cg_01", "unlock_cg_01"),
            new GalleryEntry("cg_02", "img/cg_02.png", "gallery.cg_02", "unlock_cg_02")
        )));
        progress = new ProgressTracker();
        service = new GalleryService(registry, progress);
    }

    @Test
    void lockedEntriesReturnUnlockedFalseAndNoImageRef() {
        List<GalleryEntryViewModel> views = service.viewModels("main");
        assertEquals(2, views.size());
        GalleryEntryViewModel first = views.get(0);
        assertFalse(first.unlocked());
        assertTrue(first.imageRef().isEmpty());
    }

    @Test
    void unlockedEntryExposesImageRef() {
        progress.unlock("unlock_cg_01");
        List<GalleryEntryViewModel> views = service.viewModels("main");
        GalleryEntryViewModel first = views.get(0);
        assertTrue(first.unlocked());
        assertEquals("img/cg_01.png", first.imageRef().get());
    }

    @Test
    void mixedLockedAndUnlocked() {
        progress.unlock("unlock_cg_02");
        List<GalleryEntryViewModel> views = service.viewModels("main");
        assertFalse(views.get(0).unlocked());
        assertTrue(views.get(1).unlocked());
    }

    @Test
    void throwsForUnknownGallery() {
        assertThrows(IllegalArgumentException.class, () -> service.viewModels("nonexistent"));
    }
}
