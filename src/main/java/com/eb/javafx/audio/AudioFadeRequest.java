package com.eb.javafx.audio;

import com.eb.javafx.util.Validation;

import java.time.Duration;

/** Rendering-neutral fade intent for concrete audio adapters. */
public record AudioFadeRequest(String channelId, double targetVolume, Duration duration) {
    public AudioFadeRequest {
        channelId = Validation.requireNonBlank(channelId, "Audio fade channel id is required.");
        targetVolume = Validation.requireUnitInterval(targetVolume, "Audio fade target volume must be between 0 and 1.");
        duration = Validation.requireNonNull(duration, "Audio fade duration is required.");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Audio fade duration must not be negative.");
        }
    }
}
