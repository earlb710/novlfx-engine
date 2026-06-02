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
        preferences.put("ui.footerShortcutDisplay", "display");
        preferences.put("ui.footerIconDisplay", "icons-only");
        preferences.put("ui.fontFamily", "Test Font");
        preferences.putDouble("ui.fontScale", 9.0);
        preferences.put("ui.themeFamily", "forest");
        preferences.put("ui.themeVariant", "light-pastel");
        preferences.putBoolean("accessibility.highContrast", true);
        preferences.putBoolean("accessibility.reducedMotion", true);
        preferences.put("input.mode", "keyboard");
        preferences.putDouble("audio.masterVolume", -1.0);
        preferences.putDouble("audio.musicVolume", 2.0);
        preferences.putDouble("audio.soundVolume", 0.25);

        PreferencesService service = new PreferencesService();
        service.load();

        assertEquals(640, service.windowWidth());
        assertEquals(2160, service.windowHeight());
        assertEquals(0.0, service.hudAlpha());
        assertEquals(1.0, service.sayWindowAlpha());
        assertFalse(service.showPortrait());
        assertFalse(service.cheatsVisible());
        assertTrue(service.logStatChanges());
        assertTrue(service.footerLabelsVisible());
        assertEquals(PreferencesService.FooterShortcutDisplay.DISPLAY, service.footerShortcutDisplay());
        assertEquals(PreferencesService.FooterIconDisplay.ICONS_ONLY, service.footerIconDisplay());
        assertEquals("Test Font", service.fontFamily());
        assertEquals(2.0, service.fontScale());
        assertEquals(PreferencesService.ThemeFamily.FOREST, service.themeFamily());
        assertEquals(PreferencesService.ThemeVariant.LIGHT_PASTEL, service.themeVariant());
        assertTrue(service.highContrast());
        assertTrue(service.reducedMotion());
        assertEquals("keyboard", service.inputMode());
        assertEquals(0.0, service.masterVolume());
        assertEquals(1.0, service.musicVolume());
        assertEquals(0.25, service.soundVolume());
    }

    @Test
    void loadFallsBackForUnknownInputMode() {
        preferences.put("input.mode", "invalid");

        PreferencesService service = new PreferencesService();
        service.load();

        assertEquals("mouse", service.inputMode());
    }

    @Test
    void loadAndSaveOptionalToggleAndLanguagePreferences() {
        PreferencesService service = new PreferencesService();
        service.load();

        assertFalse(service.muteAll());
        assertFalse(service.fullscreen());
        assertEquals(PreferencesService.Language.ENGLISH, service.language());

        service.saveMuteAll(true);
        service.saveFullscreen(true);
        service.saveLanguage(PreferencesService.Language.SPANISH);

        assertTrue(service.muteAll());
        assertTrue(service.fullscreen());
        assertEquals(PreferencesService.Language.SPANISH, service.language());

        PreferencesService reloaded = new PreferencesService();
        reloaded.load();
        assertTrue(reloaded.muteAll());
        assertTrue(reloaded.fullscreen());
        assertEquals(PreferencesService.Language.SPANISH, reloaded.language());
    }

    @Test
    void saveLanguageFallsBackToEnglishForUnknownValues() {
        PreferencesService service = new PreferencesService();
        service.load();

        service.saveLanguage("xx");

        assertEquals(PreferencesService.Language.ENGLISH, service.language());
    }

    @Test
    void onlyEnglishLanguageIsEnabled() {
        assertTrue(PreferencesService.Language.ENGLISH.enabled());
        for (PreferencesService.Language language : PreferencesService.Language.values()) {
            if (language != PreferencesService.Language.ENGLISH) {
                assertFalse(language.enabled(), language + " should remain disabled.");
            }
        }
    }

    @Test
    void loadDefaultsFooterShortcutsToTooltipOnly() {
        PreferencesService service = new PreferencesService();
        service.load();

        assertEquals(PreferencesService.FooterShortcutDisplay.TOOLTIP_ONLY, service.footerShortcutDisplay());
        assertFalse(service.footerLabelsVisible());
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
    void configuredWindowBoundsDriveDefaultsAndClamps() {
        PreferencesService service = new PreferencesService();
        // Widen the clamp ranges and move the default; no stored value yet, so load() should
        // land on the configured default.
        service.setWindowSizeBounds(1600, 900, 800, 5120, 600, 2880);
        service.load();
        assertEquals(1600, service.windowWidth());
        assertEquals(900, service.windowHeight());

        // A value beyond the configured ceiling now clamps to the configured max (not 3840/2160).
        service.saveWindowSize(9999, 9999);
        service.load();
        assertEquals(5120, service.windowWidth());
        assertEquals(2880, service.windowHeight());
    }

    @Test
    void configuredFontScaleBoundsChangeClamping() {
        preferences.putDouble("ui.fontScale", 9.0);
        PreferencesService service = new PreferencesService();
        // Raise the ceiling to 3.0; the stored 9.0 now clamps to 3.0 instead of the default 2.0.
        service.setFontScaleBounds(0.5, 3.0);
        service.load();
        assertEquals(3.0, service.fontScale());

        // saveFontScale honours the configured floor too.
        service.saveFontScale(0.1);
        assertEquals(0.5, service.fontScale());
    }

    @Test
    void savePreferenceGroupsPersistAndUpdateLoadedValues() {
        PreferencesService service = new PreferencesService();
        service.load();

        service.saveUiVisibility(false, false, true);
        service.saveUiOpacity(2.0, -1.0);
        service.saveFontPreferences("", 9.0);
        service.saveThemePreferences(PreferencesService.ThemeFamily.SUNSET, PreferencesService.ThemeVariant.LIGHT_PASTEL);
        service.saveAccessibilityPreferences(true, true);
        service.saveInputMode("invalid");
        service.saveMasterVolume(2.0);
        service.saveAudioChannelVolumes(0.25, 0.75);
        service.saveFooterShortcutDisplay(PreferencesService.FooterShortcutDisplay.HIDE);

        assertFalse(service.showPortrait());
        assertFalse(service.cheatsVisible());
        assertTrue(service.logStatChanges());
        assertFalse(service.footerLabelsVisible());
        assertEquals(PreferencesService.FooterShortcutDisplay.HIDE, service.footerShortcutDisplay());
        assertEquals(1.0, service.hudAlpha());
        assertEquals(0.0, service.sayWindowAlpha());
        assertEquals("System", service.fontFamily());
        assertEquals(2.0, service.fontScale());
        assertEquals(PreferencesService.ThemeFamily.SUNSET, service.themeFamily());
        assertEquals(PreferencesService.ThemeVariant.LIGHT_PASTEL, service.themeVariant());
        assertTrue(service.highContrast());
        assertTrue(service.reducedMotion());
        assertEquals("mouse", service.inputMode());
        assertEquals(1.0, service.masterVolume());
        assertEquals(0.25, service.musicVolume());
        assertEquals(0.75, service.soundVolume());
    }

    @Test
    void saveIndividualAudioChannelVolumesClampsValues() {
        PreferencesService service = new PreferencesService();
        service.load();

        service.saveMusicVolume(-1.0);
        service.saveSoundVolume(2.0);

        assertEquals(0.0, service.musicVolume());
        assertEquals(1.0, service.soundVolume());
    }

    @Test
    void saveFooterShortcutDisplayAcceptsKnownValuesAndFallsBackForUnknownValues() {
        PreferencesService service = new PreferencesService();
        service.load();

        service.saveFooterShortcutDisplay("display");

        assertEquals(PreferencesService.FooterShortcutDisplay.DISPLAY, service.footerShortcutDisplay());
        assertTrue(service.footerLabelsVisible());

        service.saveFooterShortcutDisplay("invalid");

        assertEquals(PreferencesService.FooterShortcutDisplay.TOOLTIP_ONLY, service.footerShortcutDisplay());
    }

    @Test
    void saveFooterIconDisplayAcceptsKnownValuesAndFallsBackForUnknownValues() {
        PreferencesService service = new PreferencesService();
        service.load();

        service.saveFooterIconDisplay("icons-only");

        assertEquals(PreferencesService.FooterIconDisplay.ICONS_ONLY, service.footerIconDisplay());

        service.saveFooterIconDisplay("text-only");

        assertEquals(PreferencesService.FooterIconDisplay.TEXT_ONLY, service.footerIconDisplay());

        service.saveFooterIconDisplay("invalid");

        assertEquals(PreferencesService.FooterIconDisplay.ICONS_WITH_TEXT, service.footerIconDisplay());
    }

    @Test
    void loadAndSaveThemePreferencesValidateUnknownValues() {
        preferences.put("ui.themeFamily", "invalid");
        preferences.put("ui.themeVariant", "invalid");

        PreferencesService service = new PreferencesService();
        service.load();

        assertEquals(PreferencesService.ThemeFamily.OCEAN, service.themeFamily());
        assertEquals(PreferencesService.ThemeVariant.DARK, service.themeVariant());

        service.saveThemePreferences("violet", "light-pastel");
        assertEquals(PreferencesService.ThemeFamily.VIOLET, service.themeFamily());
        assertEquals(PreferencesService.ThemeVariant.LIGHT_PASTEL, service.themeVariant());

        service.saveThemePreferences("bad", "bad");
        assertEquals(PreferencesService.ThemeFamily.OCEAN, service.themeFamily());
        assertEquals(PreferencesService.ThemeVariant.DARK, service.themeVariant());
    }
}
