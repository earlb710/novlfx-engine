package com.eb.javafx.prefs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PreferencesServiceTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        preferences.clear();
        preferences.flush();
    }

    @Test
    void loadReadsAndClampsPersistentUiPreferences() {
        preferences.putInt("window.width", 10);
        preferences.putInt("window.height", 9999);
        preferences.putDouble("ui.hudAlpha", -1.0);
        preferences.putDouble("ui.sayWindowAlpha", 2.0);
        preferences.putBoolean("ui.showPortrait", false);
        preferences.putBoolean("ui.cheatsVisible", false);
        preferences.putBoolean("ui.logStatChanges", true);
        preferences.put("ui.fontFamily", "Test Font");
        preferences.putDouble("ui.fontScale", 9.0);
        preferences.putBoolean("accessibility.highContrast", true);
        preferences.putBoolean("accessibility.reducedMotion", true);
        preferences.put("input.mode", "keyboard");
        preferences.putDouble("audio.masterVolume", -1.0);

        PreferencesService service = new PreferencesService();
        service.load();

        assertEquals(640, service.windowWidth());
        assertEquals(2160, service.windowHeight());
        assertEquals(0.0, service.hudAlpha());
        assertEquals(1.0, service.sayWindowAlpha());
        assertFalse(service.showPortrait());
        assertFalse(service.cheatsVisible());
        assertTrue(service.logStatChanges());
        assertEquals("Test Font", service.fontFamily());
        assertEquals(2.0, service.fontScale());
        assertTrue(service.highContrast());
        assertTrue(service.reducedMotion());
        assertEquals("keyboard", service.inputMode());
        assertEquals(0.0, service.masterVolume());
    }

    @Test
    void loadFallsBackForUnknownInputMode() {
        preferences.put("input.mode", "invalid");

        PreferencesService service = new PreferencesService();
        service.load();

        assertEquals("mouse", service.inputMode());
    }

    @Test
    void saveWindowSizePersistsClampedValues() {
        PreferencesService service = new PreferencesService();

        service.saveWindowSize(9999, 10);
        service.load();

        assertEquals(3840, service.windowWidth());
        assertEquals(480, service.windowHeight());
    }

    @Test
    void savePreferenceGroupsPersistAndUpdateLoadedValues() {
        PreferencesService service = new PreferencesService();
        service.load();

        service.saveUiVisibility(false, false, true);
        service.saveUiOpacity(2.0, -1.0);
        service.saveFontPreferences("", 9.0);
        service.saveAccessibilityPreferences(true, true);
        service.saveInputMode("invalid");
        service.saveMasterVolume(2.0);

        assertFalse(service.showPortrait());
        assertFalse(service.cheatsVisible());
        assertTrue(service.logStatChanges());
        assertEquals(1.0, service.hudAlpha());
        assertEquals(0.0, service.sayWindowAlpha());
        assertEquals("System", service.fontFamily());
        assertEquals(2.0, service.fontScale());
        assertTrue(service.highContrast());
        assertTrue(service.reducedMotion());
        assertEquals("mouse", service.inputMode());
        assertEquals(1.0, service.masterVolume());
    }
}
