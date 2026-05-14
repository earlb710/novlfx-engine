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
        preferencesService.saveMasterVolume(0.42);
        preferencesService.saveAudioChannelVolumes(0.25, 0.75);
        preferencesService.saveThemePreferences(PreferencesService.ThemeFamily.SUNSET, PreferencesService.ThemeVariant.LIGHT_PASTEL);
        preferencesService.saveFooterShortcutDisplay(PreferencesService.FooterShortcutDisplay.HIDE);

        PreferencesSummaryViewModel viewModel = PreferencesSummaryScreen.viewModel("Preferences", preferencesService);

        assertEquals("Preferences", viewModel.title());
        assertEquals(6, viewModel.rows().size());
        assertEquals("Master volume", viewModel.rows().get(0).label());
        assertEquals("42%", viewModel.rows().get(0).value());
        assertEquals("Music volume", viewModel.rows().get(1).label());
        assertEquals("25%", viewModel.rows().get(1).value());
        assertEquals("Sound volume", viewModel.rows().get(2).label());
        assertEquals("75%", viewModel.rows().get(2).value());
        assertEquals("Theme color", viewModel.rows().get(3).label());
        assertEquals("Sunset - Light pastel", viewModel.rows().get(3).value());
        assertEquals("Footer icons", viewModel.rows().get(4).label());
        assertEquals("Icons with text", viewModel.rows().get(4).value());
        assertEquals("Footer display", viewModel.rows().get(5).label());
        assertEquals("Do not display", viewModel.rows().get(5).value());
        assertEquals("Music volume: 25%", viewModel.screenViewModel().lines().get(1));
        assertEquals("Close", viewModel.actions().get(0).label());
    }

    @Test
    void preferencesVolumeLabelsRenderAsPercentages() {
        assertEquals("0%", PreferencesSummaryScreen.percentLabel(0.0));
        assertEquals("50%", PreferencesSummaryScreen.percentLabel(0.5));
        assertEquals("100%", PreferencesSummaryScreen.percentLabel(1.0));
    }

    @Test
    void preferencesThemeOptionsListAllFamiliesAndVariants() {
        assertEquals(List.of(
                        "Ocean - Dark",
                        "Ocean - Light pastel",
                        "Forest - Dark",
                        "Forest - Light pastel",
                        "Sunset - Dark",
                        "Sunset - Light pastel",
                        "Violet - Dark",
                        "Violet - Light pastel",
                        "Crimson - Dark",
                        "Crimson - Light pastel"),
                PreferencesSummaryScreen.themeOptionLabels());
        assertEquals("Forest - Light pastel",
                PreferencesSummaryScreen.themeOptionLabel(
                        PreferencesService.ThemeFamily.FOREST,
                        PreferencesService.ThemeVariant.LIGHT_PASTEL));
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
