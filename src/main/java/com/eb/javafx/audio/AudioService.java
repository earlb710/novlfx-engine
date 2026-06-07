package com.eb.javafx.audio;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.util.InitializationGuard;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Section 1.5 audio/media boundary for migrated JavaFX systems.
 *
 * <p>Source engines may expose global music and sound helpers. This service replaces that
 * with named channel definitions, preference-backed master volume, and validated
 * playback commands that can later be bridged to JavaFX media classes.</p>
 *
 * <p>Callers must initialize the service with loaded preferences before reading
 * volume state or issuing playback commands. Channel IDs are validated, mutable
 * volume settings are clamped to {@code 0.0..1.0}, and muting is represented by
 * silent playback commands without forgetting configured volumes.</p>
 */
public final class AudioService {
    public static final String MUSIC_CHANNEL = "music";
    public static final String SOUND_CHANNEL = "sound";
    public static final String EFFECTS_CHANNEL = "effects";
    public static final String INTIMATE_EFFECTS_CHANNEL = "intimate-effects";
    public static final String VOICE_CHANNEL = "voice";

    private final Map<String, AudioChannelDefinition> channels = new LinkedHashMap<>();
    private final Map<String, Double> channelVolumes = new LinkedHashMap<>();
    private final Map<String, AudioPlaybackCommand> lastPlaybackCommands = new LinkedHashMap<>();
    private final Map<String, SoundRequest> queuedRequests = new LinkedHashMap<>();
    private final Map<String, AudioChannelConfig> channelConfigs = new LinkedHashMap<>();
    private final InitializationGuard initializationGuard = new InitializationGuard("Audio service used before initialization.");
    private final List<Runnable> volumeChangeListeners = new ArrayList<>();
    private boolean muted;
    private double masterVolume;

    /**
     * Registers default channel names for the audio system and loads master volume.
     *
     * @param preferencesService loaded preferences supplying persisted audio settings
     * @throws IllegalArgumentException when preferences are unavailable
     */
    public void initialize(PreferencesService preferencesService) {
        Validation.requireNonNull(preferencesService, "Preferences service is required for audio initialization.");
        channels.clear();
        channelVolumes.clear();
        lastPlaybackCommands.clear();
        queuedRequests.clear();
        channelConfigs.clear();
        masterVolume = preferencesService.masterVolume();
        muted = preferencesService.muteAll();
        registerChannel(new AudioChannelDefinition(MUSIC_CHANNEL, "Looping background music.", true, 1, 1.0));
        registerChannel(new AudioChannelDefinition(SOUND_CHANNEL, "Short one-shot UI and gameplay sounds.", false, 8, 1.0));
        registerChannel(new AudioChannelDefinition(EFFECTS_CHANNEL, "Reusable environmental and scene effects.", true, 4, 1.0));
        registerChannel(new AudioChannelDefinition(INTIMATE_EFFECTS_CHANNEL, "Dedicated migrated effect channel.", true, 2, 1.0));
        registerChannel(new AudioChannelDefinition(VOICE_CHANNEL, "Character voice lines.", false, 1, 1.0));
        channelVolumes.put(MUSIC_CHANNEL, preferencesService.musicVolume());
        channelVolumes.put(SOUND_CHANNEL, preferencesService.soundVolume());
        channelVolumes.put(VOICE_CHANNEL, preferencesService.voiceVolume());
        initializationGuard.markInitialized();
    }

    /** Returns whether startup has prepared the audio boundary. */
    public boolean isInitialized() {
        return initializationGuard.isInitialized();
    }

    /**
     * Registers or replaces a channel definition and resets its channel volume.
     *
     * @param definition validated channel metadata keyed by stable ID
     */
    public void registerChannel(AudioChannelDefinition definition) {
        channels.put(definition.id(), definition);
        channelVolumes.put(definition.id(), definition.defaultVolume());
    }

    /**
     * Registers priority and ducking configuration for a named channel.
     * Replaces any previously registered config for that channel ID.
     * The channel ID does not need to have an {@link AudioChannelDefinition} registered first.
     *
     * @param config priority and ducking policy for the channel
     */
    public void registerChannel(AudioChannelConfig config) {
        channelConfigs.put(Validation.requireNonNull(config, "config").channelId(), config);
    }

    /** Returns immutable channel definitions keyed by channel id. */
    public Map<String, AudioChannelDefinition> channels() {
        return Collections.unmodifiableMap(channels);
    }

    /** Returns the startup master volume loaded from user preferences. */
    public double masterVolume() {
        assertInitialized();
        return masterVolume;
    }

    /** Updates the in-memory master volume after validating persisted preferences. */
    public void setMasterVolume(double masterVolume) {
        assertInitialized();
        this.masterVolume = clampVolume(masterVolume);
        fireVolumeChanged();
    }

