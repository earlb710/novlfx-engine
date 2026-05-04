package com.eb.javafx.timeline;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;

/** Ordered reusable timeline sequence. */
public record TimelineSequence(String id, List<TimelineStep> steps) {
    public TimelineSequence {
        id = Validation.requireNonBlank(id, "Timeline id is required.");
        steps = ImmutableCollections.copyList(steps);
    }

    public long totalDurationMillis() {
        return steps.stream().mapToLong(TimelineStep::durationMillis).sum();
    }
}
