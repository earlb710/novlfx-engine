package com.eb.javafx.achievements;

import com.eb.javafx.util.Validation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registry of all achievement definitions for the current game session. */
public final class AchievementRegistry {
    private final Map<String, AchievementDefinition> definitions = new LinkedHashMap<>();

    public void register(AchievementDefinition definition) {
        Validation.requireNonNull(definition, "definition");
        if (definitions.containsKey(definition.id()))
            throw new IllegalArgumentException("Duplicate achievement id: " + definition.id());
        definitions.put(definition.id(), definition);
    }

    public Collection<AchievementDefinition> all() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public boolean contains(String id) {
        return definitions.containsKey(id);
    }
}
