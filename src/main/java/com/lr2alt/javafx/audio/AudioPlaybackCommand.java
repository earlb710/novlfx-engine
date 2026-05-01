package com.lr2alt.javafx.audio;

/**
 * Validated playback command produced before a JavaFX media player is created.
 *
 * <p>This keeps channel and volume validation testable in headless builds while
 * leaving the final {@code MediaPlayer}/{@code AudioClip} binding as a narrow
 * adapter step for later migrated screens.</p>
 */
public final class AudioPlaybackCommand {
    private final String channelId;
    private final String sourcePath;
    private final boolean loop;
    private final double effectiveVolume;

    AudioPlaybackCommand(String channelId, String sourcePath, boolean loop, double effectiveVolume) {
        this.channelId = channelId;
        this.sourcePath = sourcePath;
        this.loop = loop;
        this.effectiveVolume = effectiveVolume;
    }

    public String channelId() {
        return channelId;
    }

    public String sourcePath() {
        return sourcePath;
    }

    public boolean loop() {
        return loop;
    }

    public double effectiveVolume() {
        return effectiveVolume;
    }
}
