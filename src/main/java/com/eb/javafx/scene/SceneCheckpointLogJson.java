package com.eb.javafx.scene;

import com.eb.javafx.save.SaveSnapshotCodec;
import com.eb.javafx.save.SaveSnapshotSection;
import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;

import java.util.List;
import java.util.Map;

/** JSON serialization for checkpoint rollback history save snapshots. */
public final class SceneCheckpointLogJson {
    public static final String SNAPSHOT_SECTION_ID = "sceneCheckpointLog";
    public static final int SNAPSHOT_SCHEMA_VERSION = 1;

    private static final SaveSnapshotCodec<SceneCheckpointLog> SNAPSHOT_CODEC = new SaveSnapshotCodec<>() {
        @Override
        public String sectionId() {
            return SNAPSHOT_SECTION_ID;
        }

        @Override
        public int schemaVersion() {
            return SNAPSHOT_SCHEMA_VERSION;
        }

        @Override
        public String toJson(SceneCheckpointLog snapshot) {
            return SceneCheckpointLogJson.toJson(snapshot);
        }

        @Override
        public SceneCheckpointLog fromJson(String json, String sourceName) {
            return SceneCheckpointLogJson.fromJson(json, sourceName);
        }
    };

    private SceneCheckpointLogJson() {
    }

    public static SaveSnapshotCodec<SceneCheckpointLog> snapshotCodec() {
        return SNAPSHOT_CODEC;
    }

    public static SaveSnapshotSection toSnapshotSection(SceneCheckpointLog log) {
        return SNAPSHOT_CODEC.toSection(log);
    }

    public static SceneCheckpointLog fromSnapshotSection(SaveSnapshotSection section) {
        return SNAPSHOT_CODEC.fromSection(section);
    }

