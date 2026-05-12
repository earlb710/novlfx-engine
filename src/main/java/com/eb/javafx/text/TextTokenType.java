package com.eb.javafx.text;

/**
 * Token kinds emitted by the section 1.7 visual novel text-tag parser.
 */
public enum TextTokenType {
    /** Styled text span that can be rendered directly. */
    TEXT,
    /** Inline icon/image marker emitted from icon tags. */
    ICON,
    /** Timed pause marker emitted from wait tags. */
    PAUSE,
    /** Paragraph break marker emitted from paragraph tags. */
    PARAGRAPH,
    /** Pause until the player clicks to continue (Ren'Py {w} tag). */
    WAIT_CLICK,
    /** Suppress default end-of-line pause; advance immediately (Ren'Py {nw} tag). */
    NO_WAIT,
    /** Set characters-per-second typewriter speed (Ren'Py {cps=N} tag). */
    SET_CPS,
    /** Skip remaining typewriter animation and display full text (Ren'Py {fast} tag). */
    FAST_FORWARD
}
