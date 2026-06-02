package com.eb.javafx.bootstrap;

import com.eb.javafx.audio.AudioChannelConfig;
import com.eb.javafx.audio.DuckingPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AudioChannelConfigsTest {

    @Test
    void nullConfigReturnsDefaults() {
        AudioChannelConfig channel = AudioChannelConfigs.fromConfig(
                null, "voice", 10, 1.0, DuckingPolicy.REDUCE_TO_PERCENT, 0.3);

        assertEquals("voice", channel.channelId());
        assertEquals(10, channel.priority());
        assertEquals(1.0, channel.defaultVolume());
        assertEquals(DuckingPolicy.REDUCE_TO_PERCENT, channel.duckingPolicy());
        assertEquals(0.3, channel.duckingPercent());
    }

    @Test
    void configOverridesAreAppliedOverDefaults() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson("""
                {
                  "audioChannels": {
                    "music": { "priority": 7, "volume": 0.5, "ducking": "reduce-to-percent", "duckPercent": 0.4 }
                  }
                }
                """, "inline");

        AudioChannelConfig channel = AudioChannelConfigs.fromConfig(
                config, "music", 5, 1.0, DuckingPolicy.NONE, 0.0);

        assertEquals(7, channel.priority());
        assertEquals(0.5, channel.defaultVolume());
        assertEquals(DuckingPolicy.REDUCE_TO_PERCENT, channel.duckingPolicy());
        assertEquals(0.4, channel.duckingPercent());
    }

    @Test
    void unsetChannelKeepsDefaults() {
        ApplicationResourceConfig config = ApplicationResourceConfig.fromJson(
                "{ \"audioChannels\": { \"music\": { \"priority\": 7 } } }", "inline");

        // "sound" has no override block → defaults preserved.
        AudioChannelConfig channel = AudioChannelConfigs.fromConfig(
                config, "sound", 3, 1.0, DuckingPolicy.NONE, 0.0);

        assertEquals(3, channel.priority());
        assertEquals(1.0, channel.defaultVolume());
        assertEquals(DuckingPolicy.NONE, channel.duckingPolicy());
    }

    @Test
    void duckingPolicyParseIsCaseInsensitiveAndFallsBack() {
        // Case-insensitive + hyphen normalisation; unknown falls back.
        assertEquals(DuckingPolicy.REDUCE_TO_PERCENT,
                AudioChannelConfigs.parseDuckingPolicyOr("reduce-to-percent", DuckingPolicy.NONE));
        assertEquals(DuckingPolicy.PAUSE,
                AudioChannelConfigs.parseDuckingPolicyOr("pause", DuckingPolicy.NONE));
        assertEquals(DuckingPolicy.NONE,
                AudioChannelConfigs.parseDuckingPolicyOr("bogus", DuckingPolicy.NONE));
        assertEquals(DuckingPolicy.NONE,
                AudioChannelConfigs.parseDuckingPolicyOr(null, DuckingPolicy.NONE));
    }
}
