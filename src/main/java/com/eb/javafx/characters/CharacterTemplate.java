package com.eb.javafx.characters;

import com.eb.javafx.gamesupport.IdentifiedDefinition;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reusable character definition used to seed per-save {@link CharacterState} instances.
 *
 * <p>{@code baseStats} supplies the initial immutable stat block for new runtime state, while tags and metadata remain
 * authoring/catalog descriptors that can be interpreted by app-specific systems.</p>
 */
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

    public Optional<String> talkingAnimationId() {
        return Optional.ofNullable(metadata.get("talkingAnimationId"));
    }

    public Optional<String> idleAnimationId() {
        return Optional.ofNullable(metadata.get("idleAnimationId"));
    }
}
