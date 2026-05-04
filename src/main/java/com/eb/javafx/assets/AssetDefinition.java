package com.eb.javafx.assets;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;

/** Reusable catalog entry for an application-owned asset path. */
public record AssetDefinition(String id, AssetType type, String relativePath, boolean preload, List<String> tags) {
    public AssetDefinition {
        id = Validation.requireNonBlank(id, "Asset id is required.");
        type = Validation.requireNonNull(type, "Asset type is required.");
        relativePath = Validation.requireNonBlank(relativePath, "Asset relative path is required.");
        tags = ImmutableCollections.copyList(tags);
    }
}
