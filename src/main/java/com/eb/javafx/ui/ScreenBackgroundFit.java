package com.eb.javafx.ui;

/**
 * Image sizing modes for reusable screen backgrounds.
 */
public enum ScreenBackgroundFit {
    /** Resize the image independently on each axis to fill the screen. */
    STRETCH,

    /** Preserve image ratio, cover the screen, and crop overflow from the center. */
    CROP_CENTER
}
