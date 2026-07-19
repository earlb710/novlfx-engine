package com.eb.javafx.dialog;

import java.util.Optional;

/**
 * Declares that a {@link DialogChain} auto-fires when the MC enters a location, optionally pulling
 * a required character into the MC's room first.
 *
 * <ul>
 *   <li>{@link #buildingId} — the building whose entry fires the chain (required). When
 *       {@link #roomPath} is also set, the chain fires only on entering that specific room;
 *       otherwise it fires on entering the building (any room).</li>
 *   <li>{@link #repeatable} — {@code false} (default) fires once ever; {@code true} fires every
 *       time the MC enters.</li>
 *   <li>{@link #requiredCharacter} — book role of an NPC who must be present at the building for
 *       the chain to play. If present (in any room of the building), they are moved into the MC's
 *       room when the chain fires.</li>
 *   <li>{@link #missingPolicy} — what happens when the required character is NOT at the building:
 *       {@link MissingPolicy#BLOCK} prevents the MC from travelling there at all;
 *       {@link MissingPolicy#SKIP} lets the MC enter but the chain simply doesn't fire.</li>
 *   <li>{@link #moveBack} — when {@code true}, the required character returns to their original
 *       room once the chain finishes; when {@code false} (default) they stay in the MC's room.</li>
 * </ul>
 */
public record DialogLocationTrigger(
        String buildingId,
        String roomPath,
        boolean repeatable,
        String requiredCharacter,
        MissingPolicy missingPolicy,
        boolean moveBack) {

    /** Behaviour when the {@link #requiredCharacter} is not at the trigger building. */
    public enum MissingPolicy {
        /** Block travel to the building entirely. */
        BLOCK,
        /** Allow entry, but don't fire the chain. */
        SKIP;

        /** Parses a code ({@code "block"} / {@code "skip"}), defaulting to {@link #SKIP}. */
        public static MissingPolicy fromString(String value) {
            if (value == null || value.isBlank()) {
                return SKIP;
            }
            return "block".equalsIgnoreCase(value.trim()) ? BLOCK : SKIP;
        }
    }

    public DialogLocationTrigger {
        buildingId = requireText(buildingId, "Location trigger requires a buildingId.");
        roomPath = blankToNull(roomPath);
        requiredCharacter = blankToNull(requiredCharacter);
        missingPolicy = missingPolicy == null ? MissingPolicy.SKIP : missingPolicy;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    /** Specific room the trigger is scoped to, or empty when it fires on building entry. */
    public Optional<String> room() {
        return Optional.ofNullable(roomPath);
    }

    /** Book role required to be present, or empty when no character is required. */
    public Optional<String> requiredCharacterRole() {
        return Optional.ofNullable(requiredCharacter);
    }

    /** True when this trigger fires on entering {@code building} at {@code room} (room ignored
     *  when this trigger is building-scoped). */
    public boolean matches(String building, String room) {
        if (buildingId == null || building == null || !buildingId.equals(building)) {
            return false;
        }
        return roomPath == null || roomPath.equals(room);
    }
}
