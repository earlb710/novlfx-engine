package com.eb.javafx.scene;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.scene.ConversationDefinition.ConversationBlock;
import com.eb.javafx.scene.ConversationDefinition.ConversationLine;
import com.eb.javafx.scene.ConversationDefinition.ConversationVariant;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Registers an AltLife-compatible JSON conversation document as content definitions and scene definitions. */
public final class JsonConversationContentModule implements StaticContentModule, SceneModule {
    private final ConversationDefinition document;
    private final ConversationConditionVariables conditionVariables;

    public JsonConversationContentModule(Path jsonPath) {
        this(jsonPath, ConversationConditionVariables.fixed());
    }

    public JsonConversationContentModule(Path jsonPath, ConversationConditionVariables conditionVariables) {
        this(ConversationDefinitionJson.load(jsonPath, conditionVariables), conditionVariables);
    }

    public JsonConversationContentModule(URL jsonUrl) {
        this(jsonUrl, ConversationConditionVariables.fixed());
    }

    public JsonConversationContentModule(URL jsonUrl, ConversationConditionVariables conditionVariables) {
        this(ConversationDefinitionJson.load(jsonUrl, conditionVariables), conditionVariables);
    }

    public JsonConversationContentModule(ConversationDefinition document) {
        this(document, ConversationConditionVariables.fixed());
    }

    public JsonConversationContentModule(ConversationDefinition document, ConversationConditionVariables conditionVariables) {
        this.document = Validation.requireNonNull(document, "Conversation definition is required.");
        this.conditionVariables = Validation.requireNonNull(conditionVariables, "Conversation condition variables are required.");
    }

