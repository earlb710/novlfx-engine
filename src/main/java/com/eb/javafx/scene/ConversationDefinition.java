package com.eb.javafx.scene;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/**
 * Data-only authored conversation bundle made from content definitions and scene-flow definitions.
 *
 * <p>This mirrors the LR2Alt JavaFX port storage shape: dialogue text is stored in a content definition map and
 * the playable conversation flow is stored as reusable scene definitions.</p>
 */
public final class ConversationDefinition {
    private final String id;
    private final String titleDefinition;
    private final Map<String, String> definitions;
    private final List<SceneDefinition> scenes;
    private final Map<String, String> metadata;

    public ConversationDefinition(
            String id,
            String titleDefinition,
            Map<String, String> definitions,
            List<SceneDefinition> scenes,
            Map<String, String> metadata) {
        this.id = Validation.requireNonBlank(id, "Conversation id is required.");
        this.titleDefinition = Validation.requireNonBlank(titleDefinition, "Conversation title definition is required.");
        this.definitions = ImmutableCollections.copyMap(definitions);
        this.scenes = List.copyOf(Validation.requireNonEmpty(scenes, "Conversation requires at least one scene."));
        this.metadata = ImmutableCollections.copyMap(metadata);
    }

    public String id() {
        return id;
    }

    public String titleDefinition() {
        return titleDefinition;
    }

    public Map<String, String> definitions() {
        return definitions;
    }

    public List<SceneDefinition> scenes() {
        return scenes;
    }

    public Map<String, String> metadata() {
        return metadata;
    }
}
