package com.eb.javafx.progress;

import com.eb.javafx.util.ImmutableCollections;

import java.util.Map;
import java.util.Set;

/** Immutable snapshot of generic flags, counters, milestones, and unlocks. */
public record ProgressSnapshot(Set<String> flags, Map<String, Integer> counters, Set<String> milestones, Set<String> unlocks) {
    public ProgressSnapshot {
        flags = flags == null || flags.isEmpty() ? Set.of() : Set.copyOf(flags);
        counters = ImmutableCollections.copyMap(counters);
        milestones = milestones == null || milestones.isEmpty() ? Set.of() : Set.copyOf(milestones);
        unlocks = unlocks == null || unlocks.isEmpty() ? Set.of() : Set.copyOf(unlocks);
    }
}
