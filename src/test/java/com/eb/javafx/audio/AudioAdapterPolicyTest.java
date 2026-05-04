package com.eb.javafx.audio;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AudioAdapterPolicyTest {
    @Test
    void policyCapturesAdapterExpectationsWithoutOwningAssets() {
        AudioAdapterPolicy policy = new AudioAdapterPolicy(
                source -> Optional.of(URI.create("file:///game/" + source)),
                Map.of(AudioService.MUSIC_CHANNEL, 1),
                Duration.ofMillis(250),
                true,
                List.of(AudioPlaybackLifecycleEvent.PLAY, AudioPlaybackLifecycleEvent.CROSSFADE));

        assertTrue(policy.assetResolver().resolve("theme.ogg").isPresent());
        assertEquals(1, policy.playerPoolSizes().get(AudioService.MUSIC_CHANNEL));
        assertEquals(Duration.ofMillis(250), policy.defaultFadeDuration());
    }

    @Test
    void fadeRequestsRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class, () ->
                new AudioFadeRequest("music", 1.5, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () ->
                new AudioFadeRequest("music", 0.5, Duration.ofMillis(-1)));
    }
}
