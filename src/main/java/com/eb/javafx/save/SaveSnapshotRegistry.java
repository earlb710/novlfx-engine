package com.eb.javafx.save;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for validating and migrating composed save snapshot sections.
 */
public final class SaveSnapshotRegistry {
    private final Map<String, SectionDefinition> sections = new LinkedHashMap<>();

    public void registerRequired(String sectionId, int schemaVersion) {
        register(sectionId, schemaVersion, true, null);
    }

    public void registerOptional(String sectionId, int schemaVersion) {
        register(sectionId, schemaVersion, false, null);
    }

    public void registerRequired(String sectionId, int schemaVersion, SaveSnapshotSectionMigration migration) {
        register(sectionId, schemaVersion, true, migration);
    }

    public void registerOptional(String sectionId, int schemaVersion, SaveSnapshotSectionMigration migration) {
        register(sectionId, schemaVersion, false, migration);
    }

    public SaveSnapshotDocument compose(List<SaveSnapshotSection> inputSections) {
        Validation.requireNonNull(inputSections, "Save snapshot sections are required.");
        Map<String, SaveSnapshotSection> ordered = new LinkedHashMap<>();
        for (SaveSnapshotSection section : inputSections) {
            if (ordered.putIfAbsent(section.sectionId(), section) != null) {
                throw new IllegalArgumentException("Duplicate save snapshot section: " + section.sectionId());
            }
        }
        for (SectionDefinition definition : sections.values()) {
            if (definition.required && !ordered.containsKey(definition.sectionId)) {
                throw new IllegalArgumentException("Missing required save snapshot section: " + definition.sectionId);
            }
        }
        List<SaveSnapshotSection> resolved = ordered.values().stream()
                .map(this::resolveSection)
                .toList();
        return new SaveSnapshotDocument(resolved);
    }

    public List<SaveSnapshotSection> decompose(SaveSnapshotDocument document) {
        Validation.requireNonNull(document, "Save snapshot document is required.");
        return compose(document.sections()).sections();
    }

    private SaveSnapshotSection resolveSection(SaveSnapshotSection section) {
        SectionDefinition definition = sections.get(section.sectionId());
        if (definition == null) {
            return section;
        }
        if (section.schemaVersion() == definition.schemaVersion) {
            return section;
        }
        if (definition.migration == null) {
            throw new IllegalArgumentException("Unsupported save snapshot section version: "
                    + section.sectionId() + "@" + section.schemaVersion());
        }
        SaveSnapshotSection migrated = definition.migration.migrate(section);
        if (!definition.sectionId.equals(migrated.sectionId()) || migrated.schemaVersion() != definition.schemaVersion) {
            throw new IllegalArgumentException("Save snapshot migration produced unsupported section metadata: "
                    + migrated.sectionId() + "@" + migrated.schemaVersion());
        }
        return migrated;
    }

    private void register(String sectionId, int schemaVersion, boolean required, SaveSnapshotSectionMigration migration) {
        sections.put(
                Validation.requireNonBlank(sectionId, "Save snapshot section id is required."),
                new SectionDefinition(sectionId, Validation.requirePositive(schemaVersion, "Save snapshot section version must be positive."), required, migration));
    }

    private record SectionDefinition(String sectionId, int schemaVersion, boolean required, SaveSnapshotSectionMigration migration) {
    }
}
