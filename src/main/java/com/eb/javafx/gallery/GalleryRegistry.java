package com.eb.javafx.gallery;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Startup registry of named gallery definitions. */
public final class GalleryRegistry {
    private final Map<String, GalleryDefinition> galleries = new LinkedHashMap<>();

    public void register(GalleryDefinition definition) {
        Validation.requireNonNull(definition, "definition");
        if (galleries.containsKey(definition.id())) {
            throw new IllegalArgumentException("Duplicate gallery id: " + definition.id());
        }
        galleries.put(definition.id(), definition);
    }

    public Optional<GalleryDefinition> find(String id) {
        return Optional.ofNullable(galleries.get(id));
    }

    public GalleryDefinition require(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown gallery: " + id));
    }
}
