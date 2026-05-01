package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.List;

/** Serializable snapshot of a resumable scene-flow position. */
public final class SceneFlowState {
    private final String activeSceneId;
    private final int stepIndex;
    private final List<SceneReturnPoint> callStack;
    private final List<String> selectedChoiceIds;
    private final String pendingUiInterruption;

    public SceneFlowState(String activeSceneId, int stepIndex, List<SceneReturnPoint> callStack, List<String> selectedChoiceIds, String pendingUiInterruption) {
        this.activeSceneId = Validation.requireNonBlank(activeSceneId, "Active scene id is required.");
        if (stepIndex < 0) {
            throw new IllegalArgumentException("Scene step index must not be negative.");
        }
        this.stepIndex = stepIndex;
        this.callStack = List.copyOf(callStack);
        this.selectedChoiceIds = List.copyOf(selectedChoiceIds);
        this.pendingUiInterruption = pendingUiInterruption;
    }

    public static SceneFlowState start(String sceneId) {
        return new SceneFlowState(sceneId, 0, List.of(), List.of(), null);
    }

    public String activeSceneId() {
        return activeSceneId;
    }

    public int stepIndex() {
        return stepIndex;
    }

    public List<SceneReturnPoint> callStack() {
        return callStack;
    }

    public List<String> selectedChoiceIds() {
        return selectedChoiceIds;
    }

    public String pendingUiInterruption() {
        return pendingUiInterruption;
    }
}
