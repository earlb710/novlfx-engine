package com.eb.javafx.globalApi;

/**
 * Navigation/screen actions represented by the section 1.6 Global API adapter.
 */
public enum GlobalRouteAction {
    /** Replace the current route with another route. */
    JUMP,
    /** Enter another route while preserving resumable caller intent. */
    CALL,
    /** Mark a screen-style route visible. */
    SHOW_SCREEN,
    /** Mark a screen-style route hidden. */
    HIDE_SCREEN
}
