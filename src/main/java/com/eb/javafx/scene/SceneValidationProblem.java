package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

/** One content-neutral scene validation diagnostic. */
public record SceneValidationProblem(
        SceneValidationSeverity severity,
        String sceneId,
        String stepId,
        String message) {
    public SceneValidationProblem {
        Validation.requireNonNull(severity, "Scene validation severity is required.");
        Validation.requireNonBlank(sceneId, "Scene validation scene id is required.");
        Validation.requireNonBlank(message, "Scene validation message is required.");
    }

    public static SceneValidationProblem error(String sceneId, String stepId, String message) {
        return new SceneValidationProblem(SceneValidationSeverity.ERROR, sceneId, stepId, message);
    }

    public static SceneValidationProblem warning(String sceneId, String stepId, String message) {
        return new SceneValidationProblem(SceneValidationSeverity.WARNING, sceneId, stepId, message);
    }
}
