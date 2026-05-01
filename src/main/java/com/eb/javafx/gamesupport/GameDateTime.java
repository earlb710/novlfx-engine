package com.eb.javafx.gamesupport;

/** Immutable generic day/time-slot value for scheduling and turn-advance support. */
public final class GameDateTime {
    private final int day;
    private final String timeSlotId;

    /**
     * Creates a valid game date/time value.
     *
     * @param day one-based day number
     * @param timeSlotId stable slot code within the owning time-slot table
     */
    public GameDateTime(int day, String timeSlotId) {
        if (day < 1) {
            throw new IllegalArgumentException("day must be at least 1.");
        }
        if (timeSlotId == null || timeSlotId.isBlank()) {
            throw new IllegalArgumentException("timeSlotId must not be blank.");
        }
        this.day = day;
        this.timeSlotId = timeSlotId;
    }

    public int day() {
        return day;
    }

    public String timeSlotId() {
        return timeSlotId;
    }

    /** Returns the next slot according to the supplied table, rolling to the next day after the final code. */
    public GameDateTime nextSlot(CodeTableDefinition timeSlots) {
        String nextSlotId = timeSlots.nextCodeId(timeSlotId);
        return new GameDateTime(nextSlotId.equals(timeSlots.firstCodeId()) ? day + 1 : day, nextSlotId);
    }

    @Override
    public String toString() {
        return "day " + day + " " + timeSlotId;
    }
}
