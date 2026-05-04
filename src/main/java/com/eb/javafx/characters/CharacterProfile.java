package com.eb.javafx.characters;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/** Generic character profile metadata without authored story behavior. */
public record CharacterProfile(String id, String displayName, List<String> tags, Map<String, String> metadata) {
    public CharacterProfile {
        id = Validation.requireNonBlank(id, "Character id is required.");
        displayName = Validation.requireNonBlank(displayName, "Character display name is required.");
        tags = ImmutableCollections.copyList(tags);
        metadata = ImmutableCollections.copyMap(metadata);
    }
}
