package com.eb.javafx.storyline;

/**
 * Resolves named {@link EventRequirement.Custom} requirements from authoring tools or JSON loaders.
 *
 * <p>The storyline JSON format supports {@code { "type": "custom", "name": "some.key" }} entries.
 * The loader delegates resolution to this registry so consumer applications can supply the actual
 * predicates without the engine needing to know about them.</p>
 *
 * <p>The returned requirement may carry any behaviour — the name is purely a lookup key.
 * Implementations should throw {@link IllegalArgumentException} when a name is unknown so
 * the loader surfaces a clear error during authoring rather than at runtime.</p>
 */
@FunctionalInterface
public interface CustomRequirementRegistry {

    /** Resolves {@code name} to an {@link EventRequirement}. Throws if the name is unknown. */
    EventRequirement resolve(String name);

    /** Registry that rejects every name — used when no custom requirements are expected. */
    static CustomRequirementRegistry empty() {
        return name -> {
            throw new IllegalArgumentException("No custom requirement registry provided — cannot resolve: " + name);
        };
    }
}
