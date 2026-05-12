package com.eb.javafx.audio;

import com.eb.javafx.util.Validation;

/**
 * Priority and ducking configuration for a named audio channel.
 *
 * <p>Channels with higher priority reduce or pause lower-priority channels when they start.
 * Channels not explicitly registered use a default config with priority 0 and {@link DuckingPolicy#NONE}.
 * The engine stores configs; adapters apply the volume changes at play time.</p>
 */
public final class AudioChannelConfig {
    private final String channelId;
    private final int priority;
    private final double defaultVolume;
    private final DuckingPolicy duckingPolicy;
    private final double duckingPercent;

    /**
     * Creates a channel priority and ducking configuration.
     *
     * @param channelId the channel this config applies to
     * @param priority higher values indicate more important channels
     * @param defaultVolume startup volume multiplier from 0.0 to 1.0
     * @param duckingPolicy how lower-priority channels are affected when this one plays
     * @param duckingPercent target volume for REDUCE_TO_PERCENT policy; ignored for other policies
     */
    public AudioChannelConfig(String channelId, int priority, double defaultVolume,
                              DuckingPolicy duckingPolicy, double duckingPercent) {
        this.channelId = Validation.requireNonBlank(channelId, "Channel id is required.");
        this.priority = priority;
        this.defaultVolume = Validation.requireUnitInterval(defaultVolume,
                "Channel config default volume must be between 0 and 1.");
        this.duckingPolicy = Validation.requireNonNull(duckingPolicy, "Ducking policy is required.");
        this.duckingPercent = duckingPolicy == DuckingPolicy.REDUCE_TO_PERCENT
                ? Validation.requireUnitInterval(duckingPercent,
                        "Ducking percent must be between 0 and 1.")
                : 0.0;
    }

    public String channelId() { return channelId; }
    public int priority() { return priority; }
    public double defaultVolume() { return defaultVolume; }
    public DuckingPolicy duckingPolicy() { return duckingPolicy; }
    public double duckingPercent() { return duckingPercent; }
}
