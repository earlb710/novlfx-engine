package com.eb.javafx.save;

/**
 * Migrates a save snapshot section to the registry's current section schema.
 */
@FunctionalInterface
public interface SaveSnapshotSectionMigration {
    SaveSnapshotSection migrate(SaveSnapshotSection section);
}
