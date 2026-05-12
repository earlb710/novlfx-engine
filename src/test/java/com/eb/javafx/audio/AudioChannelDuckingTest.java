package com.eb.javafx.audio;

import com.eb.javafx.prefs.PreferencesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

final class AudioChannelDuckingTest {
    private final Preferences preferences = Preferences.userNodeForPackage(PreferencesService.class);

    @AfterEach
    void clearTestPreferences() throws BackingStoreException {
        preferences.clear();
        preferences.flush();
    }

    private AudioService initializedService() {
        PreferencesService prefs = new PreferencesService();
        prefs.load();
        AudioService service = new AudioService();
        service.initialize(prefs);
        return service;
    }

    @Test
    void audioChannelConfigStoresAllFields() {
        AudioChannelConfig config = new AudioChannelConfig(
                AudioService.VOICE_CHANNEL, 10, 1.0, DuckingPolicy.REDUCE_TO_PERCENT, 0.3);
        assertEquals(AudioService.VOICE_CHANNEL, config.channelId());
        assertEquals(10, config.priority());
        assertEquals(1.0, config.defaultVolume(), 0.0001);
        assertEquals(DuckingPolicy.REDUCE_TO_PERCENT, config.duckingPolicy());
        assertEquals(0.3, config.duckingPercent(), 0.0001);
    }

    @Test
    void duckingPolicyNoneIgnoresDuckingPercent() {
        AudioChannelConfig config = new AudioChannelConfig(
                AudioService.MUSIC_CHANNEL, 5, 1.0, DuckingPolicy.NONE, 0.99);
        assertEquals(DuckingPolicy.NONE, config.duckingPolicy());
        assertEquals(0.0, config.duckingPercent(), 0.0001);
    }

    @Test
    void audioChannelConfigRequiresNonBlankChannelId() {
        assertThrows(IllegalArgumentException.class, () ->
                new AudioChannelConfig("", 5, 1.0, DuckingPolicy.NONE, 0.0));
    }

    @Test
    void audioChannelConfigRequiresVolumeInUnitInterval() {
        assertThrows(IllegalArgumentException.class, () ->
                new AudioChannelConfig(AudioService.MUSIC_CHANNEL, 5, 1.5, DuckingPolicy.NONE, 0.0));
    }

    @Test
    void registerChannelConfigStoresAndRetrievesConfig() {
        AudioService service = initializedService();
        AudioChannelConfig config = new AudioChannelConfig(
                AudioService.VOICE_CHANNEL, 10, 1.0, DuckingPolicy.PAUSE, 0.0);
        service.registerChannel(config);

        AudioChannelConfig retrieved = service.channelConfig(AudioService.VOICE_CHANNEL);
        assertEquals(DuckingPolicy.PAUSE, retrieved.duckingPolicy());
        assertEquals(10, retrieved.priority());
    }

    @Test
    void unregisteredChannelReturnsDefaultConfig() {
        AudioService service = initializedService();
        AudioChannelConfig config = service.channelConfig(AudioService.MUSIC_CHANNEL);

        assertEquals(AudioService.MUSIC_CHANNEL, config.channelId());
        assertEquals(0, config.priority());
        assertEquals(DuckingPolicy.NONE, config.duckingPolicy());
        assertEquals(0.0, config.duckingPercent(), 0.0001);
    }

    @Test
    void registerChannelConfigWorksAlongsideExistingChannelDefinition() {
        AudioService service = initializedService();
        service.registerChannel(new AudioChannelDefinition("custom", "Custom channel.", false, 1, 1.0));
        service.registerChannel(new AudioChannelConfig("custom", 3, 0.8, DuckingPolicy.REDUCE_TO_PERCENT, 0.5));

        assertEquals(DuckingPolicy.REDUCE_TO_PERCENT, service.channelConfig("custom").duckingPolicy());
    }
}
