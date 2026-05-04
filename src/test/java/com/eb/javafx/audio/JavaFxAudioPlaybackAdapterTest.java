package com.eb.javafx.audio;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaFxAudioPlaybackAdapterTest {
    @Test
    void loopingPlaybackReplacesExistingChannelPlayer() {
        List<FakeLoopingPlayer> loopingPlayers = new ArrayList<>();
        JavaFxAudioPlaybackAdapter adapter = new JavaFxAudioPlaybackAdapter(
                source -> Optional.of(URI.create("file:///app/" + source)),
                Map.of(),
                uri -> addLooping(loopingPlayers, uri),
                FakeOneShotPlayer::new);

        adapter.play(new AudioPlaybackCommand(AudioService.MUSIC_CHANNEL, "theme.ogg", true, 0.75));
        adapter.play(new AudioPlaybackCommand(AudioService.MUSIC_CHANNEL, "battle.ogg", true, 0.5));

        assertEquals(2, loopingPlayers.size());
        assertEquals(URI.create("file:///app/theme.ogg"), loopingPlayers.get(0).uri);
        assertEquals(0.75, loopingPlayers.get(0).volume);
        assertTrue(loopingPlayers.get(0).played);
        assertTrue(loopingPlayers.get(0).stopped);
        assertTrue(loopingPlayers.get(0).disposed);
        assertEquals(0.5, loopingPlayers.get(1).volume);
        assertTrue(loopingPlayers.get(1).played);
    }

    @Test
    void oneShotPlaybackHonorsChannelPoolSize() {
        List<FakeOneShotPlayer> oneShotPlayers = new ArrayList<>();
        JavaFxAudioPlaybackAdapter adapter = new JavaFxAudioPlaybackAdapter(
                source -> Optional.of(URI.create("file:///app/" + source)),
                Map.of(AudioService.SOUND_CHANNEL, 2),
                FakeLoopingPlayer::new,
                uri -> addOneShot(oneShotPlayers, uri));

        adapter.play(new AudioPlaybackCommand(AudioService.SOUND_CHANNEL, "a.wav", false, 1.0));
        adapter.play(new AudioPlaybackCommand(AudioService.SOUND_CHANNEL, "b.wav", false, 0.5));
        adapter.play(new AudioPlaybackCommand(AudioService.SOUND_CHANNEL, "c.wav", false, 0.25));

        assertEquals(3, oneShotPlayers.size());
        assertTrue(oneShotPlayers.get(0).stopped);
        assertTrue(oneShotPlayers.get(1).played);
        assertTrue(oneShotPlayers.get(2).played);
        assertEquals(0.25, oneShotPlayers.get(2).volume);
    }

    @Test
    void stopChannelStopsLoopingAndOneShotPlayers() {
        List<FakeLoopingPlayer> loopingPlayers = new ArrayList<>();
        List<FakeOneShotPlayer> oneShotPlayers = new ArrayList<>();
        JavaFxAudioPlaybackAdapter adapter = new JavaFxAudioPlaybackAdapter(
                source -> Optional.of(URI.create("file:///app/" + source)),
                Map.of(AudioService.SOUND_CHANNEL, 2),
                uri -> addLooping(loopingPlayers, uri),
                uri -> addOneShot(oneShotPlayers, uri));

        adapter.play(new AudioPlaybackCommand(AudioService.MUSIC_CHANNEL, "theme.ogg", true, 1.0));
        adapter.play(new AudioPlaybackCommand(AudioService.SOUND_CHANNEL, "a.wav", false, 1.0));
        adapter.play(new AudioPlaybackCommand(AudioService.SOUND_CHANNEL, "b.wav", false, 1.0));

        adapter.stopChannel(AudioService.MUSIC_CHANNEL);
        adapter.stopChannel(AudioService.SOUND_CHANNEL);

        assertTrue(loopingPlayers.get(0).stopped);
        assertTrue(loopingPlayers.get(0).disposed);
        assertTrue(oneShotPlayers.get(0).stopped);
        assertTrue(oneShotPlayers.get(1).stopped);
    }

    @Test
    void unresolvedAssetsFailBeforeCreatingPlayer() {
        JavaFxAudioPlaybackAdapter adapter = new JavaFxAudioPlaybackAdapter(
                source -> Optional.empty(),
                Map.of(),
                FakeLoopingPlayer::new,
                FakeOneShotPlayer::new);

        assertThrows(IllegalArgumentException.class,
                () -> adapter.play(new AudioPlaybackCommand(AudioService.MUSIC_CHANNEL, "missing.ogg", true, 1.0)));
    }

    private static FakeLoopingPlayer addLooping(List<FakeLoopingPlayer> players, URI uri) {
        FakeLoopingPlayer player = new FakeLoopingPlayer(uri);
        players.add(player);
        return player;
    }

    private static FakeOneShotPlayer addOneShot(List<FakeOneShotPlayer> players, URI uri) {
        FakeOneShotPlayer player = new FakeOneShotPlayer(uri);
        players.add(player);
        return player;
    }

    private static final class FakeLoopingPlayer implements JavaFxAudioPlaybackAdapter.LoopingPlayer {
        private final URI uri;
        private boolean played;
        private boolean stopped;
        private boolean disposed;
        private double volume;

        private FakeLoopingPlayer(URI uri) {
            this.uri = uri;
        }

        @Override
        public void setVolume(double volume) {
            this.volume = volume;
        }

        @Override
        public void play() {
            played = true;
        }

        @Override
        public void stop() {
            stopped = true;
        }

        @Override
        public void dispose() {
            disposed = true;
        }
    }

    private static final class FakeOneShotPlayer implements JavaFxAudioPlaybackAdapter.OneShotPlayer {
        private final URI uri;
        private boolean played;
        private boolean stopped;
        private double volume;

        private FakeOneShotPlayer(URI uri) {
            this.uri = uri;
        }

        @Override
        public void setVolume(double volume) {
            this.volume = volume;
        }

        @Override
        public void play() {
            played = true;
        }

        @Override
        public void stop() {
            stopped = true;
        }
    }
}
