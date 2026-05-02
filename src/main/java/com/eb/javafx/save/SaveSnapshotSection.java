package com.eb.javafx.save;

import com.eb.javafx.util.Validation;

/**
 * Versioned JSON section that applications can compose into their own save files.
 *
 * <p>The engine owns only reusable section identity, version, and JSON payload metadata. Application repositories
 * remain responsible for choosing the outer save-file schema and for combining engine sections with app-specific
 * state.</p>
 */
public final class SaveSnapshotSection {
    private final String sectionId;
    private final int schemaVersion;
    private final String payloadJson;

    public SaveSnapshotSection(String sectionId, int schemaVersion, String payloadJson) {
        this.sectionId = Validation.requireNonBlank(sectionId, "Save snapshot section id is required.");
        this.schemaVersion = Validation.requirePositive(
                schemaVersion,
                "Save snapshot section schema version must be positive.");
        this.payloadJson = Validation.requireNonBlank(payloadJson, "Save snapshot section JSON payload is required.");
    }

    /** Returns the reusable engine section identifier. */
    public String sectionId() {
        return sectionId;
    }

    /** Returns the section-local schema version, independent of application save-file versions. */
    public int schemaVersion() {
        return schemaVersion;
    }

    /** Returns the JSON payload owned by the section codec. */
    public String payloadJson() {
        return payloadJson;
    }
}
