package com.eb.javafx.scene;

/** Controls how the scene executor responds to text steps during playback. */
public enum ScenePlaybackMode {
    /** Normal play: pause on every text step. */
    NORMAL,
    /** Skip mode: bypass seen text steps; always stop at unseen steps and all CHOICE steps. */
    SKIP,
    /** Auto mode: text steps advance automatically after a UI-configured delay; engine advances headlessly like NORMAL. */
    AUTO
}
