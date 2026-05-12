package com.eb.javafx.transitions;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Startup registry of named visual transition definitions. */
public final class VisualTransitionRegistry {
    private final Map<String, VisualTransitionDefinition> definitions = new LinkedHashMap<>();

    public void register(VisualTransitionDefinition definition) {
        Validation.requireNonNull(definition, "definition");
        if (definitions.containsKey(definition.id())) {
            throw new IllegalArgumentException("Duplicate visual transition id: " + definition.id());
        }
        definitions.put(definition.id(), definition);
    }

    public Optional<VisualTransitionDefinition> find(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public VisualTransitionDefinition require(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown visual transition: " + id));
    }
}
