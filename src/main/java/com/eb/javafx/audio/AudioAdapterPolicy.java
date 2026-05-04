package com.eb.javafx.audio;

import com.eb.javafx.util.Validation;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Content-neutral expectations for concrete JavaFX media adapter implementations. */
public record AudioAdapterPolicy(
        AudioAssetResolver assetResolver,
        Map<String, Integer> playerPoolSizes,
        Duration defaultFadeDuration,
        boolean preloadEnabled,
        List<AudioPlaybackLifecycleEvent> supportedEvents) {
    public AudioAdapterPolicy {
        assetResolver = Validation.requireNonNull(assetResolver, "Audio asset resolver is required.");
        playerPoolSizes = Map.copyOf(Validation.requireNonNull(playerPoolSizes, "Audio player pool sizes are required."));
        defaultFadeDuration = Validation.requireNonNull(defaultFadeDuration, "Default fade duration is required.");
        if (defaultFadeDuration.isNegative()) {
            throw new IllegalArgumentException("Default fade duration must not be negative.");
        }
        supportedEvents = List.copyOf(Validation.requireNonNull(supportedEvents, "Supported audio lifecycle events are required."));
    }
}
