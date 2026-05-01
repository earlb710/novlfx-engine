package com.eb.javafx.gamesupport;

import java.util.Objects;
import java.util.List;

/**
 * Generic calendar/time-slot service for reusable scheduling support.
 *
 * <p>The clock uses a project-supplied code table for slots so engines can define
 * any slot sequence instead of inheriting game-specific morning/afternoon-style
 * constants. Setters require a non-null {@link GameDateTime} whose slot ID exists
 * in the configured table.</p>
 */
public final class GameClock {
    private static final CodeTableDefinition DEFAULT_TIME_SLOTS = new CodeTableDefinition(
            "time-slots",
            "Time Slots",
            List.of(new CodeDefinition("default", "Default", 0, List.of())));

    private final CodeTableDefinition timeSlots;
    private GameDateTime currentTime;

    /** Creates a clock with a single generic default slot for tests and placeholder bootstrapping. */
    public GameClock() {
        this(DEFAULT_TIME_SLOTS);
    }

    /** Creates a clock starting at day 1 and the first code in the supplied time-slot table. */
    public GameClock(CodeTableDefinition timeSlots) {
        this(timeSlots, Objects.requireNonNull(timeSlots, "timeSlots").firstCodeId());
    }

    /** Creates a clock starting at day 1 and the supplied slot ID. */
    public GameClock(CodeTableDefinition timeSlots, String startingTimeSlotId) {
        this.timeSlots = Objects.requireNonNull(timeSlots, "timeSlots");
        validateSlot(startingTimeSlotId);
        currentTime = new GameDateTime(1, startingTimeSlotId);
    }

    /** Returns the configured generic time-slot table. */
    public CodeTableDefinition timeSlots() {
        return timeSlots;
    }

    /** Returns the current mutable clock value. */
    public GameDateTime currentTime() {
        return currentTime;
    }

    /** Advances to the next slot and returns the updated clock value. */
    public GameDateTime advanceSlot() {
        currentTime = currentTime.nextSlot(timeSlots);
        return currentTime;
    }

    /** Replaces the current time with a prevalidated non-null value. */
    public void setCurrentTime(GameDateTime currentTime) {
        GameDateTime newTime = Objects.requireNonNull(currentTime, "currentTime");
        validateSlot(newTime.timeSlotId());
        this.currentTime = newTime;
    }

    private void validateSlot(String timeSlotId) {
        if (!timeSlots.contains(timeSlotId)) {
            throw new IllegalArgumentException("Unknown time slot: " + timeSlotId);
        }
    }
}
