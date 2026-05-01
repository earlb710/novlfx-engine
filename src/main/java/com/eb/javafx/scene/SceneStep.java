package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionEffect;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** One typed command in a structured reusable scene definition. */
public final class SceneStep {
    private final String id;
    private final SceneStepType type;
    private final String speakerId;
    private final String textDefinition;
    private final String displayReference;
    private final List<SceneChoice> choices;
    private final List<ActionEffect> effects;
    private final SceneTransition transition;
    private final Map<String, String> metadata;

    private SceneStep(
            String id,
            SceneStepType type,
            String speakerId,
            String textDefinition,
            String displayReference,
            List<SceneChoice> choices,
            List<ActionEffect> effects,
            SceneTransition transition,
            Map<String, String> metadata) {
        this.id = Validation.requireNonBlank(id, "Scene step id is required.");
        this.type = Validation.requireNonNull(type, "Scene step type is required.");
        this.speakerId = speakerId;
        this.textDefinition = textDefinition;
        this.displayReference = displayReference;
        this.choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        this.effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
        this.transition = transition == null ? SceneTransition.next() : transition;
        this.metadata = ImmutableCollections.copyMap(metadata);
        validateShape();
    }

    public static SceneStep dialogue(String id, String speakerId, String textDefinition, String displayReference) {
        return new SceneStep(id, SceneStepType.DIALOGUE, speakerId,
                Validation.requireNonBlank(textDefinition, "Dialogue text definition is required."), displayReference,
                List.of(), List.of(), SceneTransition.next(), Map.of());
    }

    public static SceneStep narration(String id, String textDefinition) {
        return new SceneStep(id, SceneStepType.NARRATION, null,
                Validation.requireNonBlank(textDefinition, "Narration text definition is required."), null,
                List.of(), List.of(), SceneTransition.next(), Map.of());
    }

    public static SceneStep choice(String id, List<SceneChoice> choices) {
        return new SceneStep(id, SceneStepType.CHOICE, null, null, null,
                Validation.requireNonEmpty(choices, "Scene choice step requires choices."), List.of(), SceneTransition.next(), Map.of());
    }

    public static SceneStep action(String id, List<ActionEffect> effects, SceneTransition transition) {
        return new SceneStep(id, SceneStepType.ACTION, null, null, null,
                List.of(), effects, transition, Map.of());
    }

    public static SceneStep transition(String id, SceneTransition transition) {
        return new SceneStep(id, SceneStepType.TRANSITION, null, null, null,
                List.of(), List.of(), transition, Map.of());
    }

    public static SceneStep create(
            String id,
            SceneStepType type,
            String speakerId,
            String textDefinition,
            String displayReference,
            List<SceneChoice> choices,
            List<ActionEffect> effects,
            SceneTransition transition,
            Map<String, String> metadata) {
        return new SceneStep(id, type, speakerId, textDefinition, displayReference, choices, effects, transition, metadata);
    }

    public String id() {
        return id;
    }

    public SceneStepType type() {
        return type;
    }

    public String speakerId() {
        return speakerId;
    }

    public String textDefinition() {
        return textDefinition;
    }

    public String displayReference() {
        return displayReference;
    }

    public List<SceneChoice> choices() {
        return choices;
    }

    public List<ActionEffect> effects() {
        return effects;
    }

    public SceneTransition transition() {
        return transition;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    private void validateShape() {
        if (type == SceneStepType.CHOICE && choices.isEmpty()) {
            throw new IllegalArgumentException("Scene choice step requires choices.");
        }
        if ((type == SceneStepType.DIALOGUE || type == SceneStepType.NARRATION) && (textDefinition == null || textDefinition.isBlank())) {
            throw new IllegalArgumentException("Scene text step requires a text definition.");
        }
    }
}
