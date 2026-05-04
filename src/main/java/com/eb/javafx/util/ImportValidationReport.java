package com.eb.javafx.util;

import java.util.List;

/** Reusable import result summary with deterministic issue ordering. */
public record ImportValidationReport(String sourceName, int importedCount, List<ImportValidationIssue> issues) {
    public ImportValidationReport {
        sourceName = Validation.requireNonBlank(sourceName, "Import source name is required.");
        Validation.requireZeroOrPositive(importedCount, "Imported count must not be negative.");
        issues = ImmutableCollections.copyList(issues);
    }

    public boolean successful() {
        return issues.stream().noneMatch(issue -> issue.severity() == ImportIssueSeverity.ERROR);
    }
}
