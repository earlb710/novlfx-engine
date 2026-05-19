package com.eb.javafx.storyline;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Mutable runtime state tracking which {@link StorylineEvent storyline events} have fired, what
 * status they resolved to, and how many times they've fired.
 *
 * <p>Designed for save/load: {@link #snapshot()} produces an immutable record, {@link #restore}
 * loads one back. Repeatable events accumulate a completion count and always overwrite their
 * recorded status with the most recent value; once-off events record their status exactly once.</p>
 *
 * <p>Flag storage piggybacks on the runtime so requirements that need a free-form "this happened"
 * marker (e.g. cutscene watched, item picked up) don't have to invent a parallel ProgressTracker.
 * Consumers that already own a {@code ProgressTracker} can ignore the flag API here.</p>
 */
public final class StorylineRuntime {
    private final LinkedHashMap<String, String> statusById = new LinkedHashMap<>();
    private final LinkedHashMap<String, Integer> completionCounts = new LinkedHashMap<>();
    private final LinkedHashSet<String> flags = new LinkedHashSet<>();

    /** Returns {@code true} when the event has been completed at least once. */
    public boolean isCompleted(String eventId) {
        return completionCounts.getOrDefault(Validation.requireNonBlank(eventId, "eventId is required."), 0) > 0;
    }

    /** Number of times the event has fired (0 if never). */
    public int completionCount(String eventId) {
        return completionCounts.getOrDefault(Validation.requireNonBlank(eventId, "eventId is required."), 0);
    }

    /** Most recent status recorded for the event (or empty if never fired). */
    public Optional<String> statusOf(String eventId) {
        return Optional.ofNullable(statusById.get(Validation.requireNonBlank(eventId, "eventId is required.")));
    }

    /** Returns {@code true} when {@code flag} is set on the runtime. */
    public boolean hasFlag(String flag) {
        return flags.contains(Validation.requireNonBlank(flag, "flag is required."));
    }

    /** Records an event completion with the supplied status; bumps the completion counter. */
    public void recordCompletion(String eventId, String status) {
        String checkedEvent = Validation.requireNonBlank(eventId, "eventId is required.");
        String checkedStatus = EventStatus.require(status);
        statusById.put(checkedEvent, checkedStatus);
        completionCounts.merge(checkedEvent, 1, Integer::sum);
    }

    public void setFlag(String flag) {
        flags.add(Validation.requireNonBlank(flag, "flag is required."));
    }

    public void clearFlag(String flag) {
        flags.remove(Validation.requireNonBlank(flag, "flag is required."));
    }

    /** Resets all state. Tests and new-game flows. */
    public void reset() {
        statusById.clear();
        completionCounts.clear();
        flags.clear();
    }

    /** Captures an immutable snapshot suitable for save/load. */
    public Snapshot snapshot() {
        return new Snapshot(Map.copyOf(statusById), Map.copyOf(completionCounts), Set.copyOf(flags));
    }

    /** Replaces all state with the snapshot's contents. */
    public void restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot is required.");
        reset();
        statusById.putAll(snapshot.statusById());
        completionCounts.putAll(snapshot.completionCounts());
        flags.addAll(snapshot.flags());
    }

    /** Immutable view of {@link StorylineRuntime}. Round-trips through {@link #snapshot()} / {@link #restore}. */
    public record Snapshot(Map<String, String> statusById, Map<String, Integer> completionCounts, Set<String> flags) {
        public Snapshot {
            statusById = Map.copyOf(Objects.requireNonNull(statusById, "statusById is required."));
            completionCounts = Map.copyOf(Objects.requireNonNull(completionCounts, "completionCounts is required."));
            flags = Set.copyOf(Objects.requireNonNull(flags, "flags is required."));
        }

        public static Snapshot empty() {
            return new Snapshot(Map.of(), Map.of(), Set.of());
        }
    }
}
