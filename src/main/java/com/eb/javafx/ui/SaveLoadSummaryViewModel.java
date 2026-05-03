package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.nio.file.Path;
import java.util.List;

/**
 * Structured save/load summary route model.
 */
public record SaveLoadSummaryViewModel(
        String title,
        int schemaVersion,
        Path saveDirectory,
        String note,
        List<ScreenActionViewModel> actions) {
    public SaveLoadSummaryViewModel {
        Validation.requireNonBlank(title, "Save/load summary title is required.");
        Validation.requirePositive(schemaVersion, "Save/load schema version must be positive.");
        Validation.requireNonNull(saveDirectory, "Save/load directory is required.");
        Validation.requireNonBlank(note, "Save/load summary note is required.");
        actions = List.copyOf(Validation.requireNonNull(actions, "Save/load summary actions are required."));
    }

    public ScreenViewModel screenViewModel() {
        return new ScreenViewModel(
                title,
                List.of(
                        "Save schema version: " + schemaVersion,
                        "Save directory: " + saveDirectory,
                        note),
                actions);
    }
}
