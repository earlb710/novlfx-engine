package com.eb.javafx.timeline;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.Map;

/** One generic timed step for sequencing UI, display, text, or audio behavior. */
public record TimelineStep(String id, long durationMillis, Map<String, String> metadata) {
    public TimelineStep {
        id = Validation.requireNonBlank(id, "Timeline step id is required.");
        durationMillis = Validation.requireZeroOrPositive(durationMillis, "Timeline step duration must not be negative.");
        metadata = ImmutableCollections.copyMap(metadata);
    }
}
