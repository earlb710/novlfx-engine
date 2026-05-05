package com.eb.javafx.scene;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.scene.ConversationDefinition.ConversationBlock;
import com.eb.javafx.scene.ConversationDefinition.ConversationLine;
import com.eb.javafx.scene.ConversationDefinition.ConversationVariant;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertEquals("Welcome.", parsed.conversations().get(0).lines().get(0).variants().get(0).text());
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
        assertEquals("sample.conversation.opening.block_0001",
                sceneRegistry.requireScene("sample.conversation.opening.block_0001").id());
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
                                        new ConversationVariant("Choose a path.", 1.0, List.of())))))));
    }
}
