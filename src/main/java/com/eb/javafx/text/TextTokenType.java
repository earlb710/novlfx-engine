package com.eb.javafx.text;

/**
 * Token kinds emitted by the section 1.7 Ren'Py text-tag parser.
 */
public enum TextTokenType {
    /** Styled text span that can be rendered directly. */
    TEXT,
    /** Timed pause marker emitted from wait tags. */
    PAUSE,
    /** Paragraph break marker emitted from paragraph tags. */
    PARAGRAPH
}
