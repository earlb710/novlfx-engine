package com.eb.javafx.scene;

import java.util.Collections;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

/**
 * Startup registry for validated structured scene definitions.
 *
 * <p>The registry preserves registered scenes by id, prevents duplicate definitions, and validates transition
 * targets before the executor attempts to follow jumps or calls.</p>
 */
public final class SceneRegistry {
    private final Map<String, SceneDefinition> scenes = new LinkedHashMap<>();

    public void register(SceneDefinition sceneDefinition) {
        if (scenes.containsKey(sceneDefinition.id())) {
            throw new IllegalArgumentException("Duplicate scene id '" + sceneDefinition.id()
                    + "' cannot be registered. Scene IDs must be unique across all scene modules.");
        }
        scenes.put(sceneDefinition.id(), sceneDefinition);
    }

    public Optional<SceneDefinition> scene(String id) {
        return Optional.ofNullable(scenes.get(id));
    }

    public SceneDefinition requireScene(String id) {
        SceneDefinition sceneDefinition = scenes.get(id);
        if (sceneDefinition == null) {
            throw new IllegalStateException("Missing scene definition '" + id
                    + "'. Register the scene before starting or linking to it.");
        }
        return sceneDefinition;
    }

    public Map<String, SceneDefinition> scenes() {
        return Collections.unmodifiableMap(scenes);
    }

    public boolean isEmpty() {
        return scenes.isEmpty();
    }

    public void validateScenes() {
        validationReport(List.of()).throwIfInvalid();
    }

    public SceneValidationReport validationReport(List<SceneReferenceValidator> referenceValidators) {
        List<SceneValidationProblem> problems = new ArrayList<>();
        List<SceneGraphSummary> summaries = new ArrayList<>();
        scenes.values().forEach(scene -> {
            validateScene(scene, referenceValidators, problems);
            summaries.add(summarizeScene(scene, problems));
        });
        return new SceneValidationReport(summaries, problems);
    }

    private void validateScene(
            SceneDefinition sceneDefinition,
            List<SceneReferenceValidator> referenceValidators,
            List<SceneValidationProblem> problems) {
        Set<String> stepIds = new HashSet<>();
        for (SceneStep step : sceneDefinition.steps()) {
            if (!stepIds.add(step.id())) {
                problems.add(SceneValidationProblem.error(sceneDefinition.id(), step.id(),
                        "Duplicate scene step id '" + step.id() + "' in scene '"
                                + sceneDefinition.id() + "'. Step IDs must be unique within a scene."));
            }
            validateTransition(sceneDefinition.id(), step.id(), step.transition(), problems);
            if (step.type() == SceneStepType.CHOICE) {
                validateChoiceStep(sceneDefinition, step, problems);
            }
            referenceValidators.forEach(validator -> problems.addAll(validator.validate(sceneDefinition, step)));
        }
    }

    private void validateChoiceStep(SceneDefinition sceneDefinition, SceneStep step, List<SceneValidationProblem> problems) {
        Set<String> choiceIds = new HashSet<>();
        for (SceneChoice choice : step.choices()) {
            if (!choiceIds.add(choice.id())) {
                problems.add(SceneValidationProblem.error(sceneDefinition.id(), step.id(),
                        "Duplicate choice id '" + choice.id() + "' in scene '"
                                + sceneDefinition.id() + "' step '" + step.id() + "'. Choice IDs must be unique within a choice step."));
            }
            validateTransition(sceneDefinition.id(), step.id(), choice.transition(), problems);
        }
    }

    private void validateTransition(
            String sceneId,
            String stepId,
            SceneTransition transition,
            List<SceneValidationProblem> problems) {
        if ((transition.type() == SceneTransitionType.JUMP || transition.type() == SceneTransitionType.CALL)
                && !scenes.containsKey(transition.targetSceneId())) {
            problems.add(SceneValidationProblem.error(sceneId, stepId,
                    "Scene '" + sceneId + "' step '" + stepId
                            + "' has " + transition.type() + " transition to missing scene '"
                            + transition.targetSceneId() + "'. Register that scene or change the transition target."));
        }
    }

    private SceneGraphSummary summarizeScene(SceneDefinition sceneDefinition, List<SceneValidationProblem> problems) {
        List<SceneStep> steps = sceneDefinition.steps();
        Map<String, Integer> indexesByStepId = new LinkedHashMap<>();
        for (int index = 0; index < steps.size(); index++) {
            indexesByStepId.putIfAbsent(steps.get(index).id(), index);
        }

        Set<Integer> reachableIndexes = new LinkedHashSet<>();
        Queue<Integer> pending = new ArrayDeque<>();
        pending.add(0);
        while (!pending.isEmpty()) {
            int index = pending.remove();
            if (index < 0 || index >= steps.size() || !reachableIndexes.add(index)) {
                continue;
            }
            SceneStep step = steps.get(index);
            enqueueTransitionTarget(sceneDefinition.id(), index, step.transition(), steps, pending, problems);
            if (step.type() == SceneStepType.CHOICE) {
                step.choices().forEach(choice ->
                        enqueueTransitionTarget(sceneDefinition.id(), index, choice.transition(), steps, pending, problems));
            }
        }

        Set<String> reachableStepIds = new LinkedHashSet<>();
        Set<String> unreachableStepIds = new LinkedHashSet<>();
        for (int index = 0; index < steps.size(); index++) {
            if (reachableIndexes.contains(index)) {
                reachableStepIds.add(steps.get(index).id());
            } else {
                unreachableStepIds.add(steps.get(index).id());
                problems.add(SceneValidationProblem.warning(sceneDefinition.id(), steps.get(index).id(),
                        "Scene '" + sceneDefinition.id() + "' step '" + steps.get(index).id()
                                + "' is unreachable from the first step."));
            }
        }
        long choiceCount = steps.stream().filter(step -> step.type() == SceneStepType.CHOICE)
                .mapToLong(step -> step.choices().size())
                .sum();
        long transitionCount = steps.stream().mapToLong(step -> 1L + step.choices().size()).sum();
        return new SceneGraphSummary(
                sceneDefinition.id(),
                steps.size(),
                (int) choiceCount,
                (int) transitionCount,
                List.copyOf(reachableStepIds),
                List.copyOf(unreachableStepIds));
    }

    private void enqueueTransitionTarget(
            String sceneId,
            int index,
            SceneTransition transition,
            List<SceneStep> steps,
            Queue<Integer> pending,
            List<SceneValidationProblem> problems) {
        if (transition.type() == SceneTransitionType.NEXT) {
            if (index + 1 < steps.size()) {
                pending.add(index + 1);
            } else {
                problems.add(SceneValidationProblem.warning(sceneId, steps.get(index).id(),
                        "Scene '" + sceneId + "' step '" + steps.get(index).id()
                                + "' has NEXT transition after the final step."));
            }
        } else if ((transition.type() == SceneTransitionType.JUMP || transition.type() == SceneTransitionType.CALL)
                && sceneId.equals(transition.targetSceneId())) {
            pending.add(0);
        }
    }
}