    /** Returns the current per-channel volume multiplier. */
    public double channelVolume(String channelId) {
        assertInitialized();
        requireChannel(channelId);
        return channelVolumes.get(channelId);
    }

    /** Updates a channel volume multiplier without changing user preferences. */
    public void setChannelVolume(String channelId, double volume) {
        assertInitialized();
        requireChannel(channelId);
        channelVolumes.put(channelId, clampVolume(volume));
        fireVolumeChanged();
    }

    /** Mutes future playback commands without forgetting configured volumes. */
    public void setMuted(boolean muted) {
        assertInitialized();
        this.muted = muted;
        fireVolumeChanged();
    }

    /**
     * Registers a listener fired whenever the master volume, a channel volume, or the mute state
     * changes. Lets a host that drives its own media players (outside the command/adapter path —
     * e.g. a looping-music player) re-apply the effective volume immediately when the user adjusts
     * the sound settings. Listeners run on the calling thread (the JavaFX thread for UI changes).
     */
    public void addVolumeChangeListener(Runnable listener) {
        if (listener != null) {
            volumeChangeListeners.add(listener);
        }
    }

    private void fireVolumeChanged() {
        for (Runnable listener : volumeChangeListeners) {
            try {
                listener.run();
            } catch (RuntimeException ignored) {
                // A misbehaving listener must not break volume updates for the others.
            }
        }
    }

    /** Returns whether playback commands should resolve to silent effective volume. */
    public boolean muted() {
        assertInitialized();
        return muted;
    }

    /**
     * Validates a request and returns the command a JavaFX media adapter should execute.
     *
     * @param request authored asset request with channel, loop flag, and relative volume
     * @return command containing the effective master/channel/request volume product
     * @throws IllegalStateException when uninitialized or the channel is unknown
     * @throws IllegalArgumentException when looping is requested on a non-looping channel
     */
    public AudioPlaybackCommand play(SoundRequest request) {
        assertInitialized();
        requireChannel(request.channelId());
        AudioChannelDefinition channel = channels.get(request.channelId());
        if (request.loop() && !channel.loopingAllowed()) {
            throw new IllegalArgumentException("Audio channel does not allow looping: " + request.channelId());
        }
        double effectiveVolume = muted
                ? 0.0
                : masterVolume * channelVolumes.get(request.channelId()) * request.relativeVolume();
        AudioPlaybackCommand command = new AudioPlaybackCommand(
                request.channelId(),
                request.sourcePath(),
                request.loop(),
                effectiveVolume);
        lastPlaybackCommands.put(request.channelId(), command);
        return command;
    }

    /** Clears the last command for a channel and cancels any pending queued request. */
    public void stopChannel(String channelId) {
        assertInitialized();
        requireChannel(channelId);
        lastPlaybackCommands.remove(channelId);
        queuedRequests.remove(channelId);
    }

    /**
     * Schedules a track to play after the current playback on the same channel ends naturally.
     * Calling again replaces the pending entry — single-depth queue matching Ren'Py semantics.
     *
     * @param request the sound request to queue; its channel must already be registered
     */
    public void queueMusic(SoundRequest request) {
        assertInitialized();
        requireChannel(request.channelId());
        queuedRequests.put(request.channelId(), request);
    }

    /** Cancels any pending queued request for the given channel. */
    public void clearQueue(String channelId) {
        assertInitialized();
        requireChannel(channelId);
        queuedRequests.remove(channelId);
    }

    /** Returns the pending queued request for a channel, if one has been scheduled. */
    public Optional<SoundRequest> queuedRequest(String channelId) {
        assertInitialized();
        requireChannel(channelId);
        return Optional.ofNullable(queuedRequests.get(channelId));
    }

    /**
     * Returns the registered priority and ducking configuration for a channel.
     * Channels with no registered config return a default with priority 0 and {@link DuckingPolicy#NONE}.
     *
     * @param channelId registered audio channel ID
     */
    public AudioChannelConfig channelConfig(String channelId) {
        assertInitialized();
        requireChannel(channelId);
        return channelConfigs.getOrDefault(channelId,
                new AudioChannelConfig(channelId, 0, 1.0, DuckingPolicy.NONE, 0.0));
    }

    /** Returns the last validated command for a channel if one exists. */
    public Optional<AudioPlaybackCommand> lastPlaybackCommand(String channelId) {
        assertInitialized();
        requireChannel(channelId);
        return Optional.ofNullable(lastPlaybackCommands.get(channelId));
    }

    private void assertInitialized() {
        initializationGuard.requireInitialized();
    }

    private void requireChannel(String channelId) {
        if (!channels.containsKey(channelId)) {
            throw new IllegalStateException("Missing audio channel: " + channelId);
        }
    }

    private double clampVolume(double volume) {
        return Validation.clampUnitInterval(volume);
    }
}
