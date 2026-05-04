package com.eb.javafx.characters;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable generic numeric stat container keyed by stable stat id. */
public record CharacterStatBlock(Map<String, Integer> values) {
    public CharacterStatBlock {
        values = ImmutableCollections.copyMap(values);
        values.keySet().forEach(id -> Validation.requireNonBlank(id, "Character stat id is required."));
    }

    public static CharacterStatBlock empty() {
        return new CharacterStatBlock(Map.of());
    }

    public int value(String statId) {
        return values.getOrDefault(Validation.requireNonBlank(statId, "Character stat id is required."), 0);
    }

    public CharacterStatBlock withValue(String statId, int value) {
        Map<String, Integer> updated = new LinkedHashMap<>(values);
        updated.put(Validation.requireNonBlank(statId, "Character stat id is required."), value);
        return new CharacterStatBlock(updated);
    }

    public CharacterStatBlock increment(String statId, int delta) {
        return withValue(statId, value(statId) + delta);
    }
}
