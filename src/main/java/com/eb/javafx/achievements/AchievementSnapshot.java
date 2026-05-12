package com.eb.javafx.achievements;

import java.util.Set;

/** Immutable snapshot of unlocked achievement ids for save/restore. */
public record AchievementSnapshot(Set<String> unlockedIds) {
    public AchievementSnapshot {
        unlockedIds = Set.copyOf(unlockedIds);
    }
}
