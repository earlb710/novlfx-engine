package com.eb.javafx.characters;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Mutable per-save generic character state with stats, relationships, flags, and metadata. */
public final class CharacterState {
    private final String characterId;
    private final String templateId;
    private CharacterStatBlock stats;
    private final Map<String, Integer> relationships = new LinkedHashMap<>();
    private final Set<String> flags = new LinkedHashSet<>();
    private final Map<String, String> metadata = new LinkedHashMap<>();

    public CharacterState(String characterId, String templateId, CharacterStatBlock stats) {
        this.characterId = Validation.requireNonBlank(characterId, "Character id is required.");
        this.templateId = Validation.requireNonBlank(templateId, "Character template id is required.");
        this.stats = stats == null ? CharacterStatBlock.empty() : stats;
    }

    public String characterId() {
        return characterId;
    }

    public String templateId() {
        return templateId;
    }

    public CharacterStatBlock stats() {
        return stats;
    }

    public void setStat(String statId, int value) {
        stats = stats.withValue(statId, value);
    }

    public int incrementStat(String statId, int delta) {
        stats = stats.increment(statId, delta);
        return stats.value(statId);
    }

    public int relationship(String relationshipId) {
        return relationships.getOrDefault(relationshipId, 0);
    }

    public int incrementRelationship(String relationshipId, int delta) {
        String checkedId = Validation.requireNonBlank(relationshipId, "Relationship id is required.");
        int updated = relationship(checkedId) + delta;
        relationships.put(checkedId, updated);
        return updated;
    }

    public void setRelationship(String relationshipId, int value) {
        relationships.put(Validation.requireNonBlank(relationshipId, "Relationship id is required."), value);
    }

    public void addFlag(String flag) {
        flags.add(Validation.requireNonBlank(flag, "Character flag is required."));
    }

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public void putMetadata(String key, String value) {
        metadata.put(
                Validation.requireNonBlank(key, "Character metadata key is required."),
                Validation.requireNonNull(value, "Character metadata value is required."));
    }

    public Map<String, Integer> relationships() {
        return ImmutableCollections.copyMap(relationships);
    }

    public Set<String> flags() {
        return Set.copyOf(flags);
    }

    public Map<String, String> metadata() {
        return ImmutableCollections.copyMap(metadata);
    }
}
