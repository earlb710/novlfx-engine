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
            throw new IllegalArgumentException("Scene already registered: " + sceneDefinition.id());
        }
        scenes.put(sceneDefinition.id(), sceneDefinition);
    }

    public Optional<SceneDefinition> scene(String id) {
        return Optional.ofNullable(scenes.get(id));
    }

    public SceneDefinition requireScene(String id) {
        SceneDefinition sceneDefinition = scenes.get(id);
        if (sceneDefinition == null) {
            throw new IllegalStateException("Missing scene definition: " + id);
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
                throw new IllegalStateException("Duplicate scene step id in " + sceneDefinition.id() + ": " + step.id());
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
                throw new IllegalStateException("Duplicate choice id in " + sceneDefinition.id() + "/" + step.id() + ": " + choice.id());
            }
            validateTransition(sceneDefinition.id(), step.id(), choice.transition());
        }
    }

    private void validateTransition(String sceneId, String stepId, SceneTransition transition) {
        if ((transition.type() == SceneTransitionType.JUMP || transition.type() == SceneTransitionType.CALL)
                && !scenes.containsKey(transition.targetSceneId())) {
            throw new IllegalStateException("Scene " + sceneId + " step " + stepId
                    + " references missing scene: " + transition.targetSceneId());
        }
    }
}
