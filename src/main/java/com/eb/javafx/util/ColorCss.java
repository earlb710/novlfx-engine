package com.eb.javafx.util;

import java.util.Locale;

import javafx.scene.paint.Color;

/**
 * Game-agnostic colour maths and JavaFX inline-CSS helpers: Rec.601 luminance, light/dark tests,
 * alpha override, {@code rgba(...)} formatting and a small {@code -fx-} label-style builder, plus
 * the trivial numeric clamps that colour-component code repeatedly needs.
 *
 * <p>Consolidated here so the Rec.601 weights, the {@code rgba(...)} string format and the 0–1↔0–255
 * conversions have a single implementation instead of being copy-pasted across screens and material
 * bakers.</p>
 */
public final class ColorCss {

    private ColorCss() {
    }

    /** Clamps a value to {@code [0, 1]}. */
    public static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    /** Maps a 0.0–1.0 component to 0–255 (rounded, clamped). */
    public static int to255(double v) {
        return Math.max(0, Math.min(255, (int) Math.round(v * 255)));
    }

    /** Clamps an integer to the 0–255 byte range. */
    public static int clampByte255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    /** Rec.601 perceived luminance of {@code c} in {@code [0, 1]} ({@code 0.299r + 0.587g + 0.114b}). */
    public static double luminance(Color c) {
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }

    /** True when {@code c} is light enough to want dark text over it (luminance &gt; {@code 0.55}). */
    public static boolean isLight(Color c) {
        return luminance(c) > 0.55;
    }

    /** True when {@code c}'s luminance exceeds {@code threshold}. */
    public static boolean isLight(Color c, double threshold) {
        return luminance(c) > threshold;
    }

    /** {@code c} with its alpha replaced by {@code alpha} (RGB preserved). */
    public static Color withAlpha(Color c, double alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /** Linear RGB interpolation between two colours: {@code t=0} → {@code a}, {@code t=1} → {@code b}
     *  (alpha forced to 1.0; {@code t} is not clamped). */
    public static Color lerp(Color a, Color b, double t) {
        return new Color(
                a.getRed()   + (b.getRed()   - a.getRed())   * t,
                a.getGreen() + (b.getGreen() - a.getGreen()) * t,
                a.getBlue()  + (b.getBlue()  - a.getBlue())  * t,
                1.0);
    }

    /** Formats a 0..1 alpha to a stable, locale-independent CSS token ({@code %.2f}), clamped to
     *  {@code [0,1]}. Avoids locales where {@code Double.toString(0.5)} renders as {@code "0,5"} —
     *  which JavaFX CSS rejects. */
    public static String formatAlpha(double alpha) {
        return String.format(Locale.ROOT, "%.2f", clamp01(alpha));
    }

    /** An {@code -fx-background-color: rgba(...)} inline style for {@code base} at {@code alpha}
     *  (locale-safe alpha via {@link #formatAlpha}). */
    public static String backgroundColorStyle(Color base, double alpha) {
        return "-fx-background-color: rgba("
                + to255(base.getRed()) + "," + to255(base.getGreen()) + "," + to255(base.getBlue())
                + "," + formatAlpha(alpha) + ");";
    }

    /** {@code c} as an {@code rgba(r,g,b,a)} CSS string (locale-independent). */
    public static String toRgbaCss(Color c) {
        return String.format(Locale.ROOT,
                "rgba(%d,%d,%d,%.3f)",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255),
                c.getOpacity());
    }

    /**
     * Builds an inline {@code -fx-} style string for a label: text fill + font size, with optional
     * font weight and background fill.
     *
     * @param text       text-fill colour (required)
     * @param fontSize   font size in points
     * @param fontWeight {@code -fx-font-weight} value (e.g. {@code "bold"}), or {@code null} to omit
     * @param bg         background fill, or {@code null} to omit
     */
    public static String labelStyle(Color text, int fontSize, String fontWeight, Color bg) {
        StringBuilder sb = new StringBuilder();
        sb.append("-fx-text-fill: ").append(toRgbaCss(text)).append(";");
        sb.append(" -fx-font-size: ").append(fontSize).append(";");
        if (fontWeight != null) {
            sb.append(" -fx-font-weight: ").append(fontWeight).append(";");
        }
        if (bg != null) {
            sb.append(" -fx-background-color: ").append(toRgbaCss(bg)).append(";");
        }
        return sb.toString();
    }
}
