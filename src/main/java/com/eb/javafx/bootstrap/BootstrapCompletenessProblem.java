package com.eb.javafx.bootstrap;

import com.eb.javafx.util.Validation;

/** One reusable bootstrap completeness diagnostic. */
public record BootstrapCompletenessProblem(String category, String id, String message) {
    public BootstrapCompletenessProblem {
        Validation.requireNonBlank(category, "Bootstrap completeness category is required.");
        Validation.requireNonBlank(id, "Bootstrap completeness id is required.");
        Validation.requireNonBlank(message, "Bootstrap completeness message is required.");
    }
}
