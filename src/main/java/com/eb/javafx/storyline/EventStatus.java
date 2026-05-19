package com.eb.javafx.storyline;

import com.eb.javafx.util.Validation;

/**
 * Conventions and helpers around event status strings.
 *
 * <p>An event's "status" is the free-form string recorded when the event finishes. The
 * conventional default is {@link #COMPLETED}; when an event's trigger collects a player choice,
 * the chosen option's id becomes the status instead (see {@code StorylineDirector}). Sub-events
 * are keyed by the parent's recorded status, so authoring choice ids like {@code accept} /
 * {@code refuse} drives branching directly without any extra enum machinery.</p>
 *
 * <p>This is a holder class, not a type — status remains a plain {@code String} throughout the
 * API. Centralising the constants here keeps spelling consistent across consumers.</p>
 */
public final class EventStatus {
    /** Default status recorded for an event that finishes without a player choice. */
    public static final String COMPLETED = "COMPLETED";
    /** Convenience status for authored failure paths. The runtime never sets this automatically. */
    public static final String FAILED = "FAILED";
    /** Convenience status for authored skip paths. The runtime never sets this automatically. */
    public static final String SKIPPED = "SKIPPED";

    private EventStatus() {
    }

    /** Normalises a status by trimming and rejecting null/blank strings. */
    public static String require(String value) {
        return Validation.requireNonBlank(value, "Event status is required.").trim();
    }

    /** {@code true} when both statuses match after trim/case-insensitive comparison. */
    public static boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }
}
