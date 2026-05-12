package com.eb.javafx.audio;

import com.eb.javafx.prefs.PreferencesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

final class VoiceChannelPrefsTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        preferences.clear();
        preferences.flush();
    }

    @Test
    void voiceEnabledDefaultsToTrue() {
        PreferencesService prefs = new PreferencesService();
        prefs.load();
        assertTrue(prefs.voiceEnabled());
    }

    @Test
    void voiceVolumeDefaultsToOne() {
        PreferencesService prefs = new PreferencesService();
        prefs.load();
        assertEquals(1.0, prefs.voiceVolume(), 0.0001);
    }

    @Test
    void autoAdvanceOnVoiceEndDefaultsToFalse() {
        PreferencesService prefs = new PreferencesService();
        prefs.load();
        assertFalse(prefs.autoAdvanceOnVoiceEnd());
    }

    @Test
    void saveVoicePreferencesPersistAndReload() {
        PreferencesService prefs = new PreferencesService();
        prefs.saveVoiceVolume(0.6);
        prefs.saveVoiceEnabled(false);
        prefs.saveAutoAdvanceOnVoiceEnd(true);

        PreferencesService reloaded = new PreferencesService();
        reloaded.load();

        assertEquals(0.6, reloaded.voiceVolume(), 0.0001);
        assertFalse(reloaded.voiceEnabled());
        assertTrue(reloaded.autoAdvanceOnVoiceEnd());
    }

    @Test
    void initializeRegistersVoiceChannelWithVoiceVolume() {
        PreferencesService prefs = new PreferencesService();
        prefs.saveVoiceVolume(0.7);
        prefs.load();

        AudioService service = new AudioService();
        service.initialize(prefs);

        assertTrue(service.channels().containsKey(AudioService.VOICE_CHANNEL));
        assertEquals(0.7, service.channelVolume(AudioService.VOICE_CHANNEL), 0.0001);
    }
}
