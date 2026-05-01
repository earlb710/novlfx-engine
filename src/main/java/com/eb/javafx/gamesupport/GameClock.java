package com.eb.javafx.gamesupport;

import java.util.Objects;

/**
 * Generic calendar/time-slot service for section 2 scheduling support.
 *
 * <p>The clock starts at day 1 morning and mutates only through explicit advance
 * or set calls. Setters require a non-null {@link GameDateTime} that has already
 * validated day and slot constraints.</p>
 */
public final class GameClock {
    private GameDateTime currentTime = new GameDateTime(1, TimeSlot.MORNING);

    /** Returns the current mutable clock value. */
    public GameDateTime currentTime() {
        return currentTime;
    }

    /** Advances to the next slot and returns the updated clock value. */
    public GameDateTime advanceSlot() {
        currentTime = currentTime.nextSlot();
        return currentTime;
    }

    /** Replaces the current time with a prevalidated non-null value. */
    public void setCurrentTime(GameDateTime currentTime) {
        this.currentTime = Objects.requireNonNull(currentTime, "currentTime");
    }
}
