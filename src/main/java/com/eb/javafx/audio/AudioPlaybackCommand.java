package com.eb.javafx.audio;

/**
 * Validated playback command produced before a JavaFX media player is created.
 *
 * <p>This keeps channel and volume validation testable in headless builds while
 * leaving the final {@code MediaPlayer}/{@code AudioClip} binding as a narrow
 * adapter step for later migrated screens. Instances are package-created by
 * {@link AudioService} after channel, loop, and effective-volume validation.</p>
 */
public final class AudioPlaybackCommand {
    private final String channelId;
    private final String sourcePath;
    private final boolean loop;
    private final double effectiveVolume;

    /**
     * Creates a command from validated playback inputs.
     *
     * @param channelId registered channel that should own playback
     * @param sourcePath authored audio asset path for the media adapter
     * @param loop whether the adapter should request looping playback
     * @param effectiveVolume combined master, channel, request, and mute volume
     */
    AudioPlaybackCommand(String channelId, String sourcePath, boolean loop, double effectiveVolume) {
        this.channelId = channelId;
        this.sourcePath = sourcePath;
        this.loop = loop;
        this.effectiveVolume = effectiveVolume;
    }

    /** Returns the registered channel that should execute this command. */
    public String channelId() {
        return channelId;
    }

    /** Returns the authored audio asset path for the future JavaFX media adapter. */
    public String sourcePath() {
        return sourcePath;
    }

    /** Returns whether playback should loop when the channel allows it. */
    public boolean loop() {
        return loop;
    }

    /** Returns the final effective volume the media adapter should apply. */
    public double effectiveVolume() {
        return effectiveVolume;
    }
}
