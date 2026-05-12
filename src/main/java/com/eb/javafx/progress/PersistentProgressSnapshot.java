package com.eb.javafx.progress;

import java.util.Map;
import java.util.Set;

public record PersistentProgressSnapshot(
        Set<String> flags,
        Map<String, Integer> counters,
        Set<String> milestones,
        Set<String> unlocks) {
    public PersistentProgressSnapshot {
        flags = Set.copyOf(flags);
        counters = Map.copyOf(counters);
        milestones = Set.copyOf(milestones);
        unlocks = Set.copyOf(unlocks);
    }
}
