package com.eb.javafx.scene;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.display.ImageDisplayRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ConversationDefinitionJsonTest {
    @Test
    void roundTripsConversationDefinitionsWithScenesAndDefinitions() {
        ConversationDefinition conversation = sampleConversation();

        ConversationDefinition parsed = ConversationDefinitionJson.fromJson(
                ConversationDefinitionJson.toJson(conversation),
                "roundtrip");

        assertEquals("sample.conversation", parsed.id());
        assertEquals("sample.conversation.title", parsed.titleDefinition());
        assertEquals("Sample Conversation", parsed.definitions().get("sample.conversation.title"));
        assertEquals(2, parsed.scenes().size());
        assertEquals("sample.conversation.start", parsed.scenes().get(0).id());
        assertEquals("continue", parsed.scenes().get(0).steps().get(1).choices().get(0).id());
        assertEquals(SceneTransitionType.COMPLETE, parsed.scenes().get(1).steps().get(0).transition().type());
    }

    @Test
    void savesAndLoadsConversationJsonFiles() throws Exception {
        Path tempFile = Files.createTempFile("conversation", ".json");

        ConversationDefinitionJson.save(tempFile, sampleConversation());
        ConversationDefinition loaded = ConversationDefinitionJson.load(tempFile);

        assertEquals("sample.conversation", loaded.id());
        assertTrue(Files.readString(tempFile).contains("\"definitions\""));
    }

    @Test
    void rejectsExecutableHooksInConversationJson() {
        String json = """
                {
                  "id": "bad",
                  "titleDefinition": "bad.title",
                  "definitions": {"bad.title": "Bad", "bad.text": "Bad"},
                  "scenes": [{
                    "id": "bad.scene",
                    "entryRequirements": ["code-only"],
                    "steps": [{"id": "line", "type": "NARRATION", "textDefinition": "bad.text"}]
                  }]
                }
                """;

        assertThrows(IllegalArgumentException.class, () -> ConversationDefinitionJson.fromJson(json, "bad"));
    }

    @Test
    void jsonConversationContentModuleRegistersDefinitionsAndScenes() {
        JsonConversationContentModule module = new JsonConversationContentModule(sampleConversation());
        ContentRegistry contentRegistry = new ContentRegistry();
        ImageDisplayRegistry imageDisplayRegistry = new ImageDisplayRegistry();
        SceneRegistry sceneRegistry = new SceneRegistry();

        module.register(contentRegistry, imageDisplayRegistry);
        module.registerScenes(sceneRegistry);
        module.validate(contentRegistry, imageDisplayRegistry);
        module.validateScenes(sceneRegistry);

        assertEquals("Sample Conversation", contentRegistry.definition("sample.conversation.title"));
        assertEquals("sample.conversation.start", sceneRegistry.requireScene("sample.conversation.start").id());
    }

    private static ConversationDefinition sampleConversation() {
        Map<String, String> definitions = new LinkedHashMap<>();
        definitions.put("sample.conversation.title", "Sample Conversation");
        definitions.put("sample.conversation.intro", "Welcome.");
        definitions.put("sample.conversation.choice.continue", "Continue");
        definitions.put("sample.conversation.end", "Done.");
        SceneDefinition start = SceneDefinition.of("sample.conversation.start", List.of(
                SceneStep.narration("intro", "sample.conversation.intro"),
                SceneStep.choice("branch", List.of(SceneChoice.of(
                        "continue",
                        "sample.conversation.choice.continue",
                        SceneTransition.jump("sample.conversation.end"))))));
        SceneDefinition end = SceneDefinition.of("sample.conversation.end", List.of(
                SceneStep.create("end", SceneStepType.NARRATION, null,
                        "sample.conversation.end", null, List.of(), List.of(), SceneTransition.complete(), Map.of())));
        return new ConversationDefinition(
                "sample.conversation",
                "sample.conversation.title",
                definitions,
                List.of(start, end),
                Map.of("format", "lr2alt-scene-content"));
    }
}
