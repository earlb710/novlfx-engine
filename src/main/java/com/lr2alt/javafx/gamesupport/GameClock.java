package com.lr2alt.javafx.gamesupport;

import java.util.Objects;

/** Generic calendar/time-slot service for section 2 scheduling support. */
public final class GameClock {
    private GameDateTime currentTime = new GameDateTime(1, TimeSlot.MORNING);

    public GameDateTime currentTime() {
        return currentTime;
    }

    public GameDateTime advanceSlot() {
        currentTime = currentTime.nextSlot();
        return currentTime;
    }

    public void setCurrentTime(GameDateTime currentTime) {
        this.currentTime = Objects.requireNonNull(currentTime, "currentTime");
    }
}
