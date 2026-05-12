package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionEffect;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * One typed command in a structured reusable scene definition.
 *
 * <p>Factory methods create dialogue, narration, choice, action, and transition steps with shape validation;
 * omitted transitions default to advancing to the next step.</p>
 */
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

    public static SceneStep conditional(String id, String conditionExpression, SceneTransition thenTransition, SceneTransition elseTransition) {
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("conditionExpression", Validation.requireNonBlank(conditionExpression, "conditionExpression"));
        meta.put("elseTransitionType", Objects.requireNonNull(elseTransition, "elseTransition").type().name());
        if (elseTransition.targetSceneId() != null) {
            meta.put("elseTransitionTarget", elseTransition.targetSceneId());
        }
        return new SceneStep(id, SceneStepType.CONDITIONAL, null, null, null,
                List.of(), List.of(),
                Objects.requireNonNull(thenTransition, "thenTransition"),
                meta);
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

    public SceneStep withChoices(List<SceneChoice> choices) {
        return new SceneStep(id, type, speakerId, textDefinition, displayReference, choices, effects, transition, metadata);
    }

    public SceneStep withChoice(String choiceId, UnaryOperator<SceneChoice> mutator) {
        String checkedChoiceId = Validation.requireNonBlank(choiceId, "Scene choice id is required.");
        Objects.requireNonNull(mutator, "mutator");
        return withChoices(choices.stream()
                .map(choice -> choice.id().equals(checkedChoiceId) ? mutator.apply(choice) : choice)
                .toList());
    }

    public SceneStep withDisplayReference(String displayReference) {
        return new SceneStep(id, type, speakerId, textDefinition, displayReference, choices, effects, transition, metadata);
    }

    public SceneStep withMetadata(Map<String, String> metadata) {
        return new SceneStep(id, type, speakerId, textDefinition, displayReference, choices, effects, transition, metadata);
    }

    public SceneStep withMetadataValue(String key, String value) {
        String checkedKey = Validation.requireNonBlank(key, "Scene metadata key is required.");
        Map<String, String> updatedMetadata = new LinkedHashMap<>(metadata);
        updatedMetadata.put(checkedKey, Validation.requireNonNull(value, "Scene metadata value is required."));
        return withMetadata(updatedMetadata);
    }

    public String conditionExpression() {
        return metadata.get("conditionExpression");
    }

    public SceneTransition elseTransition() {
        String typeStr = metadata.get("elseTransitionType");
        if (typeStr == null) return SceneTransition.next();
        SceneTransitionType type = SceneTransitionType.valueOf(typeStr);
        String target = metadata.get("elseTransitionTarget");
        return switch (type) {
            case NEXT -> SceneTransition.next();
            case JUMP -> SceneTransition.jump(target);
            case CALL -> SceneTransition.call(target);
            case RETURN -> SceneTransition.returnToCaller();
            case COMPLETE -> SceneTransition.complete();
            case FAIL -> SceneTransition.fail(target);
        };
    }

    public SceneDisplayMode displayMode() {
        String value = metadata.get("displayMode");
        if (value == null) return SceneDisplayMode.ADV;
        return SceneDisplayMode.valueOf(value);
    }

    public SceneStep withDisplayMode(SceneDisplayMode mode) {
        return withMetadataValue("displayMode", Objects.requireNonNull(mode, "mode").name());
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
