package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.RequirementResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts executor results into UI-neutral presentation data.
 *
 * <p>The presenter copies the current step, scene id, status, message, and evaluated choice availability into
 * immutable view models that can be rendered by JavaFX or asserted in tests.</p>
 */
public final class ScenePresenter {
    public SceneViewModel present(ActionContext context, SceneExecutionResult result) {
        SceneStep step = result.step();
        List<String> selectedChoiceIds = result.state().selectedChoiceIds();
        List<SceneChoiceViewModel> choices = step == null ? List.of() : step.choices().stream()
                .map(choice -> {
                    RequirementResult availability = choice.availability(context);
                    return new SceneChoiceViewModel(
                            choice.id(),
                            choice.textDefinition(),
                            availability.isAllowed(),
                            availability.reason(),
                            selectedChoiceIds.contains(choice.id()),
                            choice.metadata(),
                            previewMetadata(choice.metadata(), choice.textDefinition()));
                })
                .toList();
        List<SceneDialogueRowViewModel> dialogueRows = step == null || (step.type() != SceneStepType.DIALOGUE && step.type() != SceneStepType.NARRATION)
                ? List.of()
                : List.of(new SceneDialogueRowViewModel(step.type(), step.speakerId(), step.textDefinition(), step.displayReference()));
        List<SceneStatusRowViewModel> statusRows = statusRows(result, step, selectedChoiceIds);
        List<SceneEffectPreviewViewModel> effectPreviews = step == null
                ? List.of()
                : previewMetadata(step.metadata(), step.displayReference() == null ? step.id() : step.displayReference());
        return new SceneViewModel(
                result.status(),
                result.state().activeSceneId(),
                step == null ? null : step.id(),
                step == null ? null : step.speakerId(),
                step == null ? null : step.textDefinition(),
                step == null ? null : step.displayReference(),
                choices,
                result.message(),
                dialogueRows,
                statusRows,
                effectPreviews,
                selectedChoiceIds);
    }

    private List<SceneStatusRowViewModel> statusRows(SceneExecutionResult result, SceneStep step, List<String> selectedChoiceIds) {
        List<SceneStatusRowViewModel> rows = new ArrayList<>();
        rows.add(new SceneStatusRowViewModel("Status", result.status().name()));
        rows.add(new SceneStatusRowViewModel("Scene", result.state().activeSceneId()));
        rows.add(new SceneStatusRowViewModel("Step", step == null ? "none" : step.id()));
        if (!selectedChoiceIds.isEmpty()) {
            rows.add(new SceneStatusRowViewModel("Selected choices", String.join(", ", selectedChoiceIds)));
        }
        if (result.state().pendingUiInterruption() != null && !result.state().pendingUiInterruption().isBlank()) {
            rows.add(new SceneStatusRowViewModel("Pending interruption", result.state().pendingUiInterruption()));
        }
        if (result.message() != null && !result.message().isBlank()) {
            rows.add(new SceneStatusRowViewModel("Message", result.message()));
        }
        return List.copyOf(rows);
    }

    private List<SceneEffectPreviewViewModel> previewMetadata(Map<String, String> metadata, String fallbackValue) {
        List<SceneEffectPreviewViewModel> previews = new ArrayList<>();
        metadata.forEach((key, value) -> {
            if (key.startsWith("preview.")) {
                previews.add(new SceneEffectPreviewViewModel(key.substring("preview.".length()), value));
            }
        });
        if (previews.isEmpty() && fallbackValue != null && !fallbackValue.isBlank()) {
            previews.add(new SceneEffectPreviewViewModel("reference", fallbackValue));
        }
        return List.copyOf(previews);
    }
}
