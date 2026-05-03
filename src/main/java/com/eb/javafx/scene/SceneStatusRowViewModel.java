package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

/**
 * Reusable status row for scene diagnostics and app-owned HUD adapters.
 */
public record SceneStatusRowViewModel(String label, String value) {
    public SceneStatusRowViewModel {
        Validation.requireNonBlank(label, "Scene status row label is required.");
        Validation.requireNonBlank(value, "Scene status row value is required.");
    }
}
