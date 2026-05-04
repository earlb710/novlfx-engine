package com.eb.javafx.diagnostics;

import com.eb.javafx.util.Validation;

/** One structured diagnostic row for debug screens or startup reports. */
public record DiagnosticProblem(String source, DiagnosticSeverity severity, String message, String referenceId) {
    public DiagnosticProblem {
        source = Validation.requireNonBlank(source, "Diagnostic source is required.");
        severity = Validation.requireNonNull(severity, "Diagnostic severity is required.");
        message = Validation.requireNonBlank(message, "Diagnostic message is required.");
        referenceId = referenceId == null ? "" : referenceId;
    }
}
