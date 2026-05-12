package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class TimedChoiceMenuTest {

    @Test
    void choiceTimeoutDefaultsToNull() {
        SceneStep step = SceneStep.choice("menu", List.of(
            SceneChoice.of("opt", "text.opt", SceneTransition.complete())
        ));
        assertNull(step.choiceTimeoutMs());
        assertNull(step.choiceTimeoutDefaultId());
    }

    @Test
    void withChoiceTimeoutStoresValues() {
        SceneStep step = SceneStep.choice("menu", List.of(
            SceneChoice.of("opt", "text.opt", SceneTransition.complete())
        )).withChoiceTimeout(5000L, "opt");

        assertEquals(5000L, step.choiceTimeoutMs());
        assertEquals("opt", step.choiceTimeoutDefaultId());
    }

    @Test
    void withChoiceTimeoutNullDefaultIdIsAllowed() {
        SceneStep step = SceneStep.choice("menu", List.of(
            SceneChoice.of("opt", "text.opt", SceneTransition.complete())
        )).withChoiceTimeout(3000L, null);

        assertEquals(3000L, step.choiceTimeoutMs());
        assertNull(step.choiceTimeoutDefaultId());
    }

    @Test
    void menuCaptionDefaultsToNull() {
        SceneStep step = SceneStep.choice("menu", List.of(
            SceneChoice.of("opt", "text.opt", SceneTransition.complete())
        ));
        assertNull(step.menuCaptionTextKey());
    }

    @Test
    void withMenuCaptionStoresTextKey() {
        SceneStep step = SceneStep.choice("menu", List.of(
            SceneChoice.of("opt", "text.opt", SceneTransition.complete())
        )).withMenuCaption("choice.caption");

        assertEquals("choice.caption", step.menuCaptionTextKey());
    }
}
