package com.eb.javafx.audio;

import com.eb.javafx.util.Validation;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/** JavaFX media implementation of {@link AudioPlaybackAdapter} using app-owned asset resolution. */
public final class JavaFxAudioPlaybackAdapter implements AudioPlaybackAdapter, AutoCloseable {
    private final AudioAssetResolver assetResolver;
    private final Map<String, Integer> playerPoolSizes;
    private final LoopingPlayerFactory loopingPlayerFactory;
    private final OneShotPlayerFactory oneShotPlayerFactory;
    private final Map<String, LoopingPlayer> loopingPlayers = new LinkedHashMap<>();
    private final Map<String, Deque<OneShotPlayer>> oneShotPlayers = new LinkedHashMap<>();

    /** Creates an adapter from reusable policy metadata and an app-owned asset resolver. */
    public JavaFxAudioPlaybackAdapter(AudioAdapterPolicy policy) {
        this(policy.assetResolver(), policy.playerPoolSizes());
    }

    /** Creates an adapter with one non-looping player per channel and an app-owned asset resolver. */
    public JavaFxAudioPlaybackAdapter(AudioAssetResolver assetResolver) {
        this(assetResolver, Map.of());
    }

    /** Creates an adapter with app-owned asset resolution and channel-specific non-looping player pool sizes. */
    public JavaFxAudioPlaybackAdapter(AudioAssetResolver assetResolver, Map<String, Integer> playerPoolSizes) {
        this(assetResolver, playerPoolSizes, JavaFxAudioPlaybackAdapter::createLoopingMediaPlayer,
                JavaFxAudioPlaybackAdapter::createAudioClip);
    }

    JavaFxAudioPlaybackAdapter(
            AudioAssetResolver assetResolver,
            Map<String, Integer> playerPoolSizes,
            LoopingPlayerFactory loopingPlayerFactory,
            OneShotPlayerFactory oneShotPlayerFactory) {
        this.assetResolver = Validation.requireNonNull(assetResolver, "Audio asset resolver is required.");
        this.playerPoolSizes = validatePoolSizes(playerPoolSizes);
        this.loopingPlayerFactory = Validation.requireNonNull(loopingPlayerFactory, "Looping player factory is required.");
        this.oneShotPlayerFactory = Validation.requireNonNull(oneShotPlayerFactory, "One-shot player factory is required.");
    }

    @Override
    public void play(AudioPlaybackCommand command) {
        Validation.requireNonNull(command, "Audio playback command is required.");
        URI mediaUri = assetResolver.resolve(command.sourcePath())
                .orElseThrow(() -> new IllegalArgumentException("Audio asset could not be resolved: " + command.sourcePath()));
        if (command.loop()) {
            playLooping(command, mediaUri);
            return;
        }
        playOneShot(command, mediaUri);
    }

    @Override
    public void stopChannel(String channelId) {
        String validatedChannelId = Validation.requireNonBlank(channelId, "Audio channel id is required.");
        LoopingPlayer loopingPlayer = loopingPlayers.remove(validatedChannelId);
        if (loopingPlayer != null) {
            loopingPlayer.stop();
            loopingPlayer.dispose();
        }

        Deque<OneShotPlayer> players = oneShotPlayers.remove(validatedChannelId);
        if (players != null) {
            players.forEach(OneShotPlayer::stop);
        }
    }

    /** Stops all channel-owned players. */
    @Override
    public void close() {
        for (String channelId : new ArrayDeque<>(loopingPlayers.keySet())) {
            stopChannel(channelId);
        }
        for (String channelId : new ArrayDeque<>(oneShotPlayers.keySet())) {
            stopChannel(channelId);
        }
    }

    private void playLooping(AudioPlaybackCommand command, URI mediaUri) {
        stopChannel(command.channelId());
        LoopingPlayer player = loopingPlayerFactory.create(mediaUri);
        player.setVolume(command.effectiveVolume());
        loopingPlayers.put(command.channelId(), player);
        player.play();
    }

    private void playOneShot(AudioPlaybackCommand command, URI mediaUri) {
        Deque<OneShotPlayer> players = oneShotPlayers.computeIfAbsent(command.channelId(), ignored -> new ArrayDeque<>());
        int poolSize = playerPoolSizes.getOrDefault(command.channelId(), 1);
        while (players.size() >= poolSize) {
            players.removeFirst().stop();
        }
        OneShotPlayer player = oneShotPlayerFactory.create(mediaUri);
        player.setVolume(command.effectiveVolume());
        players.addLast(player);
        player.play();
    }

    private static Map<String, Integer> validatePoolSizes(Map<String, Integer> playerPoolSizes) {
        Map<String, Integer> validated = new LinkedHashMap<>();
        Validation.requireNonNull(playerPoolSizes, "Audio player pool sizes are required.").forEach((channelId, poolSize) ->
                validated.put(
                        Validation.requireNonBlank(channelId, "Audio channel id is required."),
                        Validation.requirePositive(
                                Validation.requireNonNull(poolSize, "Audio player pool size is required."),
                                "Audio player pool size must be positive.")));
        return Map.copyOf(validated);
    }

    private static LoopingPlayer createLoopingMediaPlayer(URI mediaUri) {
        MediaPlayer player = new MediaPlayer(new Media(mediaUri.toString()));
        player.setCycleCount(MediaPlayer.INDEFINITE);
        return new MediaPlayerLoopingPlayer(player);
    }

    private static OneShotPlayer createAudioClip(URI mediaUri) {
        return new AudioClipOneShotPlayer(new AudioClip(mediaUri.toString()));
    }

    @FunctionalInterface
    interface LoopingPlayerFactory {
        LoopingPlayer create(URI mediaUri);
    }

    @FunctionalInterface
    interface OneShotPlayerFactory {
        OneShotPlayer create(URI mediaUri);
    }

    interface LoopingPlayer {
        void setVolume(double volume);

        void play();

        void stop();

        void dispose();
    }

    interface OneShotPlayer {
        void setVolume(double volume);

        void play();

        void stop();
    }

    private record MediaPlayerLoopingPlayer(MediaPlayer player) implements LoopingPlayer {
        @Override
        public void setVolume(double volume) {
            player.setVolume(volume);
        }

        @Override
        public void play() {
            player.play();
        }

        @Override
        public void stop() {
            player.stop();
        }

        @Override
        public void dispose() {
            player.dispose();
        }
    }

    private record AudioClipOneShotPlayer(AudioClip clip) implements OneShotPlayer {
        @Override
        public void setVolume(double volume) {
            clip.setVolume(volume);
        }

        @Override
        public void play() {
            clip.play();
        }

        @Override
        public void stop() {
            clip.stop();
        }
    }
}
