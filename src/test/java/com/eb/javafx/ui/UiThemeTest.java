package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.prefs.PreferencesService.ThemeFamily;
import com.eb.javafx.prefs.PreferencesService.ThemeVariant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiThemeTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        UiTheme.clearCustomPalette();
        preferences.clear();
        preferences.flush();
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        new UiTheme().initialize(preferencesService);
        DisplayDefaults.resetActive();
    }

    @Test
    void initializeLoadsThemeTokensFromPreferencesAndDefaults() {
        preferences.put("ui.fontFamily", "Theme Test Font");
        preferences.putDouble("ui.fontScale", 1.5);
        preferences.put("ui.themeFamily", "violet");
        preferences.put("ui.themeVariant", "light-pastel");
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        assertEquals("Theme Test Font", theme.fontFamily());
        assertEquals("#775fc1", theme.accentColor());
        assertEquals("#000000", theme.textColor());
        assertEquals("rgba(240, 232, 255, 0.92)", theme.panelBackground());
        assertEquals("#e6dafd", theme.hoverBackground());
        assertEquals(1.5, theme.fontScale());
        assertTrue(theme.stylesheet().startsWith("file:"));
        assertTrue(theme.stylesheetContent().contains("-fx-background-color: #faf6ff;"));
        assertTrue(theme.stylesheetContent().contains("-fx-selection-bar: #775fc1;"));
        assertTrue(theme.stylesheetContent().contains(".combo-box-popup .list-cell:selected"));
        assertTrue(theme.stylesheetContent().contains(".screen-text-highlight"));
        assertTrue(theme.stylesheetContent().contains(".screen-value"));
        assertTrue(theme.stylesheetContent().contains(".screen-text"));
        assertTrue(theme.stylesheetContent().contains("-fx-text-fill: #775fc1;"));
        assertTrue(theme.stylesheetContent().contains("-fx-text-fill: #5b5076;"));
        assertTrue(theme.stylesheetContent().contains("-fx-text-fill: #000000;"));
    }

    @Test
    void darkThemesUseWhiteDefaultText() {
        preferences.put("ui.themeFamily", "violet");
        preferences.put("ui.themeVariant", "dark");
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        assertEquals("#ffffff", theme.textColor());
        assertTrue(theme.stylesheetContent().contains("-fx-text-fill: #ffffff;"));
    }

    @Test
    void generatedStylesheetIncludesDialogBlockRulesWithOpaqueBlackBackground() {
        // The runtime stylesheet (UiTheme.stylesheet()) is what every consumer of the engine
        // theme — including the manual dialog-block demo — actually loads. The bundled
        // default.css is only the *contract* reference; it isn't fetched at runtime. This test
        // locks in that the generated theme carries the dialog block rules so the widget shows
        // an opaque black background and a readable bright-white current entry regardless of
        // which palette the player has selected.
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        String css = theme.stylesheetContent();
        assertTrue(css.contains(".dialog-entries-view"),
                "Generated stylesheet should style the dialog block (.dialog-entries-view).");
        assertTrue(css.contains(".layout-main-app-dialog"),
                "Generated stylesheet should style the main-app-layout dialog slot.");
        assertTrue(css.contains(".dialog-entries-view {")
                        && css.substring(css.indexOf(".dialog-entries-view {"))
                                .contains("-fx-background-color: #000000;"),
                "Dialog block background should be opaque black (#000000).");
        assertTrue(css.contains(".layout-main-app-dialog {")
                        && css.substring(css.indexOf(".layout-main-app-dialog {"))
                                .contains("-fx-background-color: #000000;"),
                "Main app layout dialog slot background should be opaque black (#000000).");
        assertTrue(css.contains(".dialog-entries-view > .viewport"),
                "ScrollPane viewport must be transparent so the dialog block's own black surfaces.");
        assertTrue(css.contains(".dialog-entry-current")
                        && css.substring(css.indexOf(".dialog-entry-current"))
                                .contains("-fx-text-fill: #ffffff;"),
                "Current dialog entry should be bright white on the opaque black background.");
    }

    @Test
    void everyThemeSelectionBuildsSemanticStylesheetContent() {
        for (ThemeFamily family : ThemeFamily.values()) {
            for (ThemeVariant variant : ThemeVariant.values()) {
                preferences.put("ui.themeFamily", family.preferenceValue());
                preferences.put("ui.themeVariant", variant.preferenceValue());
                PreferencesService preferencesService = new PreferencesService();
                preferencesService.load();

                UiTheme theme = new UiTheme();
                theme.initialize(preferencesService);

                assertTrue(theme.stylesheet().startsWith("file:"));
                assertTrue(theme.stylesheetContent().contains(".screen-text-highlight"));
                assertTrue(theme.stylesheetContent().contains(".screen-value"));
                assertTrue(theme.stylesheetContent().contains(".screen-text"));
            }
        }
    }

    @Test
    void darkThemeProducesDarkFieldBackgroundAndLightFieldText() {
        preferences.put("ui.themeFamily", "ocean");
        preferences.put("ui.themeVariant", "dark");
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        RoleColors roleColors = theme.roleColors();
        assertEquals("#ffffff", roleColors.fieldColor());
        assertEquals("#0a1426", roleColors.fieldBackground());
        assertEquals("#0a1426",
                theme.themedDisplayDefaults().itemDefaults(DisplayDefaults.ROLE_FIELD).get("backgroundColor"));
        assertEquals("#ffffff",
                theme.themedDisplayDefaults().itemDefaults(DisplayDefaults.ROLE_FIELD).get("color"));
    }

    @Test
    void lightPastelThemeProducesLighterFieldBackgroundAndDarkFieldText() {
        preferences.put("ui.themeFamily", "ocean");
        preferences.put("ui.themeVariant", "light-pastel");
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        RoleColors roleColors = theme.roleColors();
        assertEquals("#1f3e50", roleColors.fieldColor());
        assertEquals("#fbfeff", roleColors.fieldBackground());
        assertEquals("#fbfeff",
                theme.themedDisplayDefaults().itemDefaults(DisplayDefaults.ROLE_FIELD).get("backgroundColor"));
        assertEquals("#1f3e50",
                theme.themedDisplayDefaults().itemDefaults(DisplayDefaults.ROLE_FIELD).get("color"));
    }

    @Test
    void initializeInstallsThemedDefaultsAsActive() {
        preferences.put("ui.themeFamily", "forest");
        preferences.put("ui.themeVariant", "light-pastel");
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        assertEquals("#1d3427",
                DisplayDefaults.active().itemDefaults(DisplayDefaults.ROLE_FIELD).get("color"));
        assertEquals("#f5fdf8",
                DisplayDefaults.active().itemDefaults(DisplayDefaults.ROLE_FIELD).get("backgroundColor"));
    }

    @Test
    void crimsonThemeProducesRedAccentColors() {
        preferences.put("ui.themeFamily", "crimson");
        preferences.put("ui.themeVariant", "dark");
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        assertEquals("#e83030", theme.accentColor());
        assertTrue(theme.stylesheet().startsWith("file:"));
        assertTrue(theme.stylesheetContent().contains("-fx-selection-bar: #e83030;"));

        preferences.put("ui.themeVariant", "light-pastel");
        preferencesService.load();
        theme.initialize(preferencesService);

        assertEquals("#c01515", theme.accentColor());
        assertTrue(theme.stylesheetContent().contains("-fx-selection-bar: #c01515;"));
    }

    @Test
    void accentOverrideStylesheetGeneratesOverrideForSpecifiedColor() {
        preferences.put("ui.themeFamily", "ocean");
        preferences.put("ui.themeVariant", "dark");
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        String overrideUri = theme.accentOverrideStylesheet("#ff4444");

        assertTrue(overrideUri.startsWith("file:"));

        String nullResult = theme.accentOverrideStylesheet(null);
        String blankResult = theme.accentOverrideStylesheet("  ");
        assertEquals(null, nullResult);
        assertEquals(null, blankResult);
    }

    @Test
    void customPaletteOverridesSelectedPaletteColors(@TempDir Path tempDir) throws Exception {
        Path json = tempDir.resolve("palette.json");
        Files.writeString(json, "{ \"baseFamily\": \"forest\", \"baseVariant\": \"dark\","
                + " \"colors\": { \"accentColor\": \"#abcdef\", \"footerIconColor\": \"#123456\" } }");
        UiTheme.loadCustomPalette(json);

        // Selected family is ocean, but the spec's baseFamily=forest is the starting palette.
        preferences.put("ui.themeFamily", "ocean");
        preferences.put("ui.themeVariant", "dark");
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        assertEquals("#abcdef", theme.accentColor(), "accentColor override should apply.");
        assertEquals("#123456", theme.footerIconColor(), "footerIconColor override should apply.");
        // A field not in the override keeps the forest/dark base value (labelText = #ffffff).
        assertEquals("#ffffff", theme.textColor());
        assertTrue(theme.stylesheetContent().contains("-fx-selection-bar: #abcdef;"));
    }

    @Test
    void customPaletteWithoutBaseUsesSelectedFamilyAsBase(@TempDir Path tempDir) throws Exception {
        Path json = tempDir.resolve("palette.json");
        Files.writeString(json, "{ \"colors\": { \"accentColor\": \"#0a0b0c\" } }");
        UiTheme.loadCustomPalette(json);

        preferences.put("ui.themeFamily", "crimson");
        preferences.put("ui.themeVariant", "dark");
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        assertEquals("#0a0b0c", theme.accentColor());
        // Base remained crimson/dark — its default accent (#e83030) was replaced, not the rest.
        assertNotEquals("#e83030", theme.accentColor());
    }

    @Test
    void highContrastWinsOverCustomPalette(@TempDir Path tempDir) throws Exception {
        Path json = tempDir.resolve("palette.json");
        Files.writeString(json, "{ \"colors\": { \"accentColor\": \"#abcdef\" } }");
        UiTheme.loadCustomPalette(json);

        preferences.putBoolean("accessibility.highContrast", true);
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        // High contrast palette ignores the custom palette for accessibility.
        assertEquals("#ffff66", theme.accentColor());
    }

    @Test
    void loadCustomPaletteNullClearsOverride(@TempDir Path tempDir) {
        UiTheme.loadCustomPalette(null);
        preferences.put("ui.themeFamily", "ocean");
        preferences.put("ui.themeVariant", "dark");
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        assertEquals("#66c1e0", theme.accentColor(), "No custom palette => stock ocean/dark accent.");
    }

    @Test
    void initializeKeepsHighContrastOverrideForAnyThemeSelection() {
        preferences.put("ui.themeFamily", "forest");
        preferences.put("ui.themeVariant", "light-pastel");
        preferences.putBoolean("accessibility.highContrast", true);
        preferences.putBoolean("accessibility.reducedMotion", true);
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        assertEquals("#ffff66", theme.accentColor());
        assertEquals("#ffffff", theme.textColor());
        assertEquals("#333300", theme.hoverBackground());
        assertTrue(theme.highContrast());
        assertTrue(theme.reducedMotion());
        assertTrue(theme.stylesheetContent().contains("-fx-background-color: #000000;"));
        assertTrue(theme.stylesheetContent().contains("-fx-selection-bar-text: #ffffff;"));
    }
}
