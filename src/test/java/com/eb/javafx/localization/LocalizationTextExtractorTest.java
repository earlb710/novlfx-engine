package com.eb.javafx.localization;

import com.eb.javafx.scene.SceneChoice;
import com.eb.javafx.scene.SceneDefinition;
import com.eb.javafx.scene.SceneStep;
import com.eb.javafx.scene.SceneTransition;
import com.eb.javafx.text.DialogMessage;
import com.eb.javafx.text.DialogSpeaker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LocalizationTextExtractorTest {

    @Test
    void extractsSceneTextIdsInEncounterOrder() {
        LocalizationTextExtractor extractor = new LocalizationTextExtractor();
        SceneDefinition scene = SceneDefinition.of("intro", List.of(
                SceneStep.narration("open", "intro.open"),
                SceneStep.choice("choice", List.of(
                        SceneChoice.of("continue", "intro.continue", SceneTransition.complete())))));

        List<String> ids = new ArrayList<>(extractor.sceneTextIds(List.of(scene)));

        assertEquals(List.of("intro.open", "intro.continue"), ids);
        assertEquals("intro.open", extractor.sceneSkeletonBundle("en", List.of(scene)).texts().get("intro.open"));
    }

    @Test
    void extractsPlainDialogSourceText() {
        LocalizationTextExtractor extractor = new LocalizationTextExtractor();
        DialogMessage message = DialogMessage.speakerMessage(
                DialogSpeaker.text("guide", "Guide"),
                "{b}Hello{/b}{p}world{w=0.4}");

        LocalizedTextBundle bundle = extractor.dialogSourceBundle("en", Map.of("intro.guide", message));

        assertEquals("Hello" + System.lineSeparator() + "world", bundle.textOr("intro.guide", ""));
    }
}
