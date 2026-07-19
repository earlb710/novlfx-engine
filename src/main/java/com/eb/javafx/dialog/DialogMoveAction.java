package com.eb.javafx.dialog;

import java.util.Optional;

/**
 * An optional movement instruction carried by a {@link DialogNode.Line}.
 *
 * <p>When a line carries a {@code DialogMoveAction} the director moves the MC
 * to the specified room / building as the line plays.  Either field may be
 * absent independently:</p>
 *
 * <ul>
 *   <li>{@code changeRoom} only — stay in the current building, move to a
 *       different room (e.g. "{@code Reception > Lab Floor}").</li>
 *   <li>{@code changeLocation} only — move to a different building's default
 *       room.</li>
 *   <li>Both set — move to {@code changeLocation} and land in {@code changeRoom}
 *       inside that building.</li>
 * </ul>
 *
 * <p>{@code following} records whether the person the MC is currently talking
 * to accompanies him.  When {@code true} the director should move that NPC to
 * the same destination before the next line plays.</p>
 *
 * <h3>JSON fields on a node</h3>
 * <pre>{@code
 * {
 *   "type":           "narrate",
 *   "id":             "walk.to.lab",
 *   "text":           "He leads the way to the lab floor.",
 *   "changeLocation": "lab",
 *   "changeRoom":     "Reception > Lab Floor",
 *   "following":      true
 * }
 * }</pre>
 */
public record DialogMoveAction(
        String changeLocation,
        String changeRoom,
        boolean following,
        boolean moveBack) {

    public DialogMoveAction {
        // at least one of the two destinations must be present
        if ((changeLocation == null || changeLocation.isBlank())
                && (changeRoom == null || changeRoom.isBlank())) {
            throw new IllegalArgumentException(
                    "DialogMoveAction requires at least one of changeLocation or changeRoom.");
        }
        changeLocation = (changeLocation == null || changeLocation.isBlank()) ? null : changeLocation.trim();
        changeRoom     = (changeRoom     == null || changeRoom.isBlank())     ? null : changeRoom.trim();
    }

    /** The building id to move to, when set. */
    public Optional<String> location() {
        return Optional.ofNullable(changeLocation);
    }

    /** The room path to move to, when set. */
    public Optional<String> room() {
        return Optional.ofNullable(changeRoom);
    }

    /** True when the MC's current conversation partner should follow him. */
    public boolean npcFollows() {
        return following;
    }

    /** True when, once the dialog chain finishes, the MC (and any following NPC) should
     *  return to the room / building they occupied before this move action fired. */
    public boolean shouldMoveBack() {
        return moveBack;
    }

    /** Convenience factory — room change only, within the current building. */
    public static DialogMoveAction toRoom(String roomPath, boolean following) {
        return new DialogMoveAction(null, roomPath, following, false);
    }

    /** Convenience factory — room change, with explicit move-back control. */
    public static DialogMoveAction toRoom(String roomPath, boolean following, boolean moveBack) {
        return new DialogMoveAction(null, roomPath, following, moveBack);
    }

    /** Convenience factory — building change, landing in the building's default room. */
    public static DialogMoveAction toBuilding(String buildingId, boolean following) {
        return new DialogMoveAction(buildingId, null, following, false);
    }

    /** Convenience factory — building change, with explicit move-back control. */
    public static DialogMoveAction toBuilding(String buildingId, boolean following, boolean moveBack) {
        return new DialogMoveAction(buildingId, null, following, moveBack);
    }

    /** Convenience factory — building change landing in a specific room. */
    public static DialogMoveAction toBuildingRoom(String buildingId, String roomPath, boolean following) {
        return new DialogMoveAction(buildingId, roomPath, following, false);
    }

    /** Convenience factory — building + room change, with explicit move-back control. */
    public static DialogMoveAction toBuildingRoom(String buildingId, String roomPath,
                                                   boolean following, boolean moveBack) {
        return new DialogMoveAction(buildingId, roomPath, following, moveBack);
    }
}
