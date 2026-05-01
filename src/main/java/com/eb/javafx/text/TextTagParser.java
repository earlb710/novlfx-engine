package com.eb.javafx.text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Section 1.7 parser for Ren'Py-style text tags used by migrated dialogue UI.
 *
 * <p>The parser turns inline tags into explicit tokens and style metadata. Unknown
 * tags are preserved as literal text so authored content is not silently lost
 * while custom replacers are still being migrated.</p>
 */
public final class TextTagParser {

    /**
     * Parses text into styled text, pause, and paragraph tokens.
     *
     * <p>Supported tags include bold, italic, color, wait, paragraph, gradient,
     * kinetic, and glitch markers. Unknown tags are emitted as literal text, while
     * malformed wait durations throw an {@link IllegalArgumentException}.</p>
     */
    public List<TextToken> parse(String source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        List<TextToken> tokens = new ArrayList<>();
        StyleState state = new StyleState();
        StringBuilder text = new StringBuilder();
        int index = 0;
        while (index < source.length()) {
            char character = source.charAt(index);
            if (character == '{') {
                int close = source.indexOf('}', index + 1);
                if (close > index) {
                    String tag = source.substring(index + 1, close);
                    if (applyTag(tag, state, tokens, text)) {
                        index = close + 1;
                        continue;
                    }
                }
            }
            text.append(character);
            index++;
        }
        flushText(tokens, text, state);
        return List.copyOf(tokens);
    }

    private boolean applyTag(String tag, StyleState state, List<TextToken> tokens, StringBuilder text) {
        switch (tag) {
            case "b" -> {
                flushText(tokens, text, state);
                state.bold = true;
                return true;
            }
            case "/b" -> {
                flushText(tokens, text, state);
                state.bold = false;
                return true;
            }
            case "i" -> {
                flushText(tokens, text, state);
                state.italic = true;
                return true;
            }
            case "/i" -> {
                flushText(tokens, text, state);
                state.italic = false;
                return true;
            }
            case "/color" -> {
                flushText(tokens, text, state);
                state.color = null;
                return true;
            }
            case "/gradient", "/kinetic", "/glitch" -> {
                flushText(tokens, text, state);
                state.effects.remove(tag.substring(1));
                return true;
            }
            case "p" -> {
                flushText(tokens, text, state);
                tokens.add(TextToken.paragraph());
                return true;
            }
            default -> {
                if (tag.startsWith("color=")) {
                    flushText(tokens, text, state);
                    state.color = tag.substring("color=".length());
                    return true;
                }
                if (tag.startsWith("w=")) {
                    flushText(tokens, text, state);
                    tokens.add(TextToken.pause(parseDuration(tag.substring("w=".length()))));
                    return true;
                }
                if (tag.startsWith("gradient=") || tag.startsWith("kinetic=") || tag.startsWith("glitch=")) {
                    flushText(tokens, text, state);
                    int separator = tag.indexOf('=');
                    state.effects.put(tag.substring(0, separator), tag.substring(separator + 1));
                    return true;
                }
                return false;
            }
        }
    }

    private double parseDuration(String rawValue) {
        try {
            return Math.max(0.0, Double.parseDouble(rawValue));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid Ren'Py pause tag duration: " + rawValue, exception);
        }
    }

    private void flushText(List<TextToken> tokens, StringBuilder text, StyleState state) {
        if (text.length() > 0) {
            tokens.add(TextToken.text(text.toString(), state.toStyle()));
            text.setLength(0);
        }
    }

    private static final class StyleState {
        private boolean bold;
        private boolean italic;
        private String color;
        private final Map<String, String> effects = new LinkedHashMap<>();

        private TextStyle toStyle() {
            return new TextStyle(bold, italic, color, effects);
        }
    }
}
