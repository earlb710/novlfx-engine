package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiThemeTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        preferences.clear();
        preferences.flush();
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        new UiTheme().initialize(preferencesService);
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
