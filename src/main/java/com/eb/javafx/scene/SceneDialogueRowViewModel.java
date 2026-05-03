package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

/**
 * UI-neutral dialogue or narration row ready for reusable scene renderers.
 */
public record SceneDialogueRowViewModel(
        SceneStepType type,
        String speakerId,
        String textDefinition,
        String displayReference) {
    public SceneDialogueRowViewModel {
        Validation.requireNonNull(type, "Scene dialogue row type is required.");
        Validation.requireNonBlank(textDefinition, "Scene dialogue row text definition is required.");
    }
}
