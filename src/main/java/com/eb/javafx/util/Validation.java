package com.eb.javafx.util;

import java.util.Collection;
import java.util.Objects;

/** Focused validation helpers for reusable engine value objects and services. */
public final class Validation {
    private Validation() {
    }

    public static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static <T extends Collection<?>> T requireNonEmpty(T value, String message) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static int requirePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static double requirePositive(double value, String message) {
        if (value <= 0.0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static long requireZeroOrPositive(long value, String message) {
        if (value < 0L) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static double requireBetween(double value, double minInclusive, double maxInclusive, String message) {
        if (value < minInclusive || value > maxInclusive) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    public static double requireUnitInterval(double value, String message) {
        return requireBetween(value, 0.0, 1.0, message);
    }

    public static int requireSlot(int slot) {
        if (slot < 1 || slot > 999) {
            throw new IllegalArgumentException("Save slot must be between 1 and 999.");
        }
        return slot;
    }

    public static double clamp(double value, double minInclusive, double maxInclusive) {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("Minimum cannot be greater than maximum.");
        }
        return Math.max(minInclusive, Math.min(maxInclusive, value));
    }

    public static double clampUnitInterval(double value) {
        return clamp(value, 0.0, 1.0);
    }

    public static <T> T requireNonNullState(T value, String message) {
        return Objects.requireNonNull(value, message);
    }
}
