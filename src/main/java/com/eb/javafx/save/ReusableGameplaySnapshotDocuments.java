package com.eb.javafx.save;

import com.eb.javafx.gamesupport.TimeSaveSnapshots;
import com.eb.javafx.progress.ProgressSnapshotCodec;
import com.eb.javafx.scene.SceneFlowStateJson;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;

/** Helpers for composing reusable gameplay slices into app-owned save snapshot documents. */
public final class ReusableGameplaySnapshotDocuments {
    private static final ProgressSnapshotCodec PROGRESS_CODEC = new ProgressSnapshotCodec();

    private ReusableGameplaySnapshotDocuments() {
    }

    /**
     * Creates the registry for the first reusable gameplay migration slice.
     *
     * <p>Unknown sections are intentionally preserved so application-owned save state can be stored next to these
     * engine-owned slices.</p>
     */
    public static SaveSnapshotRegistry reusableGameplayRegistry() {
        SaveSnapshotRegistry registry = new SaveSnapshotRegistry();
        registry.registerRequired(SceneFlowStateJson.SNAPSHOT_SECTION_ID, SceneFlowStateJson.SNAPSHOT_SCHEMA_VERSION);
        registry.registerRequired(TimeSaveSnapshots.SNAPSHOT_SECTION_ID, TimeSaveSnapshots.SNAPSHOT_SCHEMA_VERSION);
        registry.registerRequired(PROGRESS_CODEC.sectionId(), PROGRESS_CODEC.schemaVersion());
        return registry;
    }

    /** Composes reusable gameplay state plus optional application-owned sections into one validated document. */
    public static SaveSnapshotDocument compose(
            ReusableGameplaySnapshot snapshot,
            List<SaveSnapshotSection> additionalSections) {
        ReusableGameplaySnapshot checkedSnapshot =
                Validation.requireNonNull(snapshot, "Reusable gameplay snapshot is required.");
        List<SaveSnapshotSection> sections = new ArrayList<>();
        sections.add(SceneFlowStateJson.toSnapshotSection(checkedSnapshot.sceneFlowState()));
        sections.add(TimeSaveSnapshots.toSnapshotSection(checkedSnapshot.gameTime()));
        sections.add(PROGRESS_CODEC.toSection(checkedSnapshot.progress()));
        sections.addAll(Validation.requireNonNull(additionalSections, "Additional snapshot sections are required."));
        return reusableGameplayRegistry().compose(sections);
    }

    /** Restores the reusable gameplay slices from a document while leaving app-owned sections to the application. */
    public static ReusableGameplaySnapshot restore(SaveSnapshotDocument document) {
        List<SaveSnapshotSection> sections = reusableGameplayRegistry().decompose(document);
        return new ReusableGameplaySnapshot(
                SceneFlowStateJson.fromSnapshotSection(requiredSection(sections, SceneFlowStateJson.SNAPSHOT_SECTION_ID)),
                TimeSaveSnapshots.fromSnapshotSection(requiredSection(sections, TimeSaveSnapshots.SNAPSHOT_SECTION_ID)),
                PROGRESS_CODEC.fromSection(requiredSection(sections, PROGRESS_CODEC.sectionId())));
    }

    private static SaveSnapshotSection requiredSection(List<SaveSnapshotSection> sections, String sectionId) {
        return sections.stream()
                .filter(section -> sectionId.equals(section.sectionId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing reusable gameplay snapshot section: " + sectionId));
    }
}
