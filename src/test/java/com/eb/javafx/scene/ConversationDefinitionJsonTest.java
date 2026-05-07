package com.eb.javafx.scene;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.scene.ConversationDefinition.ConversationBlock;
import com.eb.javafx.scene.ConversationDefinition.ConversationLine;
import com.eb.javafx.scene.ConversationDefinition.ConversationVariant;
import com.eb.javafx.scene.ConversationDefinition.LineType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConversationDefinitionJsonTest {
    @Test
    void roundTripsAltLifeConversationDocumentShape() {
        ConversationDefinition conversation = sampleConversation();

        ConversationDefinition parsed = ConversationDefinitionJson.fromJson(
                ConversationDefinitionJson.toJson(conversation),
                "roundtrip");

        assertEquals("Sample Conversation", parsed.name());
        assertEquals("en", parsed.language());
        assertEquals(1, parsed.conversations().size());
        assertEquals("sample.conversation.opening.block_0001", parsed.conversations().get(0).id());
        assertEquals("narrator", parsed.conversations().get(0).lines().get(0).speaker());
        assertEquals("", parsed.conversations().get(0).lines().get(0).listener());
        assertEquals(LineType.SAY, parsed.conversations().get(0).lines().get(0).type());
        assertEquals("Welcome.", parsed.conversations().get(0).lines().get(0).variants().get(0).text());
        assertEquals("", parsed.conversations().get(0).lines().get(0).variants().get(0).value());
        assertEquals(1.0, parsed.conversations().get(0).lines().get(0).variants().get(0).weight());
    }

    @Test
    void savesAndLoadsConversationJsonFiles() throws Exception {
        Path tempFile = Files.createTempFile("conversation", ".json");

        ConversationDefinitionJson.save(tempFile, sampleConversation());
        ConversationDefinition loaded = ConversationDefinitionJson.load(tempFile);

        assertEquals("sample.conversation.opening.block_0001", loaded.conversations().get(0).id());
        assertTrue(Files.readString(tempFile).contains("\"conversations\""));
    }

    @Test
    void readsAltLifeExportedConversationJsonShape() {
        String json = """
                {
                  "name": "Save Warning",
                  "language": "en",
                  "conversations": [
                    {
                      "id": "game.bugfix_additions.compatibility_fix.check_save_version.block_0001",
                      "description": "Extracted dialogue block from label.",
                      "lines": [
                        {
                          "speaker": "Warning",
                          "listener": "",
                          "type": "shout",
                          "variants": [
                            {
                              "text": "You are loading a game created by a previous build ([loaded_version]).",
                              "weight": 1.0,
                              "conditions": ["has_loaded_version"]
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        ConversationDefinition parsed = ConversationDefinitionJson.fromJson(json, "altlife sample");

        assertEquals("game.bugfix_additions.compatibility_fix.check_save_version.block_0001",
                parsed.conversations().get(0).id());
        assertEquals("Warning", parsed.conversations().get(0).lines().get(0).speaker());
        assertEquals(LineType.SHOUT, parsed.conversations().get(0).lines().get(0).type());
    }

    @Test
    void defaultsMissingLineTypeToSay() {
        String json = """
                {
                  "name": "Default Type",
                  "language": "en",
                  "conversations": [{
                    "id": "game.debug.default_type.block_0001",
                    "description": "Extracted dialogue block.",
                    "lines": [{"speaker": "guide", "listener": "", "variants": [{"text": "Hello.", "weight": 1.0, "conditions": []}]}]
                  }]
                }
                """;

        ConversationDefinition parsed = ConversationDefinitionJson.fromJson(json, "default type");

        assertEquals(LineType.SAY, parsed.conversations().get(0).lines().get(0).type());
    }

    @Test
    void lineTypeFormattingEscapesTextBeforeAddingMarkup() {
        assertEquals("<b>&lt;LOOK &amp; LISTEN&gt;</b>", LineType.SHOUT.formatText("<look & listen>"));
        assertEquals("<i>&lt;quiet &amp; careful&gt;</i>", LineType.WHISPER.formatText("<QUIET & CAREFUL>"));
    }

    @Test
    void allowsEmptyVariantTextLikeAltLifeExports() {
        String json = """
                {
                  "name": "Empty Variant",
                  "language": "en",
                  "conversations": [{
                    "id": "game.debug.empty.block_0001",
                    "description": "Extracted dialogue block.",
                    "lines": [{"speaker": "string", "listener": "", "variants": [{"text": "", "weight": 1.0, "conditions": []}]}]
                  }]
                }
                """;

        ConversationDefinition parsed = ConversationDefinitionJson.fromJson(json, "empty variant");

        assertEquals("", parsed.conversations().get(0).lines().get(0).variants().get(0).text());
    }

    @Test
    void readsAndWritesChoiceVariantValues() {
        String json = """
                {
                  "name": "Choice Values",
                  "language": "en",
                  "conversations": [{
                    "id": "game.debug.choice_values.block_0001",
                    "description": "Extracted dialogue block.",
                    "lines": [{
                      "speaker": "guide",
                      "listener": "",
                      "type": "choice",
                      "variants": [
                        {"text": "Left", "value": "left-path", "weight": 1.0, "conditions": []},
                        {"text": "Right", "value": "", "weight": 1.0, "conditions": []}
                      ]
                    }]
                  }]
                }
                """;

        ConversationDefinition parsed = ConversationDefinitionJson.fromJson(json, "choice values");

        assertEquals("left-path", parsed.conversations().get(0).lines().get(0).variants().get(0).value());
        assertEquals("", parsed.conversations().get(0).lines().get(0).variants().get(1).value());
        assertTrue(ConversationDefinitionJson.toJson(parsed).contains("\"value\": \"left-path\""));
    }

    @Test
    void readsAndWritesChoiceVariantTooltipText() {
        String json = """
                {
                  "name": "Choice Tooltips",
                  "language": "en",
                  "conversations": [{
                    "id": "game.debug.choice_tooltips.block_0001",
                    "description": "Extracted dialogue block.",
                    "lines": [{
                      "speaker": "guide",
                      "listener": "",
                      "type": "choice",
                      "variants": [
                        {"text": "Left", "tooltipText": "Needs a key", "weight": 1.0, "conditions": []},
                        {"text": "Right", "tooltipText": "", "weight": 1.0, "conditions": []}
                      ]
                    }]
                  }]
                }
                """;

        ConversationDefinition parsed = ConversationDefinitionJson.fromJson(json, "choice tooltips");

        assertEquals("Needs a key", parsed.conversations().get(0).lines().get(0).variants().get(0).tooltipText());
        assertEquals("", parsed.conversations().get(0).lines().get(0).variants().get(1).tooltipText());
        assertTrue(ConversationDefinitionJson.toJson(parsed).contains("\"tooltipText\": \"Needs a key\""));
    }

    @Test
    void allowsFixedConversationVariablesInConditionValues() {
        String json = """
                {
                  "name": "Variable Conditions",
                  "language": "en",
                  "conversations": [{
                    "id": "game.debug.variable_conditions.block_0001",
                    "description": "Extracted dialogue block.",
                    "lines": [{
                      "speaker": "guide",
                      "listener": "hero",
                      "type": "choice",
                      "variants": [
                        {"text": "Ask", "weight": 1.0, "conditions": ["context=met_$line.speaker", "context=${conversation.id}_complete"]}
                      ]
                    }]
                  }]
                }
                """;

        ConversationDefinition parsed = ConversationDefinitionJson.fromJson(json, "variable conditions");

        assertEquals(List.of("context=met_$line.speaker", "context=${conversation.id}_complete"),
                parsed.conversations().get(0).lines().get(0).variants().get(0).conditions());
    }

    @Test
    void rejectsUnknownConversationVariablesInConditions() {
        String json = """
                {
                  "name": "Bad Variable Conditions",
                  "language": "en",
                  "conversations": [{
                    "id": "game.debug.bad_variable_conditions.block_0001",
                    "description": "Extracted dialogue block.",
                    "lines": [{
                      "speaker": "guide",
                      "listener": "",
                      "type": "choice",
                      "variants": [
                        {"text": "Ask", "weight": 1.0, "conditions": ["context=$unknown"]}
                      ]
                    }]
                  }]
                }
                """;

        assertThrows(IllegalArgumentException.class,
                () -> ConversationDefinitionJson.fromJson(json, "bad variable conditions"));
        assertThrows(IllegalArgumentException.class,
                () -> new ConversationVariant("Ask", 1.0, List.of("context=$text_value")));
    }

    @Test
    void rejectsMalformedConversationVariablesInConditions() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConversationVariant("Ask", 1.0, List.of("context=$")));
        assertThrows(IllegalArgumentException.class,
                () -> new ConversationVariant("Ask", 1.0, List.of("context=${line.speaker")));
    }

    @Test
    void allowsDeclaredApplicationVariablesInConditionValues() {
        String json = """
                {
                  "name": "Application Variable Conditions",
                  "language": "en",
                  "conversations": [{
                    "id": "game.debug.application_variable_conditions.block_0001",
                    "description": "Extracted dialogue block.",
                    "lines": [{
                      "speaker": "guide",
                      "listener": "hero",
                      "type": "choice",
                      "variants": [
                        {"text": "Buy", "weight": 1.0, "conditions": ["context=$money", "context=${money}_available"]}
                      ]
                    }]
                  }]
                }
                """;

        ConversationConditionVariables variables = ConversationConditionVariables.declaring(List.of("money"));
        ConversationDefinition parsed = ConversationDefinitionJson.fromJson(json, "application variable conditions", variables);

        assertEquals(List.of("context=$money", "context=${money}_available"),
                parsed.conversations().get(0).lines().get(0).variants().get(0).conditions());
        assertThrows(IllegalArgumentException.class,
                () -> ConversationDefinitionJson.fromJson(json, "application variable conditions"));
    }

    @Test
    void replacesDeclaredApplicationVariablesWithLookupHandlerValues() {
        ConversationConditionVariables variables = ConversationConditionVariables.withResolver(
                List.of("money"),
                name -> Optional.of("100"));

        assertEquals("context=100", ConversationConditionSyntax.replaceVariables("context=$money", variables));
        assertEquals("context=100_available",
                ConversationConditionSyntax.replaceVariables("context=${money}_available", variables));
        assertThrows(IllegalArgumentException.class,
                () -> ConversationConditionSyntax.replaceVariables("context=$money_available", variables));
    }

    @Test
    void jsonConversationContentModuleResolvesDeclaredApplicationVariablesInConditionMetadata() {
        String json = """
                {
                  "name": "Application Variable Projection",
                  "language": "en",
                  "conversations": [{
                    "id": "game.debug.application_variable_projection.block_0001",
                    "description": "Extracted dialogue block.",
                    "lines": [{
                      "speaker": "guide",
                      "listener": "",
                      "type": "choice",
                      "variants": [
                        {"text": "Buy", "weight": 1.0, "conditions": ["context=$money"]}
                      ]
                    }]
                  }]
                }
                """;
        ConversationConditionVariables variables = ConversationConditionVariables.withResolver(
                List.of("money"),
                name -> Optional.of("100"));
        ConversationDefinition parsed = ConversationDefinitionJson.fromJson(json, "application variable projection", variables);

        JsonConversationContentModule module = new JsonConversationContentModule(parsed, variables);

        assertEquals("[\"context=100\"]",
                module.requireConversationById("game.debug.application_variable_projection.block_0001")
                        .steps().get(0).choices().get(0).metadata().get("conditions"));
    }

    @Test
    void rejectsMissingAltLifeConversationFields() {
        String json = """
                {
                  "name": "Bad Conversation",
                  "language": "en",
                  "conversations": [{"id": "bad"}]
                }
                """;

        assertThrows(IllegalArgumentException.class, () -> ConversationDefinitionJson.fromJson(json, "bad"));
    }

    @Test
    void jsonConversationContentModuleProjectsDefinitionsAndScenesFromAltLifeShape() {
        JsonConversationContentModule module = new JsonConversationContentModule(sampleConversation());
        ContentRegistry contentRegistry = new ContentRegistry();
        ImageDisplayRegistry imageDisplayRegistry = new ImageDisplayRegistry();
        SceneRegistry sceneRegistry = new SceneRegistry();

        module.register(contentRegistry, imageDisplayRegistry);
        module.registerScenes(sceneRegistry);
        module.validate(contentRegistry, imageDisplayRegistry);
        module.validateScenes(sceneRegistry);

        assertEquals("Extracted generic conversation block.",
                contentRegistry.definition("sample.conversation.opening.block_0001.title"));
        assertEquals("Welcome.",
                contentRegistry.definition("sample.conversation.opening.block_0001.line.0001"));
        assertEquals("<b>LOUD.</b>",
                contentRegistry.definition("sample.conversation.opening.block_0001.line.0003"));
        assertEquals("<i>quiet.</i>",
                contentRegistry.definition("sample.conversation.opening.block_0001.line.0004"));
        assertEquals("Left path",
                contentRegistry.definition("sample.conversation.opening.block_0001.line.0005.choice.0001"));
        assertEquals("Needs a key",
                contentRegistry.definition("sample.conversation.opening.block_0001.line.0005.choice.0001.tooltip"));
        assertEquals(SceneStepType.CHOICE,
                sceneRegistry.requireScene("sample.conversation.opening.block_0001").steps().get(4).type());
        assertEquals("[\"has_key\"]",
                sceneRegistry.requireScene("sample.conversation.opening.block_0001").steps().get(4).choices().get(0).metadata().get("conditions"));
        assertEquals("left-path",
                sceneRegistry.requireScene("sample.conversation.opening.block_0001").steps().get(4).choices().get(0).metadata().get("value"));
        assertEquals("sample.conversation.opening.block_0001.line.0005.choice.0001.tooltip",
                sceneRegistry.requireScene("sample.conversation.opening.block_0001").steps().get(4).choices().get(0).tooltipTextDefinition());
        assertEquals("1",
                sceneRegistry.requireScene("sample.conversation.opening.block_0001").steps().get(4).choices().get(1).metadata().get("value"));
        assertEquals("sample.conversation.opening.block_0001",
                sceneRegistry.requireScene("sample.conversation.opening.block_0001").id());
    }

    @Test
    void looksUpConversationSceneByIdAndSupportsProgrammaticChoiceChanges() {
        JsonConversationContentModule module = new JsonConversationContentModule(sampleConversation());

        SceneDefinition conversation = module.requireConversationById("sample.conversation.opening.block_0001")
                .withStep("line-0005", step -> step.withChoice("line-0005-choice-0001",
                        choice -> choice.disabled("Needs key.")
                                .withIcon("icons/key")
                                .withText("Take the left path")
                                .withTooltipText("Key required")));
        SceneChoice changedChoice = conversation.steps().get(4).choices().get(0);

        assertTrue(module.findConversationById("sample.conversation.opening.block_0001").isPresent());
        assertFalse(module.findConversationById("missing").isPresent());
        assertEquals("Needs key.", changedChoice.disabledReason());
        assertEquals("icons/key", changedChoice.metadata().get("icon"));
        assertEquals("icons/key", changedChoice.metadata().get("preview.icon"));
        assertEquals("Take the left path", changedChoice.textDefinition());
        assertEquals("Key required", changedChoice.tooltipTextDefinition());
        assertEquals("left-path", changedChoice.metadata().get("value"));
    }

    private static ConversationDefinition sampleConversation() {
        return new ConversationDefinition(
                "Sample Conversation",
                "en",
                List.of(new ConversationBlock(
                        "sample.conversation.opening.block_0001",
                        "Extracted generic conversation block.",
                        List.of(
                                new ConversationLine("narrator", "", List.of(
                                        new ConversationVariant("Welcome.", 1.0, List.of()))),
                                new ConversationLine("guide", "", List.of(
                                        new ConversationVariant("Choose a path.", 1.0, List.of()))),
                                new ConversationLine("guide", "", LineType.SHOUT, List.of(
                                        new ConversationVariant("Loud.", 1.0, List.of()))),
                                new ConversationLine("guide", "", LineType.WHISPER, List.of(
                                        new ConversationVariant("QUIET.", 1.0, List.of()))),
                                new ConversationLine("guide", "", LineType.CHOICE, List.of(
                                        new ConversationVariant("Left path", "left-path", 1.0, List.of("has_key"), "Needs a key"),
                                        new ConversationVariant("Right path", 1.0, List.of())))))));
    }
}
