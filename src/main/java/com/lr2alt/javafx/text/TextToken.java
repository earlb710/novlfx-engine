package com.lr2alt.javafx.text;

/**
 * Parsed text, pause, or paragraph marker produced from Ren'Py text tags.
 */
public final class TextToken {
    private final TextTokenType type;
    private final String text;
    private final TextStyle style;
    private final double durationSeconds;

    private TextToken(TextTokenType type, String text, TextStyle style, double durationSeconds) {
        this.type = type;
        this.text = text;
        this.style = style;
        this.durationSeconds = durationSeconds;
    }

    public static TextToken text(String text, TextStyle style) {
        return new TextToken(TextTokenType.TEXT, text, style, 0.0);
    }

    public static TextToken pause(double durationSeconds) {
        return new TextToken(TextTokenType.PAUSE, "", TextStyle.plain(), durationSeconds);
    }

    public static TextToken paragraph() {
        return new TextToken(TextTokenType.PARAGRAPH, "", TextStyle.plain(), 0.0);
    }

    public TextTokenType type() {
        return type;
    }

    public String text() {
        return text;
    }

    public TextStyle style() {
        return style;
    }

    public double durationSeconds() {
        return durationSeconds;
    }
}
