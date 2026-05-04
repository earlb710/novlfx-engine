package com.eb.javafx.audio;

import com.eb.javafx.util.Validation;

import java.time.Duration;

/** Rendering-neutral crossfade intent between the current channel media and a new command. */
public record AudioCrossfadeRequest(String channelId, AudioPlaybackCommand nextCommand, Duration duration) {
    public AudioCrossfadeRequest {
        channelId = Validation.requireNonBlank(channelId, "Audio crossfade channel id is required.");
        nextCommand = Validation.requireNonNull(nextCommand, "Audio crossfade next command is required.");
        duration = Validation.requireNonNull(duration, "Audio crossfade duration is required.");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Audio crossfade duration must not be negative.");
        }
    }
}
