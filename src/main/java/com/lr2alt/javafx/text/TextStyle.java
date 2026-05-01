package com.lr2alt.javafx.text;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable style/effect state attached to parsed text spans.
 */
public final class TextStyle {
    private final boolean bold;
    private final boolean italic;
    private final String color;
    private final Map<String, String> effects;

    public TextStyle(boolean bold, boolean italic, String color, Map<String, String> effects) {
        this.bold = bold;
        this.italic = italic;
        this.color = color;
        this.effects = effects == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(effects));
    }

    public static TextStyle plain() {
        return new TextStyle(false, false, null, Collections.emptyMap());
    }

    public boolean bold() {
        return bold;
    }

    public boolean italic() {
        return italic;
    }

    public String color() {
        return color;
    }

    public Map<String, String> effects() {
        return effects;
    }
}
