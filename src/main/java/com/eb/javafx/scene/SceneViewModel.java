package com.eb.javafx.scene;

import java.util.List;

/**
 * UI-neutral scene presentation state.
 *
 * <p>The model carries resolved executor status plus the current step identifiers, text/display references,
 * choices, and message so UI code can render scene progress without depending on executor objects.</p>
 */
public final class SceneViewModel {
    private final SceneExecutionStatus status;
    private final String sceneId;
    private final String stepId;
    private final String speakerId;
    private final String textDefinition;
    private final String displayReference;
    private final List<SceneChoiceViewModel> choices;
    private final String message;
    private final List<SceneDialogueRowViewModel> dialogueRows;
    private final List<SceneStatusRowViewModel> statusRows;
    private final List<SceneEffectPreviewViewModel> effectPreviews;
    private final List<String> selectedChoiceIds;

    public SceneViewModel(SceneExecutionStatus status, String sceneId, String stepId, String speakerId, String textDefinition, String displayReference, List<SceneChoiceViewModel> choices, String message) {
        this(status, sceneId, stepId, speakerId, textDefinition, displayReference, choices, message, List.of(), List.of(), List.of(), List.of());
    }

    public SceneViewModel(
            SceneExecutionStatus status,
            String sceneId,
            String stepId,
            String speakerId,
            String textDefinition,
            String displayReference,
            List<SceneChoiceViewModel> choices,
            String message,
            List<SceneDialogueRowViewModel> dialogueRows,
            List<SceneStatusRowViewModel> statusRows,
            List<SceneEffectPreviewViewModel> effectPreviews,
            List<String> selectedChoiceIds) {
        this.status = status;
        this.sceneId = sceneId;
        this.stepId = stepId;
        this.speakerId = speakerId;
        this.textDefinition = textDefinition;
        this.displayReference = displayReference;
        this.choices = List.copyOf(choices);
        this.message = message;
        this.dialogueRows = List.copyOf(dialogueRows);
        this.statusRows = List.copyOf(statusRows);
        this.effectPreviews = List.copyOf(effectPreviews);
        this.selectedChoiceIds = List.copyOf(selectedChoiceIds);
    }

    public SceneExecutionStatus status() {
        return status;
    }

    public String sceneId() {
        return sceneId;
    }

    public String stepId() {
        return stepId;
    }

    public String speakerId() {
        return speakerId;
    }

    public String textDefinition() {
        return textDefinition;
    }

    public String displayReference() {
        return displayReference;
    }

    public List<SceneChoiceViewModel> choices() {
        return choices;
    }

    public String message() {
        return message;
    }

    public List<SceneDialogueRowViewModel> dialogueRows() {
        return dialogueRows;
    }

    public List<SceneStatusRowViewModel> statusRows() {
        return statusRows;
    }

    public List<SceneEffectPreviewViewModel> effectPreviews() {
        return effectPreviews;
    }

    public List<String> selectedChoiceIds() {
        return selectedChoiceIds;
    }
}
