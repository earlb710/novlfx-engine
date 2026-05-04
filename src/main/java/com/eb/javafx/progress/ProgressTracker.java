package com.eb.javafx.progress;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Mutable generic progress model for flags, counters, milestones, achievements, and unlocks. */
public final class ProgressTracker {
    private final Set<String> flags = new LinkedHashSet<>();
    private final Map<String, Integer> counters = new LinkedHashMap<>();
    private final Set<String> milestones = new LinkedHashSet<>();
    private final Set<String> unlocks = new LinkedHashSet<>();

    public void setFlag(String flagId, boolean enabled) {
        String checkedFlagId = Validation.requireNonBlank(flagId, "Progress flag id is required.");
        if (enabled) {
            flags.add(checkedFlagId);
        } else {
            flags.remove(checkedFlagId);
        }
    }

    public boolean hasFlag(String flagId) {
        return flags.contains(flagId);
    }

    public int counter(String counterId) {
        return counters.getOrDefault(counterId, 0);
    }

    public int incrementCounter(String counterId, int delta) {
        String checkedCounterId = Validation.requireNonBlank(counterId, "Progress counter id is required.");
        int updated = counter(checkedCounterId) + delta;
        counters.put(checkedCounterId, updated);
        return updated;
    }

    public void completeMilestone(String milestoneId) {
        milestones.add(Validation.requireNonBlank(milestoneId, "Progress milestone id is required."));
    }

    public boolean hasMilestone(String milestoneId) {
        return milestones.contains(milestoneId);
    }

    public void unlock(String unlockId) {
        unlocks.add(Validation.requireNonBlank(unlockId, "Progress unlock id is required."));
    }

    public boolean isUnlocked(String unlockId) {
        return unlocks.contains(unlockId);
    }

    public ProgressSnapshot snapshot() {
        return new ProgressSnapshot(flags, counters, milestones, unlocks);
    }

    public void restore(ProgressSnapshot snapshot) {
        ProgressSnapshot checkedSnapshot = Validation.requireNonNull(snapshot, "Progress snapshot is required.");
        flags.clear();
        flags.addAll(checkedSnapshot.flags());
        counters.clear();
        counters.putAll(checkedSnapshot.counters());
        milestones.clear();
        milestones.addAll(checkedSnapshot.milestones());
        unlocks.clear();
        unlocks.addAll(checkedSnapshot.unlocks());
    }
}
