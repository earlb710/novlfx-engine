package com.eb.javafx.util;

/**
 * Lenient parsing of {@code config.json} scalar strings into numbers, with consistent
 * blank / unparseable handling.
 *
 * <p>Config values arrive as strings (a number in the JSON is normalised to its string form by the
 * resource-config fold). These helpers centralise the "parse or keep the default" semantics every
 * config consumer needs, so the same lenient rules aren't re-implemented per call site.</p>
 *
 * <ul>
 *   <li>{@code *OrNull} variants return {@code null} on blank / unparseable input (and, where noted,
 *       on out-of-range input) so the caller keeps its existing value.</li>
 *   <li>{@code *Or(value, fallback)} variants return the supplied fallback instead.</li>
 * </ul>
 */
public final class ConfigValues {

    private ConfigValues() {
    }

    /** Positive int (a numeric string is rounded); {@code null} if blank / unparseable / &le; 0. */
    public static Integer parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = (int) Math.round(Double.parseDouble(value.trim()));
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** Positive double; {@code null} if blank / unparseable / &le; 0. */
    public static Double parsePositiveDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** Any double (zero / negative allowed, e.g. an opacity where 0.0 is meaningful);
     *  {@code null} if blank / unparseable. */
    public static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** Positive float (e.g. a JPEG quality in (0,1]); {@code null} if blank / unparseable / &le; 0. */
    public static Float parsePositiveFloat(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            float parsed = Float.parseFloat(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** A 0–1 value (e.g. an alpha); {@code null} if absent / out-of-range / unparseable. */
    public static Double parseUnitOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value.trim());
            return (parsed >= 0.0 && parsed <= 1.0) ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /** Int (a numeric string is rounded) or {@code fallback} on blank / unparseable input. */
    public static int parseIntOr(String value, int fallback) {
        try {
            return value == null || value.isBlank()
                    ? fallback : (int) Math.round(Double.parseDouble(value.trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    /** Double or {@code fallback} on blank / unparseable input. */
    public static double parseDoubleOr(String value, double fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    /** Clamps {@code value} to the {@code [0, 1]} interval. */
    public static double clampUnit(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
