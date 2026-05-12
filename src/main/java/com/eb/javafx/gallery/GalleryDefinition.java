package com.eb.javafx.gallery;

import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Objects;

/** Named collection of gallery entries presented together in a gallery screen. */
public final class GalleryDefinition {
    private final String id;
    private final String titleTextKey;
    private final List<GalleryEntry> entries;

    public GalleryDefinition(String id, String titleTextKey, List<GalleryEntry> entries) {
        this.id = Validation.requireNonBlank(id, "Gallery id is required.");
        this.titleTextKey = Validation.requireNonBlank(titleTextKey, "Gallery titleTextKey is required.");
        this.entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public String id() { return id; }
    public String titleTextKey() { return titleTextKey; }
    public List<GalleryEntry> entries() { return entries; }
}
