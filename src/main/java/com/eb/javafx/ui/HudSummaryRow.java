package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

/**
 * Reusable key/value row for HUD summary surfaces.
 *
 * <p>Rows are intentionally presentation-only: callers provide already-formatted labels and values, and
 * reusable screens can list them without knowing the source game-state schema.</p>
 */
public record HudSummaryRow(String label, String value) {
    public HudSummaryRow {
        Validation.requireNonBlank(label, "HUD summary row label is required.");
        Validation.requireNonBlank(value, "HUD summary row value is required.");
    }
}
