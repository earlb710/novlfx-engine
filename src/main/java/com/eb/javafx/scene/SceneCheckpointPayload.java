package com.eb.javafx.scene;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.Map;

/** Replayable result captured for a scene checkpoint interaction. */
public final class SceneCheckpointPayload {
    private final SceneCheckpointPayloadKind kind;
    private final String choiceId;
    private final String value;
    private final Map<String, String> metadata;

    public SceneCheckpointPayload(SceneCheckpointPayloadKind kind, String choiceId, String value, Map<String, String> metadata) {
        this.kind = Validation.requireNonNull(kind, "Scene checkpoint payload kind is required.");
        this.choiceId = choiceId;
        this.value = value;
        this.metadata = ImmutableCollections.copyMap(metadata);
    }

    public static SceneCheckpointPayload textContinuation() {
        return new SceneCheckpointPayload(SceneCheckpointPayloadKind.TEXT_CONTINUATION, null, null, Map.of());
    }

    public static SceneCheckpointPayload choiceSelection(String choiceId, String value) {
        return new SceneCheckpointPayload(
                SceneCheckpointPayloadKind.CHOICE_SELECTION,
                Validation.requireNonBlank(choiceId, "Scene checkpoint choice id is required."),
                value,
                Map.of());
    }

    public static SceneCheckpointPayload inputResult(String value, Map<String, String> metadata) {
        return new SceneCheckpointPayload(
                SceneCheckpointPayloadKind.INPUT_RESULT,
                null,
                Validation.requireNonBlank(value, "Scene checkpoint input value is required."),
                metadata);
    }

    public SceneCheckpointPayloadKind kind() {
        return kind;
    }

    public String choiceId() {
        return choiceId;
    }

    public String value() {
        return value;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    boolean sameReplayData(SceneCheckpointPayload other) {
        if (other == null) {
            return false;
        }
        return kind == other.kind
                && java.util.Objects.equals(choiceId, other.choiceId)
                && java.util.Objects.equals(value, other.value)
                && metadata.equals(other.metadata);
    }
}
