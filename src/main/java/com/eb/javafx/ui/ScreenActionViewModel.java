package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

/** Reusable action contract for route-backed screen controls. */
public record ScreenActionViewModel(String label, String routeId, boolean enabled) {
    public ScreenActionViewModel {
        Validation.requireNonBlank(label, "Screen action label is required.");
        Validation.requireNonBlank(routeId, "Screen action route id is required.");
    }
}
