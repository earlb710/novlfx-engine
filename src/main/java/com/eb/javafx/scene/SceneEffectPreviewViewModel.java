package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

/**
 * Preview-only effect metadata for scene or choice rendering.
 */
public record SceneEffectPreviewViewModel(String label, String value) {
    public SceneEffectPreviewViewModel {
        Validation.requireNonBlank(label, "Scene effect preview label is required.");
        Validation.requireNonBlank(value, "Scene effect preview value is required.");
    }
}
