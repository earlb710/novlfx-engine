package com.eb.javafx.display;

/**
 * Ordered JavaFX display layers replacing Ren'Py image layer placement.
 */
public enum DisplayLayer {
    /** Background scenery behind characters and UI overlays. */
    BACKGROUND(0),
    /** Character sprites and layered character composites. */
    CHARACTER(100),
    /** Scene overlays such as effects, notifications, or modal panels. */
    OVERLAY(200),
    /** Persistent heads-up display and controls above scene content. */
    HUD(300);

    private final int zOrder;

    DisplayLayer(int zOrder) {
        this.zOrder = zOrder;
    }

    /** Returns the numeric ordering hint used when composing JavaFX display nodes. */
    public int zOrder() {
        return zOrder;
    }
}
