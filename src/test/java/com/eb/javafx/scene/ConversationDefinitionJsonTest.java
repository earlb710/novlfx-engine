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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConversationDefinitionJsonTest {
    @Test
    void roundTripsLr2AltConversationDocumentShape() {
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
    void readsLr2AltExportedConversationJsonShape() {
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

        ConversationDefinition parsed = ConversationDefinitionJson.fromJson(json, "lr2alt sample");

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
    void allowsEmptyVariantTextLikeLr2AltExports() {
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
    void rejectsMissingLr2AltConversationFields() {
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
    void jsonConversationContentModuleProjectsDefinitionsAndScenesFromLr2AltShape() {
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
        assertEquals(SceneStepType.CHOICE,
                sceneRegistry.requireScene("sample.conversation.opening.block_0001").steps().get(4).type());
        assertEquals("[\"has_key\"]",
                sceneRegistry.requireScene("sample.conversation.opening.block_0001").steps().get(4).choices().get(0).metadata().get("conditions"));
        assertEquals("left-path",
                sceneRegistry.requireScene("sample.conversation.opening.block_0001").steps().get(4).choices().get(0).metadata().get("value"));
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
                        choice -> choice.disabled("Needs key.").withIcon("icons/key")));
        SceneChoice changedChoice = conversation.steps().get(4).choices().get(0);

        assertTrue(module.findConversationById("sample.conversation.opening.block_0001").isPresent());
        assertTrue(module.conversationById("sample.conversation.opening.block_0001").isPresent());
        assertFalse(module.conversationById("missing").isPresent());
        assertEquals("Needs key.", changedChoice.disabledReason());
        assertEquals("icons/key", changedChoice.metadata().get("icon"));
        assertEquals("icons/key", changedChoice.metadata().get("preview.icon"));
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
                                        new ConversationVariant("Left path", "left-path", 1.0, List.of("has_key")),
                                        new ConversationVariant("Right path", 1.0, List.of())))))));
    }
}
