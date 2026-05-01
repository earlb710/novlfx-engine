package com.eb.javafx.gamesupport;

/** Generic time slots used by the first-pass scheduling foundation. */
public enum TimeSlot {
    MORNING("morning"),
    AFTERNOON("afternoon"),
    EVENING("evening"),
    NIGHT("night");

    private final String id;

    TimeSlot(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public TimeSlot next() {
        TimeSlot[] slots = values();
        return slots[(ordinal() + 1) % slots.length];
    }
}
