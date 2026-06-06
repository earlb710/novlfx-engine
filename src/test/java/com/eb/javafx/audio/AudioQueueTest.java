package com.eb.javafx.audio;

import com.eb.javafx.prefs.PreferencesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

final class AudioQueueTest {
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
    void queuedFactoryBuildsRequestForChannel() {
        SoundRequest request = SoundRequest.queued("music/theme2.ogg", AudioService.MUSIC_CHANNEL);
        assertEquals(AudioService.MUSIC_CHANNEL, request.channelId());
        assertEquals("music/theme2.ogg", request.sourcePath());
        assertFalse(request.loop());
        assertEquals(1.0, request.relativeVolume(), 0.0001);
    }

    @Test
    void musicFactoryBuildsLoopingRequestOnMusicChannel() {
        SoundRequest request = SoundRequest.music("music/theme.ogg");
        assertEquals(AudioService.MUSIC_CHANNEL, request.channelId());
        assertEquals("music/theme.ogg", request.sourcePath());
        assertTrue(request.loop(), "Background music should loop so it repeats when finished.");
        assertEquals(1.0, request.relativeVolume(), 0.0001);
    }

    @Test
    void musicRequestPlaysAsLoopingCommand() {
        AudioService service = initializedService();
        AudioPlaybackCommand command = service.play(SoundRequest.music("music/theme.ogg"));
        assertTrue(command.loop(), "Music playback command should loop indefinitely.");
        assertEquals(AudioService.MUSIC_CHANNEL, command.channelId());
    }

    @Test
    void queueMusicStoresPendingRequest() {
        AudioService service = initializedService();
        SoundRequest queued = SoundRequest.queued("music/track2.ogg", AudioService.MUSIC_CHANNEL);
        service.queueMusic(queued);

        assertTrue(service.queuedRequest(AudioService.MUSIC_CHANNEL).isPresent());
        assertEquals("music/track2.ogg", service.queuedRequest(AudioService.MUSIC_CHANNEL).get().sourcePath());
    }

    @Test
    void queueMusicReplacesExistingQueueEntry() {
        AudioService service = initializedService();
        service.queueMusic(SoundRequest.queued("music/track2.ogg", AudioService.MUSIC_CHANNEL));
        service.queueMusic(SoundRequest.queued("music/track3.ogg", AudioService.MUSIC_CHANNEL));

        assertEquals("music/track3.ogg", service.queuedRequest(AudioService.MUSIC_CHANNEL).get().sourcePath());
    }

    @Test
    void clearQueueRemovesPendingEntry() {
        AudioService service = initializedService();
        service.queueMusic(SoundRequest.queued("music/track2.ogg", AudioService.MUSIC_CHANNEL));
        service.clearQueue(AudioService.MUSIC_CHANNEL);

        assertFalse(service.queuedRequest(AudioService.MUSIC_CHANNEL).isPresent());
    }

    @Test
    void stopChannelAlsoClearsQueue() {
        AudioService service = initializedService();
        service.play(new SoundRequest(AudioService.MUSIC_CHANNEL, "music/track1.ogg", true, 1.0));
        service.queueMusic(SoundRequest.queued("music/track2.ogg", AudioService.MUSIC_CHANNEL));

        service.stopChannel(AudioService.MUSIC_CHANNEL);

        assertFalse(service.queuedRequest(AudioService.MUSIC_CHANNEL).isPresent());
    }

    @Test
    void queuedRequestEmptyByDefault() {
        AudioService service = initializedService();
        assertFalse(service.queuedRequest(AudioService.MUSIC_CHANNEL).isPresent());
    }
}