    public static SceneCheckpointLog fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<SceneCheckpoint> checkpoints = JsonData.optionalList(root, "checkpoints", "scene checkpoints").stream()
                .map(entry -> checkpointFromObject(JsonData.requireObject(entry, "scene checkpoints[]")))
                .toList();
        return new SceneCheckpointLog(
                checkpoints,
                JsonData.requiredInt(root, "cursor", "scene checkpoint cursor"),
                JsonData.requiredInt(root, "rollbackBarrierIndex", "scene checkpoint rollback barrier"),
                JsonData.optionalBoolean(root, "rollbackFixed", false, "scene checkpoint fixed rollback flag"));
    }

    public static String toJson(SceneCheckpointLog log) {
        StringBuilder json = new StringBuilder("{\n")
                .append("  \"cursor\": ").append(log.cursor()).append(",\n")
                .append("  \"rollbackBarrierIndex\": ").append(log.rollbackBarrierIndex()).append(",\n")
                .append("  \"rollbackFixed\": ").append(log.rollbackFixed()).append(",\n")
                .append("  \"checkpoints\": [");
        for (int index = 0; index < log.checkpoints().size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            appendCheckpoint(json, log.checkpoints().get(index));
        }
        json.append("]\n}\n");
        return json.toString();
    }

    private static SceneCheckpoint checkpointFromObject(Map<String, Object> object) {
        return new SceneCheckpoint(
                JsonData.requiredInt(object, "sequence", "scene checkpoint sequence"),
                stateFromObject(JsonData.requireObject(object.get("state"), "scene checkpoint state")),
                JsonData.requiredString(object, "sceneId", "scene checkpoint scene id"),
                JsonData.requiredInt(object, "stepIndex", "scene checkpoint step index"),
                JsonData.requiredString(object, "stepId", "scene checkpoint step id"),
                JsonData.enumValue(SceneStepType.class, JsonData.requiredString(object, "stepType", "scene checkpoint step type"), "scene checkpoint step type"),
                JsonData.optionalObject(object, "payload", "scene checkpoint payload").map(SceneCheckpointLogJson::payloadFromObject).orElse(null),
                JsonData.optionalBoolean(object, "rollbackBlocked", false, "scene checkpoint rollback blocked flag"),
                JsonData.optionalBoolean(object, "rollbackFixed", false, "scene checkpoint rollback fixed flag"),
                JsonData.optionalObject(object, "metadata", "scene checkpoint metadata").map(metadata -> JsonData.stringMap(metadata, "scene checkpoint metadata")).orElse(Map.of()));
    }

    private static SceneFlowState stateFromObject(Map<String, Object> object) {
        List<SceneReturnPoint> callStack = JsonData.optionalList(object, "callStack", "scene flow callStack").stream()
                .map(entry -> {
                    Map<String, Object> point = JsonData.requireObject(entry, "scene flow callStack[]");
                    return new SceneReturnPoint(
                            JsonData.requiredString(point, "sceneId", "return scene id"),
                            JsonData.requiredInt(point, "stepIndex", "return step index"));
                })
                .toList();
        return new SceneFlowState(
                JsonData.requiredString(object, "activeSceneId", "active scene id"),
                JsonData.requiredInt(object, "stepIndex", "scene step index"),
                callStack,
                JsonData.stringList(object.getOrDefault("selectedChoiceIds", List.of()), "selected choice ids"),
                JsonData.optionalString(object, "pendingUiInterruption", "pending UI interruption").orElse(null));
    }

    private static SceneCheckpointPayload payloadFromObject(Map<String, Object> object) {
        return new SceneCheckpointPayload(
                JsonData.enumValue(SceneCheckpointPayloadKind.class, JsonData.requiredString(object, "kind", "scene checkpoint payload kind"), "scene checkpoint payload kind"),
                JsonData.optionalString(object, "choiceId", "scene checkpoint choice id").orElse(null),
                JsonData.optionalString(object, "value", "scene checkpoint payload value").orElse(null),
                JsonData.optionalObject(object, "metadata", "scene checkpoint payload metadata").map(metadata -> JsonData.stringMap(metadata, "scene checkpoint payload metadata")).orElse(Map.of()));
    }

    private static void appendCheckpoint(StringBuilder json, SceneCheckpoint checkpoint) {
        json.append("{\"sequence\": ").append(checkpoint.sequence())
                .append(", \"state\": ");
        appendState(json, checkpoint.state());
        json.append(", \"sceneId\": ").append(JsonStrings.quote(checkpoint.sceneId()))
                .append(", \"stepIndex\": ").append(checkpoint.stepIndex())
                .append(", \"stepId\": ").append(JsonStrings.quote(checkpoint.stepId()))
                .append(", \"stepType\": ").append(JsonStrings.quote(checkpoint.stepType().name()))
                .append(", \"payload\": ");
        appendPayload(json, checkpoint.payload());
        json.append(", \"rollbackBlocked\": ").append(checkpoint.rollbackBlocked())
                .append(", \"rollbackFixed\": ").append(checkpoint.rollbackFixed())
                .append(", \"metadata\": ");
        appendStringMap(json, checkpoint.metadata());
        json.append('}');
    }

    private static void appendState(StringBuilder json, SceneFlowState state) {
        json.append("{\"activeSceneId\": ").append(JsonStrings.quote(state.activeSceneId()))
                .append(", \"stepIndex\": ").append(state.stepIndex())
                .append(", \"callStack\": [");
        for (int index = 0; index < state.callStack().size(); index++) {
            SceneReturnPoint point = state.callStack().get(index);
            if (index > 0) {
                json.append(", ");
            }
            json.append("{\"sceneId\": ").append(JsonStrings.quote(point.sceneId()))
                    .append(", \"stepIndex\": ").append(point.stepIndex()).append('}');
        }
        json.append("], \"selectedChoiceIds\": [");
        for (int index = 0; index < state.selectedChoiceIds().size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            json.append(JsonStrings.quote(state.selectedChoiceIds().get(index)));
        }
        json.append("], \"pendingUiInterruption\": ")
                .append(JsonStrings.nullableQuote(state.pendingUiInterruption()))
                .append('}');
    }

    private static void appendPayload(StringBuilder json, SceneCheckpointPayload payload) {
        if (payload == null) {
            json.append("null");
            return;
        }
        json.append("{\"kind\": ").append(JsonStrings.quote(payload.kind().name()))
                .append(", \"choiceId\": ").append(JsonStrings.nullableQuote(payload.choiceId()))
                .append(", \"value\": ").append(JsonStrings.nullableQuote(payload.value()))
                .append(", \"metadata\": ");
        appendStringMap(json, payload.metadata());
        json.append('}');
    }

    private static void appendStringMap(StringBuilder json, Map<String, String> values) {
        json.append('{');
        int index = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (index > 0) {
                json.append(", ");
            }
            json.append(JsonStrings.quote(entry.getKey()))
                    .append(": ")
                    .append(JsonStrings.quote(entry.getValue()));
            index++;
        }
        json.append('}');
    }
}
