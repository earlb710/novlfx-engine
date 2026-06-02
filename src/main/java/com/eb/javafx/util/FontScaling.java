package com.eb.javafx.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scales every {@code -fx-font-size} declaration in a stylesheet by a factor — the mechanism
 * behind the global "text size" accessibility setting.
 *
 * <p>Because the engine and game stylesheets pin font sizes in absolute {@code px}/{@code pt}
 * (which a root {@code -fx-font-size} does NOT scale), the only reliable way to make <em>all</em>
 * text bigger/smaller is to rewrite those size values. This rewrites the CSS text at
 * stylesheet-generation time, multiplying each font-size value by the scale and leaving every
 * other property (radii, padding, spacing) untouched.</p>
 */
public final class FontScaling {

    private static final Pattern FONT_SIZE = Pattern.compile(
            "(-fx-font-size\\s*:\\s*)([0-9]+(?:\\.[0-9]+)?)(px|pt|em)");

    private FontScaling() {
    }

    /**
     * Returns {@code css} with every {@code -fx-font-size} value multiplied by {@code scale}.
     * Returns the input unchanged when {@code scale} is ~1.0 or the css is null/empty.
     */
    public static String scale(String css, double scale) {
        if (css == null || css.isEmpty() || Math.abs(scale - 1.0) < 1.0e-6) {
            return css;
        }
        Matcher matcher = FONT_SIZE.matcher(css);
        StringBuilder out = new StringBuilder(css.length() + 32);
        while (matcher.find()) {
            double scaled = Double.parseDouble(matcher.group(2)) * scale;
            String replacement = matcher.group(1) + formatSize(scaled) + matcher.group(3);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** Formats a scaled size with up to one decimal place, dropping a trailing {@code .0}. */
    private static String formatSize(double value) {
        String formatted = String.format(Locale.ROOT, "%.1f", value);
        return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
    }
}
