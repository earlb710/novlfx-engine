package com.eb.javafx.save;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.progress.ProgressSnapshot;
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
        GameDateTime gameTime,
        ProgressSnapshot progress) {
    public ReusableGameplaySnapshot {
        sceneFlowState = Validation.requireNonNull(sceneFlowState, "Scene flow state is required.");
        gameTime = Validation.requireNonNull(gameTime, "Game time is required.");
        progress = Validation.requireNonNull(progress, "Progress snapshot is required.");
    }
}
