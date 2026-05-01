package com.eb.javafx.scene;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Startup registry for validated structured scene definitions. */
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
        scenes.values().forEach(this::validateScene);
    }

    private void validateScene(SceneDefinition sceneDefinition) {
        Set<String> stepIds = new HashSet<>();
        for (SceneStep step : sceneDefinition.steps()) {
            if (!stepIds.add(step.id())) {
                throw new IllegalStateException("Duplicate scene step id '" + step.id() + "' in scene '"
                        + sceneDefinition.id() + "'. Step IDs must be unique within a scene.");
            }
            validateTransition(sceneDefinition.id(), step.id(), step.transition());
            if (step.type() == SceneStepType.CHOICE) {
                validateChoiceStep(sceneDefinition, step);
            }
        }
    }

    private void validateChoiceStep(SceneDefinition sceneDefinition, SceneStep step) {
        Set<String> choiceIds = new HashSet<>();
        for (SceneChoice choice : step.choices()) {
            if (!choiceIds.add(choice.id())) {
                throw new IllegalStateException("Duplicate choice id '" + choice.id() + "' in scene '"
                        + sceneDefinition.id() + "' step '" + step.id() + "'. Choice IDs must be unique within a choice step.");
            }
            validateTransition(sceneDefinition.id(), step.id(), choice.transition());
        }
    }

    private void validateTransition(String sceneId, String stepId, SceneTransition transition) {
        if ((transition.type() == SceneTransitionType.JUMP || transition.type() == SceneTransitionType.CALL)
                && !scenes.containsKey(transition.targetSceneId())) {
            throw new IllegalStateException("Scene '" + sceneId + "' step '" + stepId
                    + "' has " + transition.type() + " transition to missing scene '"
                    + transition.targetSceneId() + "'. Register that scene or change the transition target.");
        }
    }
}
