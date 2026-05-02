package com.eb.javafx.save;

import com.eb.javafx.util.Validation;

/**
 * Converts one reusable engine state slice to and from an application-composable save snapshot section.
 *
 * <p>Applications can use codecs to place engine-owned JSON payloads inside their own save schemas without asking
 * the engine to define the complete application save document.</p>
 */
public interface SaveSnapshotCodec<T> {
    /** Returns the stable section id used in an application's save document. */
    String sectionId();

    /** Returns the section-local schema version accepted by this codec. */
    int schemaVersion();

    /** Serializes the reusable state slice into this section's JSON payload. */
    String toJson(T snapshot);

    /** Deserializes this section's JSON payload into the reusable state slice. */
    T fromJson(String json, String sourceName);

    /** Wraps a serialized payload with section id and version metadata. */
    default SaveSnapshotSection toSection(T snapshot) {
        return new SaveSnapshotSection(sectionId(), schemaVersion(), toJson(snapshot));
    }

    /**
     * Validates section id/version metadata before deserializing a section payload.
     *
     * @throws IllegalArgumentException when a caller passes a different section or unsupported version
     */
    default T fromSection(SaveSnapshotSection section) {
        Validation.requireNonNull(section, "Save snapshot section is required.");
        if (!sectionId().equals(section.sectionId())) {
            throw new IllegalArgumentException("Unsupported save snapshot section: " + section.sectionId());
        }
        if (schemaVersion() != section.schemaVersion()) {
            throw new IllegalArgumentException("Unsupported save snapshot section version: " + section.schemaVersion());
        }
        return fromJson(section.payloadJson(), section.sectionId());
    }
}
