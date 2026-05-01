package com.eb.javafx.content;

import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Holds static definitions loaded during application startup.
 *
 * <p>Source-game define variables and module-level lists can create many static
 * game definitions as files are imported. This registry is the Java equivalent: it
 * gives those definitions an explicit home so migrated content can be loaded before
 * mutable save data is created.</p>
 */
public final class ContentRegistry {
    private final Map<String, String> definitions = new LinkedHashMap<>();
    private final Set<String> requiredDefinitionIds = new LinkedHashSet<>();

    /**
     * Registers engine-level required content keys that must be supplied before runtime state.
     *
     * <p>Application or reusable demo modules provide the values for these IDs via
     * {@link #registerDefinition(String, String)} before validation runs.</p>
     */
    public void registerBaseContent() {
        registerRequiredDefinition("application.name");
        registerRequiredDefinition("startup.route");
    }

    /** Registers or replaces a static definition value. */
    public void registerDefinition(String id, String value) {
        definitions.put(validateId(id), Objects.requireNonNull(value, "Definition value must not be null."));
    }

    /** Registers every supplied static definition value in iteration order. */
    public void registerDefinitions(Map<String, String> definitions) {
        Objects.requireNonNull(definitions, "Definitions must not be null.")
                .forEach(this::registerDefinition);
    }

    /** Marks a definition ID as required during startup validation. */
    public void registerRequiredDefinition(String id) {
        requiredDefinitionIds.add(validateId(id));
    }

    /** Marks each supplied definition ID as required during startup validation. */
    public void registerRequiredDefinitions(Collection<String> ids) {
        Objects.requireNonNull(ids, "Required definition IDs must not be null.")
                .forEach(this::registerRequiredDefinition);
    }

    /**
     * Validates content that must exist before game state or UI routes can use it.
     */
    public void validateRules() {
        requiredDefinitionIds.forEach(this::requireDefinition);
    }

    /**
     * Returns an immutable view so controllers cannot mutate static startup data.
     *
     * <p>Definitions remain owned by the registry; later content modules should
     * register through explicit methods rather than modifying the returned map.</p>
     */
    public Map<String, String> definitions() {
        return Collections.unmodifiableMap(definitions);
    }

    /** Returns immutable required definition IDs in deterministic validation order. */
    public Set<String> requiredDefinitionIds() {
        return Collections.unmodifiableSet(requiredDefinitionIds);
    }

    /**
     * Retrieves a single definition by ID for UI labels or future service wiring.
     *
     * @throws IllegalStateException when the requested definition is missing
     */
    public String definition(String id) {
        requireDefinition(id);
        return definitions.get(id);
    }

    private void requireDefinition(String id) {
        if (!definitions.containsKey(id)) {
            throw new IllegalStateException("Missing required content definition: " + id);
        }
    }

    private String validateId(String id) {
        Objects.requireNonNull(id, "Definition ID must not be null.");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Definition ID must not be blank.");
        }
        return id;
    }
}
