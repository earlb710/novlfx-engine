package com.eb.javafx.bootstrap;

import com.eb.javafx.audio.AudioChannelConfig;
import com.eb.javafx.audio.DuckingPolicy;
import com.eb.javafx.util.ConfigValues;

import java.util.Locale;

/**
 * Builds {@link AudioChannelConfig}s from an {@link ApplicationResourceConfig}, applying any
 * {@code audioChannels.<id>} overrides ({@code priority} / {@code volume} / {@code ducking} /
 * {@code duckPercent}) over caller-supplied defaults.
 *
 * <p>Generic bootstrap plumbing: <em>which</em> channels a game registers (and their default
 * priorities/ducking) is game policy and stays in the host, but the per-channel config-merge is
 * identical for every game, so it lives here.</p>
 */
public final class AudioChannelConfigs {

    private AudioChannelConfigs() {
    }

    /**
     * Builds a channel config, overlaying {@code config.json}'s {@code audioChannels.<channelId>}
     * fields (if present) over the supplied defaults. A {@code null} config returns the defaults.
     */
    public static AudioChannelConfig fromConfig(ApplicationResourceConfig config, String channelId,
            int defaultPriority, double defaultVolume,
            DuckingPolicy defaultPolicy, double defaultDuckPercent) {
        int priority = defaultPriority;
        double volume = defaultVolume;
        DuckingPolicy policy = defaultPolicy;
        double duckPercent = defaultDuckPercent;
        if (config != null) {
            priority = ConfigValues.parseIntOr(
                    config.audioChannelField(channelId, "priority").orElse(null), priority);
            volume = ConfigValues.clampUnit(ConfigValues.parseDoubleOr(
                    config.audioChannelField(channelId, "volume").orElse(null), volume));
            policy = parseDuckingPolicyOr(config.audioChannelField(channelId, "ducking").orElse(null), policy);
            duckPercent = ConfigValues.clampUnit(ConfigValues.parseDoubleOr(
                    config.audioChannelField(channelId, "duckPercent").orElse(null), duckPercent));
        }
        return new AudioChannelConfig(channelId, priority, volume, policy, duckPercent);
    }

    /** Audio-specific enum parse (case-insensitive, hyphen→underscore); unknown falls back. */
    static DuckingPolicy parseDuckingPolicyOr(String value, DuckingPolicy fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return DuckingPolicy.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            System.err.println("[AudioChannelConfigs] Unknown ducking policy '" + value
                    + "', using " + fallback);
            return fallback;
        }
    }
}
