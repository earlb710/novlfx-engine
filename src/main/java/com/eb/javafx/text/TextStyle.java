package com.eb.javafx.text;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable style/effect state attached to parsed text spans.
 *
 * <p>Color values are parser-preserved strings, and effects are immutable
 * key/value metadata such as gradient, kinetic, or glitch options. The plain
 * style has no emphasis, color, or effects.</p>
 */
public final class TextStyle {
    private final boolean bold;
    private final boolean italic;
    private final String color;
    private final Map<String, String> effects;

    /**
     * Creates immutable text style metadata.
     *
     * @param bold whether bold rendering is active
     * @param italic whether italic rendering is active
     * @param color optional authored color value
     * @param effects optional effect metadata copied into an immutable map
     */
    public TextStyle(boolean bold, boolean italic, String color, Map<String, String> effects) {
        this.bold = bold;
        this.italic = italic;
        this.color = color;
        this.effects = effects == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(effects));
    }

    /** Returns an unstyled text style with no color or effects. */
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
