package com.eb.javafx.content;

import com.eb.javafx.gamesupport.IdentifiedDefinition;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/** Descriptor for application-owned content packs loaded through reusable registries. */
public record ContentPackDescriptor(
        String id,
        String title,
        String version,
        List<String> dependencyIds,
        Map<String, String> metadata) implements IdentifiedDefinition {
    public ContentPackDescriptor {
        id = Validation.requireNonBlank(id, "Content pack id is required.");
        title = Validation.requireNonBlank(title, "Content pack title is required.");
        version = Validation.requireNonBlank(version, "Content pack version is required.");
        dependencyIds = ImmutableCollections.copyList(dependencyIds);
        dependencyIds.forEach(dependencyId -> Validation.requireNonBlank(dependencyId, "Content pack dependency id is required."));
        metadata = ImmutableCollections.copyMap(metadata);
    }
}
