package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SceneChoiceCaptionTest {

    @Test
    void choiceCaptionDefaultsToNull() {
        SceneChoice choice = SceneChoice.of("opt", "text.opt", SceneTransition.complete());
        assertNull(choice.captionTextKey());
    }

    @Test
    void withCaptionStoresTextKey() {
        SceneChoice choice = SceneChoice.of("opt", "text.opt", SceneTransition.complete())
            .withCaption("choice.opt.hint");
        assertEquals("choice.opt.hint", choice.captionTextKey());
    }

    @Test
    void withCaptionRejectsBlankKey() {
        SceneChoice choice = SceneChoice.of("opt", "text.opt", SceneTransition.complete());
        assertThrows(IllegalArgumentException.class, () -> choice.withCaption(""));
    }

    @Test
    void captionSurvivesOtherBuilderCalls() {
        SceneChoice choice = SceneChoice.of("opt", "text.opt", SceneTransition.complete())
            .withCaption("choice.opt.hint")
            .withTooltipText("tooltip.text");
        assertEquals("choice.opt.hint", choice.captionTextKey());
    }
}
