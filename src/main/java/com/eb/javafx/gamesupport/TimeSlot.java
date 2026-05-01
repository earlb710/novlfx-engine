package com.eb.javafx.gamesupport;

/**
 * Generic time slots used by the first-pass scheduling foundation.
 *
 * <p>Each enum has a stable lowercase ID for authored data and save summaries;
 * callers should use {@link #id()} rather than relying on enum constant names for
 * external formats.</p>
 */
public enum TimeSlot {
    /** Start-of-day scheduling slot. */
    MORNING("morning"),
    /** Midday scheduling slot. */
    AFTERNOON("afternoon"),
    /** Late-day scheduling slot. */
    EVENING("evening"),
    /** End-of-day scheduling slot; advancing rolls to morning. */
    NIGHT("night");

    private final String id;

    TimeSlot(String id) {
        this.id = id;
    }

    /** Returns the stable external ID for authored data and persistence. */
    public String id() {
        return id;
    }

    /** Returns the next slot, wrapping from night back to morning. */
    public TimeSlot next() {
        TimeSlot[] slots = values();
        return slots[(ordinal() + 1) % slots.length];
    }
}
