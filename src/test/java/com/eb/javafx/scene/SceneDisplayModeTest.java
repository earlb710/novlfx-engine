package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SceneDisplayModeTest {

    @Test
    void defaultDisplayModeIsAdv() {
        SceneStep step = SceneStep.narration("line", "text.line");
        assertEquals(SceneDisplayMode.ADV, step.displayMode());
    }

    @Test
    void withNvlDisplayModeSetsMetadata() {
        SceneStep step = SceneStep.narration("line", "text.line").withDisplayMode(SceneDisplayMode.NVL);
        assertEquals(SceneDisplayMode.NVL, step.displayMode());
    }

    @Test
    void dialogueStepDefaultsToAdv() {
        SceneStep step = SceneStep.dialogue("line", "hero", "text.hello", null);
        assertEquals(SceneDisplayMode.ADV, step.displayMode());
    }
}
