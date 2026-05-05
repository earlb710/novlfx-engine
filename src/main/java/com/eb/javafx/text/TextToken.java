package com.eb.javafx.text;

import com.eb.javafx.util.Validation;

/**
 * Parsed text, icon, pause, or paragraph marker produced from visual novel text tags.
 *
 * <p>Only text tokens use the text/style fields meaningfully; icon tokens use
 * the icon ID field, pause tokens use duration seconds, and paragraph tokens
 * act as structural markers with plain style and zero duration.</p>
 */
public final class TextToken {
    private final TextTokenType type;
    private final String text;
    private final String iconId;
    private final TextStyle style;
    private final double durationSeconds;

    private TextToken(TextTokenType type, String text, String iconId, TextStyle style, double durationSeconds) {
        this.type = type;
        this.text = text;
        this.iconId = iconId;
        this.style = style;
        this.durationSeconds = durationSeconds;
    }

    /** Creates a styled text span token. */
    public static TextToken text(String text, TextStyle style) {
        return new TextToken(TextTokenType.TEXT, text, "", style, 0.0);
    }

    /** Creates an inline icon marker token using a registered image ID. */
    public static TextToken icon(String iconId) {
        return new TextToken(TextTokenType.ICON, "", Validation.requireNonBlank(iconId, "Icon id is required."),
                TextStyle.plain(), 0.0);
    }

    /** Creates a wait/pause token with duration in seconds. */
    public static TextToken pause(double durationSeconds) {
        return new TextToken(TextTokenType.PAUSE, "", "", TextStyle.plain(), durationSeconds);
    }

    /** Creates a paragraph-break marker token. */
    public static TextToken paragraph() {
        return new TextToken(TextTokenType.PARAGRAPH, "", "", TextStyle.plain(), 0.0);
    }

    public TextTokenType type() {
        return type;
    }

    public String text() {
        return text;
    }

    public String iconId() {
        return iconId;
    }

    public TextStyle style() {
        return style;
    }

    public double durationSeconds() {
        return durationSeconds;
    }
}
