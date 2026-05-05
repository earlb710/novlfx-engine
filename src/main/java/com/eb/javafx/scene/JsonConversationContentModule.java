package com.eb.javafx.scene;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.scene.ConversationDefinition.ConversationBlock;
import com.eb.javafx.scene.ConversationDefinition.ConversationLine;
import com.eb.javafx.scene.ConversationDefinition.ConversationVariant;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Registers an LR2Alt-compatible JSON conversation document as content definitions and scene definitions. */
public final class JsonConversationContentModule implements StaticContentModule, SceneModule {
    private final ConversationDefinition document;

    public JsonConversationContentModule(Path jsonPath) {
        this(ConversationDefinitionJson.load(jsonPath));
    }

    public JsonConversationContentModule(ConversationDefinition document) {
        this.document = Validation.requireNonNull(document, "Conversation definition is required.");
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
                        stepsFor(conversation)))
                .toList();
    }

    private static List<SceneStep> stepsFor(ConversationBlock conversation) {
        return java.util.stream.IntStream.range(0, conversation.lines().size())
                .mapToObj(index -> stepFor(conversation, index))
                .toList();
    }

    private static SceneStep stepFor(ConversationBlock conversation, int lineIndex) {
        ConversationLine line = conversation.lines().get(lineIndex);
        if (line.type() == ConversationDefinition.LineType.CHOICE) {
            return SceneStep.create(
                    stepId(lineIndex),
                    SceneStepType.CHOICE,
                    null,
                    null,
                    null,
                    choicesFor(conversation, lineIndex),
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

    private static List<SceneChoice> choicesFor(ConversationBlock conversation, int lineIndex) {
        ConversationLine line = conversation.lines().get(lineIndex);
        return java.util.stream.IntStream.range(0, line.variants().size())
                .mapToObj(index -> choiceFor(conversation, lineIndex, index, line.variants().get(index)))
                .toList();
    }

    private static SceneChoice choiceFor(ConversationBlock conversation, int lineIndex, int variantIndex, ConversationVariant variant) {
        Map<String, String> metadata = variant.conditions().isEmpty()
                ? Map.of()
                : Map.of("conditions", conditionsJson(variant.conditions()));
        return new SceneChoice(
                choiceId(lineIndex, variantIndex),
                choiceDefinitionId(conversation, lineIndex, variantIndex),
                List.of(),
                List.of(),
                null,
                SceneTransition.next(),
                metadata);
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

    private static String stepId(int lineIndex) {
        return "line-" + String.format("%04d", lineIndex + 1);
    }

    private static String choiceId(int lineIndex, int variantIndex) {
        return stepId(lineIndex) + "-choice-" + String.format("%04d", variantIndex + 1);
    }

    private static String conditionsJson(List<String> conditions) {
        return conditions.stream()
                .map(JsonStrings::quote)
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
