package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.RequirementResult;

/**
 * Converts executor results into UI-neutral presentation data.
 *
 * <p>The presenter copies the current step, scene id, status, message, and evaluated choice availability into
 * immutable view models that can be rendered by JavaFX or asserted in tests.</p>
 */
public final class ScenePresenter {
    public SceneViewModel present(ActionContext context, SceneExecutionResult result) {
        SceneStep step = result.step();
        return new SceneViewModel(
                result.status(),
                result.state().activeSceneId(),
                step == null ? null : step.id(),
                step == null ? null : step.speakerId(),
                step == null ? null : step.textDefinition(),
                step == null ? null : step.displayReference(),
                step == null ? java.util.List.of() : step.choices().stream()
                        .map(choice -> {
                            RequirementResult availability = choice.availability(context);
                            return new SceneChoiceViewModel(choice.id(), choice.textDefinition(), availability.isAllowed(), availability.reason());
                        })
                        .toList(),
                result.message());
    }
}
