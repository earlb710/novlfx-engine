package com.eb.javafx.save;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.progress.ProgressSnapshot;
import com.eb.javafx.scene.SceneCheckpointLog;
import com.eb.javafx.scene.SceneFlowState;
import com.eb.javafx.util.Validation;

/**
 * Minimal reusable gameplay state bundle for application-owned save documents.
 *
 * <p>The bundle deliberately contains only engine-owned state slices that are useful to many ports. Application
 * repositories still own authored save fields, domain rules, and the outer save schema.</p>
 */
public record ReusableGameplaySnapshot(
        SceneFlowState sceneFlowState,
        SceneCheckpointLog sceneCheckpointLog,
        GameDateTime gameTime,
        ProgressSnapshot progress,
        InventorySnapshot inventory,
        WardrobeSnapshot wardrobe,
        CharacterStatesSnapshot characters,
        JournalSnapshot journal,
        LocationOccupancySnapshot locationOccupancy) {
    public ReusableGameplaySnapshot(
            SceneFlowState sceneFlowState,
            GameDateTime gameTime,
            ProgressSnapshot progress,
            InventorySnapshot inventory,
            WardrobeSnapshot wardrobe,
            CharacterStatesSnapshot characters,
            JournalSnapshot journal,
            LocationOccupancySnapshot locationOccupancy) {
        this(
                sceneFlowState,
                SceneCheckpointLog.empty(),
                gameTime,
                progress,
                inventory,
                wardrobe,
                characters,
                journal,
                locationOccupancy);
    }

    public ReusableGameplaySnapshot(
            SceneFlowState sceneFlowState,
            GameDateTime gameTime,
            ProgressSnapshot progress) {
        this(
                sceneFlowState,
                SceneCheckpointLog.empty(),
                gameTime,
                progress,
                InventorySnapshot.empty(),
                WardrobeSnapshot.empty(),
                CharacterStatesSnapshot.empty(),
                JournalSnapshot.empty(),
                LocationOccupancySnapshot.empty());
    }

    public ReusableGameplaySnapshot {
        sceneFlowState = Validation.requireNonNull(sceneFlowState, "Scene flow state is required.");
        sceneCheckpointLog = Validation.requireNonNull(sceneCheckpointLog, "Scene checkpoint log is required.");
        gameTime = Validation.requireNonNull(gameTime, "Game time is required.");
        progress = Validation.requireNonNull(progress, "Progress snapshot is required.");
        inventory = Validation.requireNonNull(inventory, "Inventory snapshot is required.");
        wardrobe = Validation.requireNonNull(wardrobe, "Wardrobe snapshot is required.");
        characters = Validation.requireNonNull(characters, "Character states snapshot is required.");
        journal = Validation.requireNonNull(journal, "Journal snapshot is required.");
        locationOccupancy = Validation.requireNonNull(locationOccupancy, "Location occupancy snapshot is required.");
    }
}
