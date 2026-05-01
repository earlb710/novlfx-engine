package com.eb.javafx.audio;

/**
 * Immutable request to play an authored sound or music asset on a named channel.
 *
 * <p>The channel ID must match a registered {@link AudioChannelDefinition}, the
 * source path is an authored resource path, and relative volume is a per-request
 * multiplier in the inclusive {@code 0.0..1.0} range.</p>
 */
public final class SoundRequest {
    private final String channelId;
    private final String sourcePath;
    private final boolean loop;
    private final double relativeVolume;

    /**
     * Creates a validated sound or music playback request.
     *
     * @param channelId registered audio channel ID
     * @param sourcePath non-blank authored asset path
     * @param loop whether the request needs looping playback
     * @param relativeVolume per-request multiplier from {@code 0.0} to {@code 1.0}
     */
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
