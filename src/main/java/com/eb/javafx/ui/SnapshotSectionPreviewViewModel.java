package com.eb.javafx.ui;

import com.eb.javafx.save.SaveSnapshotSection;
import com.eb.javafx.util.Validation;

/**
 * Reusable preview row for a composed save snapshot section.
 */
public record SnapshotSectionPreviewViewModel(String sectionId, int schemaVersion, String payloadSummary) {
    public SnapshotSectionPreviewViewModel {
        Validation.requireNonBlank(sectionId, "Snapshot preview section id is required.");
        Validation.requirePositive(schemaVersion, "Snapshot preview schema version must be positive.");
        Validation.requireNonBlank(payloadSummary, "Snapshot preview payload summary is required.");
    }

    public static SnapshotSectionPreviewViewModel fromSection(SaveSnapshotSection section) {
        Validation.requireNonNull(section, "Save snapshot section is required.");
        String payload = section.payloadJson();
        String summary = payload.length() > 72 ? payload.substring(0, 69) + "..." : payload;
        return new SnapshotSectionPreviewViewModel(section.sectionId(), section.schemaVersion(), summary);
    }
}
