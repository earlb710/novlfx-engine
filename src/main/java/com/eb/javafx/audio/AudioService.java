package com.eb.javafx.audio;

import com.eb.javafx.prefs.PreferencesService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Section 1.5 audio/media boundary for migrated JavaFX systems.
 *
 * <p>Ren'Py exposes global music and sound helpers. This service replaces that
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

    private final Map<String, AudioChannelDefinition> channels = new LinkedHashMap<>();
    private final Map<String, Double> channelVolumes = new LinkedHashMap<>();
    private final Map<String, AudioPlaybackCommand> lastPlaybackCommands = new LinkedHashMap<>();
    private boolean initialized;
    private boolean muted;
    private double masterVolume;

    /**
     * Registers default channel names for the audio system and loads master volume.
     *
     * @param preferencesService loaded preferences supplying persisted audio settings
     * @throws IllegalArgumentException when preferences are unavailable
     */
    public void initialize(PreferencesService preferencesService) {
        if (preferencesService == null) {
            throw new IllegalArgumentException("Preferences service is required for audio initialization.");
        }
        channels.clear();
        channelVolumes.clear();
        lastPlaybackCommands.clear();
        masterVolume = preferencesService.masterVolume();
        muted = false;
        registerChannel(new AudioChannelDefinition(MUSIC_CHANNEL, "Looping background music.", true, 1, 1.0));
        registerChannel(new AudioChannelDefinition(SOUND_CHANNEL, "Short one-shot UI and gameplay sounds.", false, 8, 1.0));
        registerChannel(new AudioChannelDefinition(EFFECTS_CHANNEL, "Reusable environmental and scene effects.", true, 4, 1.0));
        registerChannel(new AudioChannelDefinition(INTIMATE_EFFECTS_CHANNEL, "Dedicated migrated effect channel.", true, 2, 1.0));
        initialized = true;
    }

    /** Returns whether startup has prepared the audio boundary. */
    public boolean isInitialized() {
        return initialized;
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
    }

    /** Mutes future playback commands without forgetting configured volumes. */
    public void setMuted(boolean muted) {
        assertInitialized();
        this.muted = muted;
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

    /** Clears the last command for a channel to model a stop request. */
    public void stopChannel(String channelId) {
        assertInitialized();
        requireChannel(channelId);
        lastPlaybackCommands.remove(channelId);
    }

    /** Returns the last validated command for a channel if one exists. */
    public Optional<AudioPlaybackCommand> lastPlaybackCommand(String channelId) {
        assertInitialized();
        requireChannel(channelId);
        return Optional.ofNullable(lastPlaybackCommands.get(channelId));
    }

    private void assertInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Audio service used before initialization.");
        }
    }

    private void requireChannel(String channelId) {
        if (!channels.containsKey(channelId)) {
            throw new IllegalStateException("Missing audio channel: " + channelId);
        }
    }

    private double clampVolume(double volume) {
        return Math.max(0.0, Math.min(1.0, volume));
    }
}
