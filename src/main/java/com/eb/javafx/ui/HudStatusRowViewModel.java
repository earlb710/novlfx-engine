package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

/** One reusable read-only HUD/status row. */
public record HudStatusRowViewModel(String label, String value, boolean visible) {
    public HudStatusRowViewModel {
        label = Validation.requireNonBlank(label, "HUD status row label is required.");
        value = Validation.requireNonNull(value, "HUD status row value is required.");
    }
}
