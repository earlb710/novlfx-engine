package com.eb.javafx.display;

/**
 * Ordered JavaFX display layers replacing Ren'Py image layer placement.
 */
public enum DisplayLayer {
    BACKGROUND(0),
    CHARACTER(100),
    OVERLAY(200),
    HUD(300);

    private final int zOrder;

    DisplayLayer(int zOrder) {
        this.zOrder = zOrder;
    }

    public int zOrder() {
        return zOrder;
    }
}
