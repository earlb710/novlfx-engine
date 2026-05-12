package com.eb.javafx.transitions;

/** Named visual-effect types played by the display adapter between scene changes. */
public enum SceneTransitionEffect {
    NONE,
    DISSOLVE,
    FADE_BLACK,
    WIPE_LEFT,
    WIPE_RIGHT,
    MOVE_IN_LEFT,
    MOVE_IN_RIGHT;

    public boolean isInstant() {
        return this == NONE;
    }
}
