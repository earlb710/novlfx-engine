package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.util.Validation;

/**
 * Shared size limits for manual test/support UI screens.
 */
public final class TestUiScreenSize {
    public static final int MAX_WIDTH = 800;
    public static final int MAX_HEIGHT = 600;

    private TestUiScreenSize() {
    }

    public static int capWidth(int width) {
        return Math.min(width, MAX_WIDTH);
    }

    public static int capHeight(int height) {
        return Math.min(height, MAX_HEIGHT);
    }

    public static int sceneWidth(PreferencesService preferencesService) {
        return capWidth(Validation.requireNonNull(preferencesService, "Preferences service is required.").windowWidth());
    }

    public static int sceneHeight(PreferencesService preferencesService) {
        return capHeight(Validation.requireNonNull(preferencesService, "Preferences service is required.").windowHeight());
    }
}
