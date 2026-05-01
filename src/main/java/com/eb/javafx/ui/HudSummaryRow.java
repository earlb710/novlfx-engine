package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

/** Reusable key/value row for HUD summary surfaces. */
public record HudSummaryRow(String label, String value) {
    public HudSummaryRow {
        Validation.requireNonBlank(label, "HUD summary row label is required.");
        Validation.requireNonBlank(value, "HUD summary row value is required.");
    }
}
