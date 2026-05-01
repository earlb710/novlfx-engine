package com.lr2alt.javafx.ui;

import com.lr2alt.javafx.prefs.PreferencesService;
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
    }

    @Test
    void initializeLoadsThemeTokensFromPreferencesAndDefaults() {
        preferences.put("ui.fontFamily", "Theme Test Font");
        preferences.putDouble("ui.fontScale", 1.5);
        preferences.putBoolean("accessibility.highContrast", true);
        preferences.putBoolean("accessibility.reducedMotion", true);
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();

        UiTheme theme = new UiTheme();
        theme.initialize(preferencesService);

        assertEquals("Theme Test Font", theme.fontFamily());
        assertEquals("#ffff66", theme.accentColor());
        assertEquals("#ffffff", theme.textColor());
        assertEquals("#000000", theme.panelBackground());
        assertEquals("#333300", theme.hoverBackground());
        assertEquals(1.5, theme.fontScale());
        assertTrue(theme.highContrast());
        assertTrue(theme.reducedMotion());
        assertTrue(theme.stylesheet().endsWith("/com/lr2alt/javafx/ui/lr2alt.css"));
    }
}
