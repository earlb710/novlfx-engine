package com.eb.javafx.gamesupport;

/**
 * Immutable generic day/time-slot value for scheduling and turn-advance support.
 *
 * <p>Days are one-based and time slots advance through {@link TimeSlot#next()}.
 * Advancing from night to morning rolls over to the next day.</p>
 */
public final class GameDateTime {
    private final int day;
    private final TimeSlot timeSlot;

    /**
     * Creates a valid game date/time value.
     *
     * @param day one-based day number
     * @param timeSlot non-null slot within the day
     */
    public GameDateTime(int day, TimeSlot timeSlot) {
        if (day < 1) {
            throw new IllegalArgumentException("day must be at least 1.");
        }
        if (timeSlot == null) {
            throw new IllegalArgumentException("timeSlot must not be null.");
        }
        this.day = day;
        this.timeSlot = timeSlot;
    }

    public int day() {
        return day;
    }

    public TimeSlot timeSlot() {
        return timeSlot;
    }

    /** Returns the next slot, rolling to the next day after {@link TimeSlot#NIGHT}. */
    public GameDateTime nextSlot() {
        TimeSlot nextSlot = timeSlot.next();
        return new GameDateTime(nextSlot == TimeSlot.MORNING ? day + 1 : day, nextSlot);
    }

    @Override
    public String toString() {
        return "day " + day + " " + timeSlot.id();
    }
}
