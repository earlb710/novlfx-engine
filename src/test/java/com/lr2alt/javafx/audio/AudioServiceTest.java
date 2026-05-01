package com.lr2alt.javafx.audio;

import com.lr2alt.javafx.prefs.PreferencesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioServiceTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        preferences.clear();
        preferences.flush();
    }

    @Test
    void initializeRegistersDefaultChannelsAndMasterVolume() {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.saveMasterVolume(0.5);
        preferencesService.load();

        AudioService service = new AudioService();
        service.initialize(preferencesService);

        assertTrue(service.isInitialized());
        assertEquals(4, service.channels().size());
        assertEquals(0.5, service.masterVolume());
        assertTrue(service.channels().get(AudioService.MUSIC_CHANNEL).loopingAllowed());
        assertFalse(service.channels().get(AudioService.SOUND_CHANNEL).loopingAllowed());
    }

    @Test
    void playValidatesChannelLoopingAndComputesEffectiveVolume() {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.saveMasterVolume(0.5);
        preferencesService.load();
        AudioService service = new AudioService();
        service.initialize(preferencesService);
        service.setChannelVolume(AudioService.EFFECTS_CHANNEL, 0.4);

        AudioPlaybackCommand command = service.play(new SoundRequest(AudioService.EFFECTS_CHANNEL, "effect.ogg", true, 0.5));

        assertEquals(AudioService.EFFECTS_CHANNEL, command.channelId());
        assertEquals("effect.ogg", command.sourcePath());
        assertEquals(0.1, command.effectiveVolume(), 0.0001);
        assertEquals(command, service.lastPlaybackCommand(AudioService.EFFECTS_CHANNEL).orElseThrow());
        assertThrows(IllegalArgumentException.class, () ->
                service.play(new SoundRequest(AudioService.SOUND_CHANNEL, "loop.ogg", true, 1.0)));
    }
}
