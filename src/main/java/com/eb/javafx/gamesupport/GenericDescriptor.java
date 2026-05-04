package com.eb.javafx.gamesupport;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/** Reusable static descriptor for content-neutral registries and catalogs. */
public record GenericDescriptor(
        String id,
        String kind,
        String title,
        List<String> tags,
        Map<String, String> metadata) implements IdentifiedDefinition {
    public GenericDescriptor {
        id = Validation.requireNonBlank(id, "Descriptor id is required.");
        kind = Validation.requireNonBlank(kind, "Descriptor kind is required.");
        title = Validation.requireNonBlank(title, "Descriptor title is required.");
        tags = ImmutableCollections.copyList(tags);
        tags.forEach(tag -> Validation.requireNonBlank(tag, "Descriptor tag is required."));
        metadata = ImmutableCollections.copyMap(metadata);
    }
}
