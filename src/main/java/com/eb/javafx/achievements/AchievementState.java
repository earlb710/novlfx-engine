package com.eb.javafx.achievements;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashSet;
import java.util.Set;

/** Mutable runtime state tracking which achievements have been unlocked. */
public final class AchievementState {
    private final Set<String> unlockedIds = new LinkedHashSet<>();

    public void unlock(String id) {
        unlockedIds.add(Validation.requireNonBlank(id, "id"));
    }

    public boolean isUnlocked(String id) {
        return unlockedIds.contains(id);
    }

    public Set<String> unlockedIds() {
        return Set.copyOf(unlockedIds);
    }

    public AchievementSnapshot snapshot() {
        return new AchievementSnapshot(unlockedIds);
    }

    public void restore(AchievementSnapshot snapshot) {
        Validation.requireNonNull(snapshot, "snapshot");
        unlockedIds.clear();
        unlockedIds.addAll(snapshot.unlockedIds());
    }
}
