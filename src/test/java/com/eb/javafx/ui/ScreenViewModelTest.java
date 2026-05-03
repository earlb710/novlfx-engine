package com.eb.javafx.ui;

import com.eb.javafx.scene.SceneChoiceViewModel;
import com.eb.javafx.scene.SceneDialogueRowViewModel;
import com.eb.javafx.scene.SceneEffectPreviewViewModel;
import com.eb.javafx.scene.SceneExecutionStatus;
import com.eb.javafx.scene.SceneStatusRowViewModel;
import com.eb.javafx.scene.SceneStepType;
import com.eb.javafx.scene.SceneViewModel;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class ScreenViewModelTest {
    @Test
    void copiesLinesAndActionsForReusableScreenContracts() {
        List<String> lines = new ArrayList<>(List.of("Ready"));
        List<ScreenActionViewModel> actions = new ArrayList<>(List.of(
                new ScreenActionViewModel("Back", "main-menu", true)));

        ScreenViewModel viewModel = new ScreenViewModel("Title", lines, actions);
        lines.add("Mutated");
        actions.clear();

        assertEquals(List.of("Ready"), viewModel.lines());
        assertEquals(1, viewModel.actions().size());
    }

    @Test
    void validatesRequiredTextForReusableScreenContracts() {
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenViewModel("", List.of("Ready"), List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenViewModel("Title", List.of(" "), List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenActionViewModel("Back", "", true));
    }

    @Test
    void sceneViewModelCarriesDialogueStatusChoiceHistoryAndEffectPreviewState() {
        SceneViewModel viewModel = new SceneViewModel(
                SceneExecutionStatus.WAITING_FOR_CHOICE,
                "intro",
                "choice",
                "ava",
                "dialogue.ava",
                "ava/happy",
                List.of(new SceneChoiceViewModel(
                        "continue",
                        "choice.continue",
                        true,
                        null,
                        true,
                        Map.of("preview.effect", "advance"),
                        List.of(new SceneEffectPreviewViewModel("effect", "advance")))),
                "Waiting for input.",
                List.of(new SceneDialogueRowViewModel(SceneStepType.DIALOGUE, "ava", "dialogue.ava", "ava/happy")),
                List.of(new SceneStatusRowViewModel("Status", "WAITING_FOR_CHOICE")),
                List.of(new SceneEffectPreviewViewModel("pose", "happy")),
                List.of("continue"));

        assertEquals("intro", viewModel.sceneId());
        assertEquals(1, viewModel.dialogueRows().size());
        assertEquals(1, viewModel.statusRows().size());
        assertEquals(1, viewModel.effectPreviews().size());
        assertEquals(List.of("continue"), viewModel.selectedChoiceIds());
        assertEquals("advance", viewModel.choices().get(0).effectPreviews().get(0).value());
        assertEquals("advance", viewModel.choices().get(0).metadata().get("preview.effect"));
    }
}
