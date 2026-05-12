package com.eb.javafx.display;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Startup registry of semantic display tag definitions. */
public final class DisplayTagRegistry {
    private final Map<String, DisplayTagDefinition> tags = new LinkedHashMap<>();

    public void register(DisplayTagDefinition definition) {
        Validation.requireNonNull(definition, "definition");
        if (tags.containsKey(definition.tag())) {
            throw new IllegalArgumentException("Duplicate display tag: " + definition.tag());
        }
        tags.put(definition.tag(), definition);
    }

    public Optional<DisplayTagDefinition> find(String tag) {
        return Optional.ofNullable(tags.get(tag));
    }

    public DisplayTagDefinition require(String tag) {
        return find(tag).orElseThrow(() -> new IllegalArgumentException("Unknown display tag: " + tag));
    }
}
