package com.eb.javafx.scene;

import com.eb.javafx.save.SaveSnapshotDocument;
import com.eb.javafx.save.SaveSnapshotRegistry;
import com.eb.javafx.save.SaveSnapshotSection;
import com.eb.javafx.util.Validation;

import java.util.List;

/** Helpers for composing reusable scene-flow state into app-owned save snapshot documents. */
public final class SceneFlowSnapshotDocuments {
    private SceneFlowSnapshotDocuments() {
    }

    public static SaveSnapshotRegistry sceneFlowRegistry() {
        SaveSnapshotRegistry registry = new SaveSnapshotRegistry();
        registry.registerRequired(SceneFlowStateJson.SNAPSHOT_SECTION_ID, SceneFlowStateJson.SNAPSHOT_SCHEMA_VERSION);
        return registry;
    }

    public static SaveSnapshotDocument compose(SceneFlowState state, List<SaveSnapshotSection> additionalSections) {
        Validation.requireNonNull(state, "Scene flow state is required.");
        List<SaveSnapshotSection> sections = new java.util.ArrayList<>();
        sections.add(SceneFlowStateJson.toSnapshotSection(state));
        sections.addAll(Validation.requireNonNull(additionalSections, "Additional snapshot sections are required."));
        return sceneFlowRegistry().compose(sections);
    }

    public static SceneFlowState restore(SaveSnapshotDocument document) {
        SaveSnapshotSection section = sceneFlowRegistry().decompose(document).stream()
                .filter(candidate -> SceneFlowStateJson.SNAPSHOT_SECTION_ID.equals(candidate.sectionId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing scene flow snapshot section."));
        return SceneFlowStateJson.fromSnapshotSection(section);
    }
}
