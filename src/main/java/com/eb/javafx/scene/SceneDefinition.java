package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionRequirement;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reusable scene definition containing typed command steps and metadata.
 *
 * <p>Definitions validate that each scene has an id and at least one immutable step, while preserving optional
 * entry requirements and metadata for code-registered modules.</p>
 */
public final class SceneDefinition {
    private final String id;
    private final List<ActionRequirement> entryRequirements;
    private final List<SceneStep> steps;
    private final Map<String, String> metadata;

    public SceneDefinition(String id, List<ActionRequirement> entryRequirements, List<SceneStep> steps, Map<String, String> metadata) {
        this.id = Validation.requireNonBlank(id, "Scene id is required.");
        this.entryRequirements = List.copyOf(Objects.requireNonNull(entryRequirements, "entryRequirements"));
        this.steps = List.copyOf(Validation.requireNonEmpty(steps, "Scene definition requires at least one step."));
        this.metadata = ImmutableCollections.copyMap(metadata);
    }

    public static SceneDefinition of(String id, List<SceneStep> steps) {
        return new SceneDefinition(id, List.of(), steps, Map.of());
    }

    public String id() {
        return id;
    }

    public List<ActionRequirement> entryRequirements() {
        return entryRequirements;
    }

    public List<SceneStep> steps() {
        return steps;
    }

    public Map<String, String> metadata() {
        return metadata;
    }
}
