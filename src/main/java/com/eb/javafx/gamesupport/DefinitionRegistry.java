package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Generic deterministic registry for reusable static game-support descriptors. */
public final class DefinitionRegistry<T extends IdentifiedDefinition> {
    private final String definitionName;
    private final Map<String, T> definitions = new LinkedHashMap<>();

    public DefinitionRegistry(String definitionName) {
        this.definitionName = Validation.requireNonBlank(definitionName, "Definition name must not be blank.");
    }

    /** Registers a definition and rejects duplicate IDs before it can be exposed to UI or runtime systems. */
    public void register(T definition) {
        T checkedDefinition = Validation.requireNonNull(definition, definitionName + " definition must not be null.");
        String id = Validation.requireNonBlank(checkedDefinition.id(), definitionName + " ID must not be blank.");
        if (definitions.containsKey(id)) {
            throw new IllegalArgumentException(definitionName + " already registered: " + id);
        }
        definitions.put(id, checkedDefinition);
    }

    /** Returns a definition by stable ID, if present. */
    public Optional<T> definition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    /** Returns registered definitions in deterministic registration order. */
    public List<T> definitions() {
        return Collections.unmodifiableList(new ArrayList<>(definitions.values()));
    }

    public boolean contains(String id) {
        return definitions.containsKey(id);
    }

    public boolean isEmpty() {
        return definitions.isEmpty();
    }
}
