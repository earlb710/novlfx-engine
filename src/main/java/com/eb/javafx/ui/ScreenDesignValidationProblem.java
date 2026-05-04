package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

/** One validation finding for a screen design. */
public record ScreenDesignValidationProblem(ScreenDesignValidationSeverity severity, String path, String message) {
    public ScreenDesignValidationProblem {
        severity = Validation.requireNonNull(severity, "Screen design validation severity is required.");
        path = Validation.requireNonBlank(path, "Screen design validation path is required.");
        message = Validation.requireNonBlank(message, "Screen design validation message is required.");
    }
}
