package com.eb.javafx.ui;

import javafx.scene.paint.Color;

/**
 * Generic theme-aware colour math: luminance, light/dark theme inference, and channel darkening.
 *
 * <p>Pure colour utilities with no game-domain coupling — a game decides <em>which</em> colours to
 * paint (its palette choices stay in the game), but the "is this theme light or dark?" inference and
 * the "darken this accent so it reads on a pastel background" transform are identical for every
 * game.</p>
 */
public final class ThemeColors {

    private ThemeColors() {
    }

    /** Rec.601 relative luminance (0–1) of {@code color}. */
    public static double relativeLuminance(Color color) {
        return 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
    }

    /**
     * Whether the active theme should be treated as "light", inferred from its text colour:
     * dark text ⇒ light background ⇒ light theme (and vice versa). Returns {@code false} for a null
     * theme / blank / unparseable text colour.
     */
    public static boolean isLightTheme(UiTheme uiTheme) {
        if (uiTheme == null) {
            return false;
        }
        try {
            String textHex = uiTheme.textColor();
            if (textHex == null || textHex.isBlank()) {
                return false;
            }
            return relativeLuminance(Color.web(textHex)) < 0.5;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Darkens each RGB channel of {@code hex} by {@code factor} (typically 0–1), preserving hue.
     * Returns {@code hex} unchanged when it can't be parsed, or {@code null} for a null input.
     */
    public static String darken(String hex, double factor) {
        if (hex == null) {
            return null;
        }
        try {
            Color c = Color.web(hex);
            int r = clampChannel((int) Math.round(c.getRed() * 255.0 * factor));
            int g = clampChannel((int) Math.round(c.getGreen() * 255.0 * factor));
            int b = clampChannel((int) Math.round(c.getBlue() * 255.0 * factor));
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (IllegalArgumentException ignored) {
            return hex;
        }
    }

    /**
     * Lightens {@code hex} by blending each RGB channel {@code factor} of the way toward white
     * ({@code 0} = unchanged, {@code 1} = white), at full opacity, preserving hue otherwise.
     * Returns {@code hex} unchanged when it can't be parsed, or {@code null} for a null input.
     * The mirror of {@link #darken(String, double)}.
     */
    public static String lighten(String hex, double factor) {
        if (hex == null) {
            return null;
        }
        try {
            Color c = Color.web(hex);
            int r = clampChannel((int) Math.round((c.getRed()   + (1.0 - c.getRed())   * factor) * 255.0));
            int g = clampChannel((int) Math.round((c.getGreen() + (1.0 - c.getGreen()) * factor) * 255.0));
            int b = clampChannel((int) Math.round((c.getBlue()  + (1.0 - c.getBlue())  * factor) * 255.0));
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (IllegalArgumentException ignored) {
            return hex;
        }
    }

    /** Formats {@code color} as a JavaFX-CSS {@code rgb(r,g,b)} string (0–255 per channel). */
    public static String toCssRgb(Color color) {
        return String.format("rgb(%d,%d,%d)",
                (int) Math.round(color.getRed()   * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue()  * 255));
    }

    private static int clampChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
