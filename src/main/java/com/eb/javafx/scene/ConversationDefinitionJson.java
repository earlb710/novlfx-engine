package com.eb.javafx.scene;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** JSON import/export for conversation bundles containing content definitions and scene-flow data. */
public final class ConversationDefinitionJson {
    private ConversationDefinitionJson() {
    }

    public static ConversationDefinition load(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Conversation JSON path is required.");
        try {
            return fromJson(Files.readString(jsonPath, StandardCharsets.UTF_8), jsonPath.toString());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read conversation JSON: " + jsonPath, exception);
        }
    }

    public static void save(Path jsonPath, ConversationDefinition conversation) {
        Validation.requireNonNull(jsonPath, "Conversation JSON path is required.");
        Validation.requireNonNull(conversation, "Conversation definition is required.");
        try {
            Path parent = jsonPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(jsonPath, toJson(conversation), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to write conversation JSON: " + jsonPath, exception);
        }
    }

    public static ConversationDefinition fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<SceneDefinition> scenes = JsonData.requiredList(root, "scenes", "conversation scenes").stream()
                .map(entry -> SceneDefinitionJson.parseScene(JsonData.requireObject(entry, "conversation scenes[]")))
                .toList();
        return new ConversationDefinition(
                JsonData.requiredString(root, "id", "conversation id"),
                JsonData.requiredString(root, "titleDefinition", "conversation titleDefinition"),
                JsonData.optionalObject(root, "definitions", "conversation definitions")
                        .map(definitions -> JsonData.stringMap(definitions, "conversation definitions"))
                        .orElse(Map.of()),
                scenes,
                JsonData.optionalObject(root, "metadata", "conversation metadata")
                        .map(metadata -> JsonData.stringMap(metadata, "conversation metadata"))
                        .orElse(Map.of()));
    }

    public static String toJson(ConversationDefinition conversation) {
        Validation.requireNonNull(conversation, "Conversation definition is required.");
        StringBuilder json = new StringBuilder("{\n")
                .append("  \"id\": ").append(JsonStrings.quote(conversation.id())).append(",\n")
                .append("  \"titleDefinition\": ").append(JsonStrings.quote(conversation.titleDefinition())).append(",\n")
                .append("  \"metadata\": ").append(SceneDefinitionJson.stringMapJson(conversation.metadata())).append(",\n")
                .append("  \"definitions\": ").append(SceneDefinitionJson.stringMapJson(conversation.definitions())).append(",\n")
                .append("  \"scenes\": [\n");
        for (int index = 0; index < conversation.scenes().size(); index++) {
            SceneDefinitionJson.appendScene(json, conversation.scenes().get(index), "    ");
            if (index + 1 < conversation.scenes().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n}\n");
        return json.toString();
    }

}
