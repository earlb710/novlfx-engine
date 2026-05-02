package com.eb.javafx.scene;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;

import java.util.List;
import java.util.Map;

/**
 * JSON serialization for resumable scene-flow save snapshots.
 *
 * <p>The format persists active scene position, call stack, selected choices, and pending UI interruption
 * fields using the shared small-JSON helpers rather than application-specific save schemas.</p>
 */
public final class SceneFlowStateJson {
    private SceneFlowStateJson() {
    }

    public static SceneFlowState fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<SceneReturnPoint> callStack = JsonData.optionalList(root, "callStack", "scene flow callStack").stream()
                .map(entry -> {
                    Map<String, Object> object = JsonData.requireObject(entry, "scene flow callStack[]");
                    return new SceneReturnPoint(
                            JsonData.requiredString(object, "sceneId", "return scene id"),
                            JsonData.requiredInt(object, "stepIndex", "return step index"));
                })
                .toList();
        return new SceneFlowState(
                JsonData.requiredString(root, "activeSceneId", "active scene id"),
                JsonData.requiredInt(root, "stepIndex", "scene step index"),
                callStack,
                JsonData.stringList(root.getOrDefault("selectedChoiceIds", List.of()), "selected choice ids"),
                JsonData.optionalString(root, "pendingUiInterruption", "pending UI interruption").orElse(null));
    }

    public static String toJson(SceneFlowState state) {
        StringBuilder json = new StringBuilder("{\n")
                .append("  \"activeSceneId\": ").append(JsonStrings.quote(state.activeSceneId())).append(",\n")
                .append("  \"stepIndex\": ").append(state.stepIndex()).append(",\n")
                .append("  \"callStack\": [");
        for (int index = 0; index < state.callStack().size(); index++) {
            SceneReturnPoint point = state.callStack().get(index);
            if (index > 0) {
                json.append(", ");
            }
            json.append("{\"sceneId\": ").append(JsonStrings.quote(point.sceneId()))
                    .append(", \"stepIndex\": ").append(point.stepIndex()).append('}');
        }
        json.append("],\n  \"selectedChoiceIds\": [");
        for (int index = 0; index < state.selectedChoiceIds().size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            json.append(JsonStrings.quote(state.selectedChoiceIds().get(index)));
        }
        json.append("],\n  \"pendingUiInterruption\": ")
                .append(JsonStrings.nullableQuote(state.pendingUiInterruption()))
                .append("\n}\n");
        return json.toString();
    }
}
