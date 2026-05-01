package com.eb.javafx.util;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/** Shared formatting helpers for timestamps and elapsed diagnostics. */
public final class TimeFormatting {
    private TimeFormatting() {
    }

    public static String formatInstant(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(Validation.requireNonNull(instant, "Instant is required."));
    }

    public static Instant parseInstant(String value) {
        return Instant.parse(Validation.requireNonBlank(value, "Instant text is required."));
    }

    public static long elapsedMillis(Instant startedAt, Instant completedAt) {
        return Duration.between(
                Validation.requireNonNull(startedAt, "Start instant is required."),
                Validation.requireNonNull(completedAt, "Completed instant is required.")).toMillis();
    }

    public static String formatElapsedMillis(Instant startedAt, Instant completedAt) {
        return elapsedMillis(startedAt, completedAt) + " ms";
    }
}
