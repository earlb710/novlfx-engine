package com.eb.javafx.dialog;

import java.util.Objects;
import java.util.Optional;

/**
 * Snapshot of the world state a {@code DialogRequirement} reads when deciding whether a
 * {@link DialogChain} is currently eligible.
 *
 * <p>Fields are optional. A null/blank value means "not currently observed"; matching requirements
 * will fail. Use {@link #empty()} when no context is available (most tests, very early game start).</p>
 */
public record StoryContext(
        String currentLocationId,
        int gameDay,
        String timeOfDay,
        String activeCharacterRoleId,
        String lastTriggerId) {

    public static final int UNKNOWN_DAY = 0;

    public StoryContext {
        currentLocationId = trim(currentLocationId);
        timeOfDay = trim(timeOfDay);
        activeCharacterRoleId = trim(activeCharacterRoleId);
        lastTriggerId = trim(lastTriggerId);
        if (gameDay < 0) {
            throw new IllegalArgumentException("gameDay must be >= 0. Got: " + gameDay);
        }
    }

    public static StoryContext empty() {
        return new StoryContext(null, UNKNOWN_DAY, null, null, null);
    }

    public Optional<String> location() {
        return Optional.ofNullable(currentLocationId);
    }

    public Optional<String> period() {
        return Optional.ofNullable(timeOfDay);
    }

    public Optional<String> activeCharacter() {
        return Optional.ofNullable(activeCharacterRoleId);
    }

    public Optional<String> trigger() {
        return Optional.ofNullable(lastTriggerId);
    }

    public StoryContext withLocation(String locationId) {
        return new StoryContext(locationId, gameDay, timeOfDay, activeCharacterRoleId, lastTriggerId);
    }

    public StoryContext withDay(int day) {
        return new StoryContext(currentLocationId, day, timeOfDay, activeCharacterRoleId, lastTriggerId);
    }

    public StoryContext withTimeOfDay(String period) {
        return new StoryContext(currentLocationId, gameDay, period, activeCharacterRoleId, lastTriggerId);
    }

    public StoryContext withActiveCharacter(String roleId) {
        return new StoryContext(currentLocationId, gameDay, timeOfDay, roleId, lastTriggerId);
    }

    public StoryContext withTrigger(String triggerId) {
        return new StoryContext(currentLocationId, gameDay, timeOfDay, activeCharacterRoleId, triggerId);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public boolean isLocation(String locationId) {
        return currentLocationId != null
                && currentLocationId.equals(Objects.requireNonNull(locationId, "Location id is required."));
    }

    public boolean isTimeOfDay(String period) {
        return timeOfDay != null
                && timeOfDay.equalsIgnoreCase(Objects.requireNonNull(period, "Time of day is required."));
    }

    public boolean isActiveCharacter(String roleId) {
        return activeCharacterRoleId != null
                && activeCharacterRoleId.equals(Objects.requireNonNull(roleId, "Character role id is required."));
    }

    public boolean hasTrigger(String triggerId) {
        return lastTriggerId != null
                && lastTriggerId.equals(Objects.requireNonNull(triggerId, "Trigger id is required."));
    }
}
