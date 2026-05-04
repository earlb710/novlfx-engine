package com.eb.javafx.characters;

import com.eb.javafx.util.Validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Generic numeric relationship values keyed by character id and relationship id. */
public final class RelationshipState {
    private final Map<String, Map<String, Integer>> values = new LinkedHashMap<>();

    public int value(String characterId, String relationshipId) {
        return values.getOrDefault(characterId, Map.of()).getOrDefault(relationshipId, 0);
    }

    public int increment(String characterId, String relationshipId, int delta) {
        String checkedCharacterId = Validation.requireNonBlank(characterId, "Character id is required.");
        String checkedRelationshipId = Validation.requireNonBlank(relationshipId, "Relationship id is required.");
        int updated = value(checkedCharacterId, checkedRelationshipId) + delta;
        values.computeIfAbsent(checkedCharacterId, ignored -> new LinkedHashMap<>()).put(checkedRelationshipId, updated);
        return updated;
    }

    public Map<String, Map<String, Integer>> values() {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        values.forEach((characterId, relationships) -> copy.put(characterId, Collections.unmodifiableMap(relationships)));
        return Collections.unmodifiableMap(copy);
    }
}
