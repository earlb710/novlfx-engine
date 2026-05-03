package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.save.SaveLoadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SummaryViewModelTest {
    @TempDir
    Path saveDirectory;

    @Test
    void preferencesSummaryUsesRowsInsteadOfRawLines() {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        preferencesService.saveUiOpacity(0.42, 0.75);

        PreferencesSummaryViewModel viewModel = PreferencesSummaryScreen.viewModel("Preferences", preferencesService);

        assertEquals("Preferences", viewModel.title());
        assertEquals("Window", viewModel.rows().get(0).label());
        assertEquals("HUD alpha", viewModel.rows().get(1).label());
        assertEquals("0.42", viewModel.rows().get(1).value());
        assertEquals(12, viewModel.rows().size());
        assertEquals("HUD alpha: 0.42", viewModel.screenViewModel().lines().get(1));
        assertEquals("Back to main menu", viewModel.actions().get(0).label());
    }

    @Test
    void saveLoadSummaryUsesSchemaFields() {
        SaveLoadService saveLoadService = new SaveLoadService(saveDirectory);
        saveLoadService.initialize();

        SaveLoadSummaryViewModel viewModel = SaveLoadSummaryScreen.viewModel("Save/Load", saveLoadService);

        assertEquals("Save/Load", viewModel.title());
        assertEquals(SaveLoadService.CURRENT_SCHEMA_VERSION, viewModel.schemaVersion());
        assertEquals(saveDirectory, viewModel.saveDirectory());
        assertEquals("Transient UI state is intentionally excluded from save data.", viewModel.note());
        assertEquals(List.of(
                "Save schema version: 1",
                "Save directory: " + saveDirectory,
                "Transient UI state is intentionally excluded from save data."), viewModel.screenViewModel().lines());
    }

    @Test
    void hudSummaryUsesDedicatedFields() {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        preferencesService.saveUiOpacity(0.33, 0.75);

        HudSummaryViewModel viewModel = HudSummaryScreen.viewModel("HUD", preferencesService);

        assertEquals("HUD", viewModel.title());
        assertEquals("Persistent HUD layer", viewModel.layerDescription());
        assertEquals(0.33, viewModel.opacity());
        assertEquals(List.of("Persistent HUD layer", "HUD opacity: 0.33"), viewModel.screenViewModel().lines());
    }

    @Test
    void summaryModelsValidateRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> new PreferencesSummaryRowViewModel(" ", "value"));
        assertThrows(IllegalArgumentException.class, () -> new SaveLoadSummaryViewModel("Save", 0, saveDirectory, "note", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new HudSummaryViewModel("HUD", "Layer", 1.1, List.of()));
    }
}
