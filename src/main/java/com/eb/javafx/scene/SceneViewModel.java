package com.eb.javafx.scene;

import java.util.List;

/** UI-neutral scene presentation state. */
public final class SceneViewModel {
    private final SceneExecutionStatus status;
    private final String sceneId;
    private final String stepId;
    private final String speakerId;
    private final String textDefinition;
    private final String displayReference;
    private final List<SceneChoiceViewModel> choices;
    private final String message;

    public SceneViewModel(SceneExecutionStatus status, String sceneId, String stepId, String speakerId, String textDefinition, String displayReference, List<SceneChoiceViewModel> choices, String message) {
        this.status = status;
        this.sceneId = sceneId;
        this.stepId = stepId;
        this.speakerId = speakerId;
        this.textDefinition = textDefinition;
        this.displayReference = displayReference;
        this.choices = List.copyOf(choices);
        this.message = message;
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
}
