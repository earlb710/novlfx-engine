package com.eb.javafx.util;

import java.util.Collection;
import java.util.Set;

/** Shared helpers for content-neutral import and registry validation. */
public final class GenericValidation {
    private GenericValidation() {
    }

    public static void requireKnownIds(Collection<String> ids, Set<String> knownIds, String label) {
        Validation.requireNonNull(ids, "IDs are required.");
        Validation.requireNonNull(knownIds, "Known IDs are required.");
        String checkedLabel = Validation.requireNonBlank(label, "Validation label is required.");
        for (String id : ids) {
            String checkedId = Validation.requireNonBlank(id, checkedLabel + " id is required.");
            if (!knownIds.contains(checkedId)) {
                throw new IllegalArgumentException("Unknown " + checkedLabel + ": " + checkedId);
            }
        }
    }

    public static ImportValidationIssue missingReference(String path, String label, String id) {
        return new ImportValidationIssue(
                ImportIssueSeverity.ERROR,
                path,
                "Missing " + Validation.requireNonBlank(label, "Validation label is required.") + ": "
                        + Validation.requireNonBlank(id, "Reference id is required."));
    }
}
