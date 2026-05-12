package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;
import java.util.Optional;

/**
 * UI-neutral dialogue or narration row ready for reusable scene renderers.
 */
public record SceneDialogueRowViewModel(
        SceneStepType type,
        String speakerId,
        String textDefinition,
        String displayReference,
        Optional<CtcIndicatorDefinition> ctcIndicator) {
    public SceneDialogueRowViewModel {
        Validation.requireNonNull(type, "Scene dialogue row type is required.");
        Validation.requireNonBlank(textDefinition, "Scene dialogue row text definition is required.");
        Validation.requireNonNull(ctcIndicator, "ctcIndicator");
    }
}
