package com.eb.javafx.gallery;

import com.eb.javafx.progress.ProgressTracker;

import java.util.List;
import java.util.Objects;

/** Queries a GalleryRegistry and ProgressTracker to produce GalleryEntryViewModels. */
public final class GalleryService {
    private final GalleryRegistry registry;
    private final ProgressTracker progress;

    public GalleryService(GalleryRegistry registry, ProgressTracker progress) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.progress = Objects.requireNonNull(progress, "progress");
    }

    public List<GalleryEntryViewModel> viewModels(String galleryId) {
        GalleryDefinition def = registry.require(galleryId);
        return def.entries().stream()
            .map(entry -> {
                boolean unlocked = progress.isUnlocked(entry.requiredUnlockId());
                return new GalleryEntryViewModel(entry.id(), entry.captionTextKey(), unlocked, entry.imageRef());
            })
            .toList();
    }
}
