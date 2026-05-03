package com.eb.javafx.ui;

import com.eb.javafx.scene.SceneChoiceViewModel;
import com.eb.javafx.scene.SceneDialogueRowViewModel;
import com.eb.javafx.scene.SceneEffectPreviewViewModel;
import com.eb.javafx.scene.SceneStatusRowViewModel;
import com.eb.javafx.scene.SceneViewModel;
import com.eb.javafx.util.Validation;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Reusable JavaFX scene-flow content with dialogue, status, effect previews, and choice callbacks.
 */
public final class SceneFlowView {
    private SceneFlowView() {
    }

    public static VBox createContent(SceneViewModel viewModel, Consumer<String> choiceHandler) {
        Validation.requireNonNull(viewModel, "Scene view model is required.");
        Consumer<String> effectiveChoiceHandler = Objects.requireNonNullElse(choiceHandler, id -> {
        });

        VBox content = new VBox(ScreenShell.BODY_SPACING);
        content.getChildren().add(statusPanel(viewModel));
        if (!viewModel.dialogueRows().isEmpty()) {
            content.getChildren().add(dialoguePanel(viewModel));
        }
        if (!viewModel.effectPreviews().isEmpty()) {
            content.getChildren().add(effectPanel(viewModel.effectPreviews()));
        }
        content.getChildren().add(choicePanel(viewModel, effectiveChoiceHandler));
        return content;
    }

    private static Node statusPanel(SceneViewModel viewModel) {
        VBox panel = ScreenShell.styledPanel(ScreenShell.SCENE_STATUS_PANEL_STYLE_CLASS);
        for (SceneStatusRowViewModel row : viewModel.statusRows()) {
            panel.getChildren().add(new Label(row.label() + ": " + row.value()));
        }
        return panel;
    }

    private static Node dialoguePanel(SceneViewModel viewModel) {
        VBox panel = ScreenShell.styledPanel(ScreenShell.SCENE_DIALOGUE_PANEL_STYLE_CLASS);
        for (SceneDialogueRowViewModel row : viewModel.dialogueRows()) {
            String speaker = row.speakerId() == null || row.speakerId().isBlank() ? "narrator" : row.speakerId();
            panel.getChildren().add(new Label(speaker + ": " + row.textDefinition()));
            if (row.displayReference() != null && !row.displayReference().isBlank()) {
                Label display = new Label("Display: " + row.displayReference());
                display.getStyleClass().add("scene-effect-preview");
                panel.getChildren().add(display);
            }
        }
        return panel;
    }

    private static Node effectPanel(Iterable<SceneEffectPreviewViewModel> previews) {
        VBox panel = ScreenShell.styledPanel(ScreenShell.SCENE_EFFECTS_PANEL_STYLE_CLASS);
        for (SceneEffectPreviewViewModel preview : previews) {
            Label label = new Label(preview.label() + ": " + preview.value());
            label.getStyleClass().add("scene-effect-preview");
            panel.getChildren().add(label);
        }
        return panel;
    }

    private static Node choicePanel(SceneViewModel viewModel, Consumer<String> choiceHandler) {
        VBox panel = ScreenShell.styledPanel(ScreenShell.SCENE_CHOICES_PANEL_STYLE_CLASS);
        for (SceneChoiceViewModel choice : viewModel.choices()) {
            Button button = new Button(choice.textDefinition());
            button.getStyleClass().add("scene-choice-button");
            button.setDisable(!choice.available());
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> choiceHandler.accept(choice.id()));
            panel.getChildren().add(button);
            if (choice.selected()) {
                Label selected = new Label("Selected in history");
                selected.getStyleClass().add("scene-choice-state");
                panel.getChildren().add(selected);
            }
            if (!choice.available() && choice.disabledReason() != null && !choice.disabledReason().isBlank()) {
                Label disabledReason = new Label(choice.disabledReason());
                disabledReason.getStyleClass().add("scene-choice-state");
                panel.getChildren().add(disabledReason);
            }
            if (!choice.effectPreviews().isEmpty()) {
                VBox previews = new VBox(4);
                previews.setPadding(new Insets(0, 0, 0, 12));
                for (SceneEffectPreviewViewModel preview : choice.effectPreviews()) {
                    previews.getChildren().add(new Label(preview.label() + ": " + preview.value()));
                }
                panel.getChildren().add(previews);
            }
        }
        if (viewModel.choices().isEmpty()) {
            panel.getChildren().add(new Label("No choices available."));
        }
        return panel;
    }
}
