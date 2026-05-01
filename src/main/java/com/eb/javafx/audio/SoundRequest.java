package com.eb.javafx.audio;

/**
 * Immutable request to play an authored sound or music asset on a named channel.
 */
public final class SoundRequest {
    private final String channelId;
    private final String sourcePath;
    private final boolean loop;
    private final double relativeVolume;

    public SoundRequest(String channelId, String sourcePath, boolean loop, double relativeVolume) {
        if (channelId == null || channelId.isBlank()) {
            throw new IllegalArgumentException("Sound request channel id is required.");
        }
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalArgumentException("Sound request source path is required.");
        }
        if (relativeVolume < 0.0 || relativeVolume > 1.0) {
            throw new IllegalArgumentException("Sound request relative volume must be between 0 and 1.");
        }
        this.channelId = channelId;
        this.sourcePath = sourcePath;
        this.loop = loop;
        this.relativeVolume = relativeVolume;
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

    public double relativeVolume() {
        return relativeVolume;
    }
}
