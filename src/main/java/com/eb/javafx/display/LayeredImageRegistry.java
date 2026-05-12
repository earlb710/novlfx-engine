package com.eb.javafx.display;

import com.eb.javafx.util.Validation;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Startup registry of named layered image definitions. */
public final class LayeredImageRegistry {
    private final Map<String, LayeredImageDefinition> definitions = new LinkedHashMap<>();

    public void register(LayeredImageDefinition definition) {
        Validation.requireNonNull(definition, "definition");
        if (definitions.containsKey(definition.id())) {
            throw new IllegalArgumentException("Duplicate layered image id: " + definition.id());
        }
        definitions.put(definition.id(), definition);
    }

    public Optional<LayeredImageDefinition> find(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public LayeredImageDefinition require(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown layered image: " + id));
    }
}
