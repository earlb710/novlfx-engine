package com.eb.javafx.save;

import com.eb.javafx.characters.CharacterState;
import com.eb.javafx.characters.CharacterStatBlock;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Immutable save snapshot of one reusable character state. */
public record CharacterSnapshot(
        String characterId,
        String templateId,
        Map<String, Integer> stats,
        Map<String, Integer> relationships,
        Set<String> flags,
        Map<String, String> metadata) {
    public CharacterSnapshot {
        characterId = Validation.requireNonBlank(characterId, "Character id is required.");
        templateId = Validation.requireNonBlank(templateId, "Character template id is required.");
        stats = ImmutableCollections.copyMap(stats);
        relationships = ImmutableCollections.copyMap(relationships);
        flags = flags == null || flags.isEmpty() ? Set.of() : Set.copyOf(new LinkedHashSet<>(flags));
        metadata = ImmutableCollections.copyMap(metadata);
        stats.keySet().forEach(id -> Validation.requireNonBlank(id, "Character stat id is required."));
        relationships.keySet().forEach(id -> Validation.requireNonBlank(id, "Relationship id is required."));
        flags.forEach(flag -> Validation.requireNonBlank(flag, "Character flag is required."));
        metadata.keySet().forEach(key -> Validation.requireNonBlank(key, "Character metadata key is required."));
    }

    public static CharacterSnapshot fromState(CharacterState state) {
        CharacterState checkedState = Validation.requireNonNull(state, "Character state is required.");
        return new CharacterSnapshot(
                checkedState.characterId(),
                checkedState.templateId(),
                checkedState.stats().values(),
                checkedState.relationships(),
                checkedState.flags(),
                checkedState.metadata());
    }

    public CharacterState toState() {
        CharacterState state = new CharacterState(characterId, templateId, new CharacterStatBlock(stats));
        relationships.forEach(state::setRelationship);
        flags.forEach(state::addFlag);
        metadata.forEach(state::putMetadata);
        return state;
    }
}
