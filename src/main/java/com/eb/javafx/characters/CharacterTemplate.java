package com.eb.javafx.characters;

import com.eb.javafx.gamesupport.IdentifiedDefinition;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/** Reusable character template with generic stats and metadata only. */
public record CharacterTemplate(
        String id,
        String displayName,
        String archetypeId,
        CharacterStatBlock baseStats,
        List<String> tags,
        Map<String, String> metadata) implements IdentifiedDefinition {
    public CharacterTemplate {
        id = Validation.requireNonBlank(id, "Character template id is required.");
        displayName = Validation.requireNonBlank(displayName, "Character template display name is required.");
        archetypeId = archetypeId == null ? "" : archetypeId;
        baseStats = baseStats == null ? CharacterStatBlock.empty() : baseStats;
        tags = ImmutableCollections.copyList(tags);
        tags.forEach(tag -> Validation.requireNonBlank(tag, "Character template tag is required."));
        metadata = ImmutableCollections.copyMap(metadata);
    }
}