    @Override
    public void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
        contentRegistry.registerDefinitions(definitions());
    }

    @Override
    public void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
        definitions().keySet().forEach(contentRegistry::definition);
    }

    @Override
    public void registerScenes(SceneRegistry sceneRegistry) {
        scenes().forEach(sceneRegistry::register);
    }

    @Override
    public void validateScenes(SceneRegistry sceneRegistry) {
        document.conversations().forEach(conversation -> sceneRegistry.requireScene(conversation.id()));
    }

    public ConversationDefinition conversation() {
        return document;
    }

    public Optional<SceneDefinition> findConversationById(String id) {
        String checkedId = Validation.requireNonBlank(id, "Conversation id is required.");
        return document.conversations().stream()
                .filter(conversation -> conversation.id().equals(checkedId))
                .findFirst()
                .map(conversation -> SceneDefinition.of(conversation.id(), stepsFor(conversation, conditionVariables)));
    }

    public SceneDefinition requireConversationById(String id) {
        String checkedId = Validation.requireNonBlank(id, "Conversation id is required.");
        return findConversationById(checkedId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown conversation id: " + checkedId));
    }

    Map<String, String> definitions() {
        Map<String, String> definitions = new LinkedHashMap<>();
        document.conversations().forEach(conversation -> {
            definitions.put(titleDefinitionId(conversation), conversation.description());
            for (int index = 0; index < conversation.lines().size(); index++) {
                ConversationLine line = conversation.lines().get(index);
                if (line.type() == ConversationDefinition.LineType.CHOICE) {
                    for (int variantIndex = 0; variantIndex < line.variants().size(); variantIndex++) {
                        ConversationVariant variant = line.variants().get(variantIndex);
                        definitions.put(choiceDefinitionId(conversation, index, variantIndex), variant.text());
                        definitions.put(choiceTooltipDefinitionId(conversation, index, variantIndex), variant.tooltipText());
                    }
                } else {
                    definitions.put(lineDefinitionId(conversation, index), line.type().formatText(line.variants().get(0).text()));
                }
            }
        });
        return Collections.unmodifiableMap(definitions);
    }

    List<SceneDefinition> scenes() {
        return document.conversations().stream()
                .map(conversation -> SceneDefinition.of(
                        conversation.id(),
                        stepsFor(conversation, conditionVariables)))
                .toList();
    }

    private static List<SceneStep> stepsFor(
            ConversationBlock conversation,
            ConversationConditionVariables conditionVariables) {
        return java.util.stream.IntStream.range(0, conversation.lines().size())
                .mapToObj(index -> stepFor(conversation, index, conditionVariables))
                .toList();
    }

    private static SceneStep stepFor(
            ConversationBlock conversation,
            int lineIndex,
            ConversationConditionVariables conditionVariables) {
        ConversationLine line = conversation.lines().get(lineIndex);
        if (line.type() == ConversationDefinition.LineType.CHOICE) {
            return SceneStep.create(
                    stepId(lineIndex),
                    SceneStepType.CHOICE,
                    null,
                    null,
                    null,
                    choicesFor(conversation, lineIndex, conditionVariables),
                    List.of(),
                    transitionFor(conversation, lineIndex),
                    Map.of("lineType", line.type().jsonValue()));
        }
        String definitionId = lineDefinitionId(conversation, lineIndex);
        if ("narrator".equalsIgnoreCase(line.speaker())) {
            return SceneStep.create(
                    stepId(lineIndex),
                    SceneStepType.NARRATION,
                    null,
                    definitionId,
                    null,
                    List.of(),
                    List.of(),
                    transitionFor(conversation, lineIndex),
                    Map.of("speaker", line.speaker(), "lineType", line.type().jsonValue()));
        }
        return SceneStep.create(
                stepId(lineIndex),
                SceneStepType.DIALOGUE,
                line.speaker(),
                definitionId,
                null,
                List.of(),
                List.of(),
                transitionFor(conversation, lineIndex),
                Map.of("lineType", line.type().jsonValue()));
    }

    private static List<SceneChoice> choicesFor(
            ConversationBlock conversation,
            int lineIndex,
            ConversationConditionVariables conditionVariables) {
        ConversationLine line = conversation.lines().get(lineIndex);
        return java.util.stream.IntStream.range(0, line.variants().size())
                .mapToObj(index -> choiceFor(conversation, lineIndex, index, line.variants().get(index), conditionVariables))
                .toList();
    }

    private static SceneChoice choiceFor(
            ConversationBlock conversation,
            int lineIndex,
            int variantIndex,
            ConversationVariant variant,
            ConversationConditionVariables conditionVariables) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("value", choiceValue(variant, variantIndex));
        if (!variant.conditions().isEmpty()) {
            metadata.put("conditions", conditionsJson(variant.conditions(), conditionVariables));
        }
        return new SceneChoice(
                choiceId(lineIndex, variantIndex),
                choiceDefinitionId(conversation, lineIndex, variantIndex),
                choiceTooltipDefinitionId(conversation, lineIndex, variantIndex),
                List.of(),
                List.of(),
                null,
                SceneTransition.next(),
                metadata);
    }

    private static String choiceValue(ConversationVariant variant, int variantIndex) {
        // AltLife exports and draft editor rows may omit choice values; use the zero-based JSON variant index so every runtime choice still returns a stable authored value.
        return variant.value().isEmpty() ? Integer.toString(variantIndex) : variant.value();
    }

    private static SceneTransition transitionFor(ConversationBlock conversation, int lineIndex) {
        return lineIndex + 1 >= conversation.lines().size() ? SceneTransition.complete() : SceneTransition.next();
    }

    static String titleDefinitionId(ConversationBlock conversation) {
        return conversation.id() + ".title";
    }

    static String lineDefinitionId(ConversationBlock conversation, int lineIndex) {
        return conversation.id() + ".line." + String.format("%04d", lineIndex + 1);
    }

    static String choiceDefinitionId(ConversationBlock conversation, int lineIndex, int variantIndex) {
        return lineDefinitionId(conversation, lineIndex) + ".choice." + String.format("%04d", variantIndex + 1);
    }

    static String choiceTooltipDefinitionId(ConversationBlock conversation, int lineIndex, int variantIndex) {
        return choiceDefinitionId(conversation, lineIndex, variantIndex) + ".tooltip";
    }

    private static String stepId(int lineIndex) {
        return "line-" + String.format("%04d", lineIndex + 1);
    }

    private static String choiceId(int lineIndex, int variantIndex) {
        return stepId(lineIndex) + "-choice-" + String.format("%04d", variantIndex + 1);
    }

    private static String conditionsJson(List<String> conditions, ConversationConditionVariables conditionVariables) {
        return conditions.stream()
                .map(condition -> ConversationConditionSyntax.replaceVariables(condition, conditionVariables))
                .map(JsonStrings::quote)
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
