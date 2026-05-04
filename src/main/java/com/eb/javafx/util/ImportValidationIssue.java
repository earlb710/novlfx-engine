package com.eb.javafx.util;

/** One reusable import or validation issue tied to an optional path. */
public record ImportValidationIssue(ImportIssueSeverity severity, String path, String message) {
    public ImportValidationIssue {
        severity = Validation.requireNonNull(severity, "Import issue severity is required.");
        path = path == null ? "" : path;
        message = Validation.requireNonBlank(message, "Import issue message is required.");
    }
}
