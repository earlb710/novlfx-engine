package com.eb.javafx.progress;

import com.eb.javafx.util.Validation;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Persistent progress tracker backed by {@link java.util.prefs.Preferences}.
 *
 * <p>Tracks flags, counters, milestones, and unlocks that survive application restarts.
 * State is written through immediately on each mutation so no explicit save step is required.</p>
 */
public final class PersistentProgressTracker {
    private static final String FLAGS_NODE = "flags";
    private static final String COUNTERS_NODE = "counters";
    private static final String MILESTONES_NODE = "milestones";
    private static final String UNLOCKS_NODE = "unlocks";

    private final Preferences root;

    /**
     * Creates a tracker rooted at the given namespace in the user preferences tree.
     *
     * @param gameNamespace slash-separated path (e.g. {@code "com/example/mygame"}); must not be blank
     */
    public PersistentProgressTracker(String gameNamespace) {
        Validation.requireNonBlank(gameNamespace, "gameNamespace");
        this.root = Preferences.userRoot().node(gameNamespace);
    }

    /** Sets or removes a boolean flag. A {@code false} value removes the key entirely. */
    public void setFlag(String flagId, boolean enabled) {
        Validation.requireNonBlank(flagId, "flagId");
        if (enabled) {
            root.node(FLAGS_NODE).putBoolean(flagId, true);
        } else {
            root.node(FLAGS_NODE).remove(flagId);
        }
        flush();
    }

    /** Returns {@code true} if the flag is set. */
    public boolean hasFlag(String flagId) {
        return root.node(FLAGS_NODE).getBoolean(
                Validation.requireNonBlank(flagId, "flagId"), false);
    }

    /** Returns the current counter value, or {@code 0} if not yet set. */
    public int counter(String counterId) {
        return root.node(COUNTERS_NODE).getInt(
                Validation.requireNonBlank(counterId, "counterId"), 0);
    }

    /** Increments the counter by {@code delta} and returns the updated value. */
    public int incrementCounter(String counterId, int delta) {
        Preferences node = root.node(COUNTERS_NODE);
        int updated = node.getInt(Validation.requireNonBlank(counterId, "counterId"), 0) + delta;
        node.putInt(counterId, updated);
        flush();
        return updated;
    }

    /** Marks a milestone as completed. */
    public void completeMilestone(String milestoneId) {
        root.node(MILESTONES_NODE).putBoolean(
                Validation.requireNonBlank(milestoneId, "milestoneId"), true);
        flush();
    }

    /** Returns {@code true} if the milestone has been completed. */
    public boolean hasMilestone(String milestoneId) {
        return root.node(MILESTONES_NODE).getBoolean(
                Validation.requireNonBlank(milestoneId, "milestoneId"), false);
    }

    /** Marks a gallery item or collectible as unlocked. */
    public void unlock(String unlockId) {
        root.node(UNLOCKS_NODE).putBoolean(
                Validation.requireNonBlank(unlockId, "unlockId"), true);
        flush();
    }

    /** Returns {@code true} if the item has been unlocked. */
    public boolean isUnlocked(String unlockId) {
        return root.node(UNLOCKS_NODE).getBoolean(
                Validation.requireNonBlank(unlockId, "unlockId"), false);
    }

    /** Returns an immutable snapshot of the current persistent progress state. */
    public PersistentProgressSnapshot snapshot() {
        return new PersistentProgressSnapshot(
                readBooleanKeys(FLAGS_NODE),
                readCounters(),
                readBooleanKeys(MILESTONES_NODE),
                readBooleanKeys(UNLOCKS_NODE));
    }

    /**
     * Overwrites all persistent progress with the given snapshot.
     * All existing flags, counters, milestones, and unlocks are cleared first.
     */
    public void restore(PersistentProgressSnapshot snapshot) {
        Validation.requireNonNull(snapshot, "snapshot");
        clearAndWriteBooleans(FLAGS_NODE, snapshot.flags());
        clearAndWriteCounters(snapshot.counters());
        clearAndWriteBooleans(MILESTONES_NODE, snapshot.milestones());
        clearAndWriteBooleans(UNLOCKS_NODE, snapshot.unlocks());
        flush();
    }

    private Set<String> readBooleanKeys(String nodeName) {
        try {
            Preferences node = root.node(nodeName);
            Set<String> keys = new LinkedHashSet<>();
            for (String key : node.keys()) {
                if (node.getBoolean(key, false)) keys.add(key);
            }
            return keys;
        } catch (BackingStoreException e) {
            return Set.of();
        }
    }

    private Map<String, Integer> readCounters() {
        try {
            Preferences node = root.node(COUNTERS_NODE);
            Map<String, Integer> counters = new LinkedHashMap<>();
            for (String key : node.keys()) {
                counters.put(key, node.getInt(key, 0));
            }
            return counters;
        } catch (BackingStoreException e) {
            return Map.of();
        }
    }

    private void clearAndWriteBooleans(String nodeName, Set<String> keys) {
        try {
            Preferences node = root.node(nodeName);
            node.clear();
            for (String key : keys) {
                node.putBoolean(key, true);
            }
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to clear persistent progress node: " + nodeName, e);
        }
    }

    private void clearAndWriteCounters(Map<String, Integer> counters) {
        try {
            Preferences node = root.node(COUNTERS_NODE);
            node.clear();
            counters.forEach(node::putInt);
        } catch (BackingStoreException e) {
            throw new IllegalStateException("Failed to clear persistent progress counters.", e);
        }
    }

    private void flush() {
        try {
            root.flush();
        } catch (BackingStoreException e) {
            // Non-fatal: OS-level write failure; data may still be in memory.
        }
    }
}
