package com.eb.javafx.save;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.progress.ProgressSnapshot;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable rollback/save snapshot of reusable mutable gameplay state at a checkpoint boundary. */
public record GameplayStateSnapshot(
        GameDateTime gameTime,
        ProgressSnapshot progress,
        InventorySnapshot inventory,
        WardrobeSnapshot wardrobe,
        CharacterStatesSnapshot characters,
        JournalSnapshot journal,
        LocationOccupancySnapshot locationOccupancy,
        List<SaveSnapshotSection> customSections) {
    public GameplayStateSnapshot(
            GameDateTime gameTime,
            ProgressSnapshot progress,
            InventorySnapshot inventory,
            WardrobeSnapshot wardrobe,
            CharacterStatesSnapshot characters,
            JournalSnapshot journal,
            LocationOccupancySnapshot locationOccupancy) {
        this(gameTime, progress, inventory, wardrobe, characters, journal, locationOccupancy, List.of());
    }

    public GameplayStateSnapshot {
        gameTime = Validation.requireNonNull(gameTime, "Game time is required.");
        progress = Validation.requireNonNull(progress, "Progress snapshot is required.");
        inventory = Validation.requireNonNull(inventory, "Inventory snapshot is required.");
        wardrobe = Validation.requireNonNull(wardrobe, "Wardrobe snapshot is required.");
        characters = Validation.requireNonNull(characters, "Character states snapshot is required.");
        journal = Validation.requireNonNull(journal, "Journal snapshot is required.");
        locationOccupancy = Validation.requireNonNull(locationOccupancy, "Location occupancy snapshot is required.");
        customSections = copyCustomSections(customSections);
    }

    private static List<SaveSnapshotSection> copyCustomSections(List<SaveSnapshotSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return List.of();
        }
        Map<String, SaveSnapshotSection> byId = new LinkedHashMap<>();
        for (SaveSnapshotSection section : sections) {
            SaveSnapshotSection checkedSection =
                    Validation.requireNonNull(section, "Custom gameplay snapshot section is required.");
            if (byId.putIfAbsent(checkedSection.sectionId(), checkedSection) != null) {
                throw new IllegalArgumentException("Duplicate custom gameplay snapshot section: " + checkedSection.sectionId());
            }
        }
        return List.copyOf(byId.values());
    }
}
