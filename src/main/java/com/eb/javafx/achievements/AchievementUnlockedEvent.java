package com.eb.javafx.achievements;

import com.eb.javafx.events.GameEvent;

import java.util.Map;

/** Factory for ACHIEVEMENT_UNLOCKED game events. */
public final class AchievementUnlockedEvent {
    public static final String EVENT_TYPE = "ACHIEVEMENT_UNLOCKED";

    public static GameEvent create(String achievementId) {
        return GameEvent.now(EVENT_TYPE, achievementId, Map.of("achievementId", achievementId));
    }

    private AchievementUnlockedEvent() {}
}
