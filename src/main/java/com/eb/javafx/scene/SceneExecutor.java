package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.ActionEffect;
import com.eb.javafx.gamesupport.ActionResult;
import com.eb.javafx.gamesupport.RequirementResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Headless executor for structured dialogue, choice, action, and transition steps.
 *
 * <p>The executor advances scene state until it reaches a text display, a choice prompt, completion, or
 * failure, leaving all JavaFX rendering and user input decisions to adapters.</p>
 */
public final class SceneExecutor {
    private final SceneRegistry sceneRegistry;

    public SceneExecutor(SceneRegistry sceneRegistry) {
        this.sceneRegistry = Objects.requireNonNull(sceneRegistry, "sceneRegistry");
    }

    public SceneFlowState start(String sceneId) {
        sceneRegistry.requireScene(sceneId);
        return SceneFlowState.start(sceneId);
    }

    public SceneExecutionResult advanceUntilPause(ActionContext context, SceneFlowState state) {
        Objects.requireNonNull(context, "context");
        SceneFlowState current = Objects.requireNonNull(state, "state");
        while (true) {
            SceneDefinition scene = sceneRegistry.requireScene(current.activeSceneId());
            if (current.stepIndex() >= scene.steps().size()) {
                return complete(current, "Scene completed: " + scene.id());
            }
            SceneStep step = scene.steps().get(current.stepIndex());
            switch (step.type()) {
                case DIALOGUE, NARRATION -> {
                    return new SceneExecutionResult(SceneExecutionStatus.DISPLAYING_TEXT, current, step, List.of(), null);
                }
                case CHOICE -> {
                    List<SceneChoice> availableChoices = step.choices().stream()
                            .filter(choice -> choice.availability(context).isAllowed())
                            .toList();
                    return new SceneExecutionResult(SceneExecutionStatus.WAITING_FOR_CHOICE, current, step, availableChoices, null);
                }
                case ACTION -> {
                    ActionResult result = applyEffects(context, step.effects());
                    if (!result.success()) {
                        return fail(current, result.message());
                    }
                    current = applyTransition(current, step.transition());
                }
                case TRANSITION -> current = applyTransition(current, step.transition());
            }
        }
    }

    public SceneExecutionResult continueFromText(ActionContext context, SceneFlowState state) {
        SceneDefinition scene = sceneRegistry.requireScene(state.activeSceneId());
        SceneStep step = scene.steps().get(state.stepIndex());
        if (step.type() != SceneStepType.DIALOGUE && step.type() != SceneStepType.NARRATION) {
            throw new IllegalStateException("Scene is not paused on text: " + step.id());
        }
        return advanceUntilPause(context, applyTransition(state, step.transition()));
    }

    public SceneExecutionResult selectChoice(ActionContext context, SceneFlowState state, String choiceId) {
        SceneDefinition scene = sceneRegistry.requireScene(state.activeSceneId());
        SceneStep step = scene.steps().get(state.stepIndex());
        if (step.type() != SceneStepType.CHOICE) {
            throw new IllegalStateException("Scene is not waiting for a choice: " + step.id());
        }
        SceneChoice choice = step.choices().stream()
                .filter(candidate -> candidate.id().equals(choiceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown scene choice: " + choiceId));
        RequirementResult availability = choice.availability(context);
        if (!availability.isAllowed()) {
            return new SceneExecutionResult(SceneExecutionStatus.WAITING_FOR_CHOICE, state, step, List.of(), availability.reason());
        }
        ActionResult result = choice.applyEffects(context);
        if (!result.success()) {
            return fail(state, result.message());
        }
        List<String> selectedChoiceIds = new ArrayList<>(state.selectedChoiceIds());
        selectedChoiceIds.add(choice.id());
        SceneFlowState selectedState = new SceneFlowState(state.activeSceneId(), state.stepIndex(), state.callStack(), selectedChoiceIds, state.pendingUiInterruption());
        return advanceUntilPause(context, applyTransition(selectedState, choice.transition()));
    }

    private ActionResult applyEffects(ActionContext context, List<ActionEffect> effects) {
        ActionResult lastResult = ActionResult.noChange("No scene effects registered.");
        for (ActionEffect effect : effects) {
            lastResult = effect.apply(context);
            if (!lastResult.success()) {
                return lastResult;
            }
        }
        return lastResult;
    }

    private SceneFlowState applyTransition(SceneFlowState state, SceneTransition transition) {
        return switch (transition.type()) {
            case NEXT -> new SceneFlowState(state.activeSceneId(), state.stepIndex() + 1, state.callStack(), state.selectedChoiceIds(), state.pendingUiInterruption());
            case JUMP -> new SceneFlowState(transition.targetSceneId(), 0, state.callStack(), state.selectedChoiceIds(), state.pendingUiInterruption());
            case CALL -> {
                List<SceneReturnPoint> callStack = new ArrayList<>(state.callStack());
                callStack.add(new SceneReturnPoint(state.activeSceneId(), state.stepIndex() + 1));
                yield new SceneFlowState(transition.targetSceneId(), 0, callStack, state.selectedChoiceIds(), state.pendingUiInterruption());
            }
            case RETURN -> returnToCaller(state);
            case COMPLETE -> new SceneFlowState(state.activeSceneId(), Integer.MAX_VALUE, state.callStack(), state.selectedChoiceIds(), state.pendingUiInterruption());
            case FAIL -> new SceneFlowState(state.activeSceneId(), Integer.MAX_VALUE, state.callStack(), state.selectedChoiceIds(), transition.targetSceneId());
        };
    }

    private SceneFlowState returnToCaller(SceneFlowState state) {
        if (state.callStack().isEmpty()) {
            return new SceneFlowState(state.activeSceneId(), Integer.MAX_VALUE, state.callStack(), state.selectedChoiceIds(), state.pendingUiInterruption());
        }
        List<SceneReturnPoint> callStack = new ArrayList<>(state.callStack());
        SceneReturnPoint returnPoint = callStack.remove(callStack.size() - 1);
        return new SceneFlowState(returnPoint.sceneId(), returnPoint.stepIndex(), callStack, state.selectedChoiceIds(), state.pendingUiInterruption());
    }

    private SceneExecutionResult complete(SceneFlowState state, String message) {
        return new SceneExecutionResult(SceneExecutionStatus.COMPLETED, state, null, List.of(), message);
    }

    private SceneExecutionResult fail(SceneFlowState state, String message) {
        return new SceneExecutionResult(SceneExecutionStatus.FAILED, state, null, List.of(), message);
    }
}
