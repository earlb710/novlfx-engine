package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ScreenTextResourcesTest {
    @Test
    void loadsRuntimeScreenTitleAndSidecarText() {
        assertEquals("Preferences", ScreenTextResources.title(ScreenTextResources.PREFERENCES));
        assertEquals("Audio", ScreenTextResources.text(ScreenTextResources.PREFERENCES, "block.audio.title"));
        assertEquals("Close preferences.", ScreenTextResources.text(
                ScreenTextResources.PREFERENCES,
                "footer.preferences.close.tooltip"));
    }

    @Test
    void formatsRuntimeScreenTextBindings() {
        assertEquals(
                "Save schema version: 3",
                ScreenTextResources.format(
                        ScreenTextResources.SAVE_LOAD,
                        "line.schema-version",
                        Map.of("schemaVersion", "3")));
    }
}
