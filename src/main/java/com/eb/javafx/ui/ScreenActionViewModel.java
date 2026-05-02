package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

/**
 * Reusable action contract for route-backed screen controls.
 *
 * <p>Actions carry the button label, destination route id, and enabled state used by {@link ViewModelScreen}
 * to create navigation controls without coupling screens to JavaFX button setup.</p>
 */
public record ScreenActionViewModel(String label, String routeId, boolean enabled) {
    public ScreenActionViewModel {
        Validation.requireNonBlank(label, "Screen action label is required.");
        Validation.requireNonBlank(routeId, "Screen action route id is required.");
    }
}
