package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

/**
 * One labeled preference value for the reusable preferences summary screen.
 */
public record PreferencesSummaryRowViewModel(String label, String value) {
    public PreferencesSummaryRowViewModel {
        Validation.requireNonBlank(label, "Preference summary row label is required.");
        Validation.requireNonBlank(value, "Preference summary row value is required.");
    }

    public String line() {
        return label + ": " + value;
    }
}
