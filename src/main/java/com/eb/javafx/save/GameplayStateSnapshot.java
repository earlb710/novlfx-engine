package com.eb.javafx.save;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.progress.ProgressSnapshot;
import com.eb.javafx.util.Validation;

/** Immutable rollback/save snapshot of reusable mutable gameplay state at a checkpoint boundary. */
public record GameplayStateSnapshot(
        GameDateTime gameTime,
        ProgressSnapshot progress,
        InventorySnapshot inventory,
        WardrobeSnapshot wardrobe,
        CharacterStatesSnapshot characters,
        JournalSnapshot journal,
        LocationOccupancySnapshot locationOccupancy) {
    public GameplayStateSnapshot {
        gameTime = Validation.requireNonNull(gameTime, "Game time is required.");
        progress = Validation.requireNonNull(progress, "Progress snapshot is required.");
        inventory = Validation.requireNonNull(inventory, "Inventory snapshot is required.");
        wardrobe = Validation.requireNonNull(wardrobe, "Wardrobe snapshot is required.");
        characters = Validation.requireNonNull(characters, "Character states snapshot is required.");
        journal = Validation.requireNonNull(journal, "Journal snapshot is required.");
        locationOccupancy = Validation.requireNonNull(locationOccupancy, "Location occupancy snapshot is required.");
    }
}
