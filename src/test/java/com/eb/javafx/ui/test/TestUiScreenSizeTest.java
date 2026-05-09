package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TestUiScreenSizeTest {
    @Test
    void capsLargeRequestedSizes() {
        assertEquals(800, TestUiScreenSize.capWidth(1200));
        assertEquals(600, TestUiScreenSize.capHeight(900));
    }

    @Test
    void capsPreferenceBackedSceneSizes() {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        preferencesService.saveWindowSize(1600, 900);

        assertEquals(800, TestUiScreenSize.sceneWidth(preferencesService));
        assertEquals(600, TestUiScreenSize.sceneHeight(preferencesService));
    }
}
