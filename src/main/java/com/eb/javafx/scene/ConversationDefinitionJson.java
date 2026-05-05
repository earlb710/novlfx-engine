package com.eb.javafx.scene;

import com.eb.javafx.scene.ConversationDefinition.ConversationBlock;
import com.eb.javafx.scene.ConversationDefinition.ConversationLine;
import com.eb.javafx.scene.ConversationDefinition.ConversationVariant;
import com.eb.javafx.scene.ConversationDefinition.LineType;
import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** JSON import/export for LR2Alt-compatible conversation documents. */
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
        List<ConversationBlock> conversations = JsonData.requiredList(root, "conversations", "conversations").stream()
                .map(entry -> parseConversation(JsonData.requireObject(entry, "conversations[]")))
                .toList();
        return new ConversationDefinition(
                JsonData.requiredString(root, "name", "conversation name"),
                JsonData.requiredString(root, "language", "conversation language"),
                conversations);
    }

    public static String toJson(ConversationDefinition document) {
        Validation.requireNonNull(document, "Conversation definition is required.");
        StringBuilder json = new StringBuilder("{\n")
                .append("  \"name\": ").append(JsonStrings.quote(document.name())).append(",\n")
                .append("  \"language\": ").append(JsonStrings.quote(document.language())).append(",\n")
                .append("  \"conversations\": [\n");
        for (int index = 0; index < document.conversations().size(); index++) {
            appendConversation(json, document.conversations().get(index), "    ");
            if (index + 1 < document.conversations().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n}\n");
        return json.toString();
    }

    private static ConversationBlock parseConversation(Map<String, Object> object) {
        List<ConversationLine> lines = JsonData.requiredList(object, "lines", "conversation lines").stream()
                .map(entry -> parseLine(JsonData.requireObject(entry, "conversation lines[]")))
                .toList();
        return new ConversationBlock(
                JsonData.requiredString(object, "id", "conversation id"),
                JsonData.requiredString(object, "description", "conversation description"),
                lines);
    }

    private static ConversationLine parseLine(Map<String, Object> object) {
        List<ConversationVariant> variants = JsonData.requiredList(object, "variants", "conversation line variants").stream()
                .map(entry -> parseVariant(JsonData.requireObject(entry, "conversation line variants[]")))
                .toList();
        return new ConversationLine(
                JsonData.requiredString(object, "speaker", "conversation line speaker"),
                stringAllowingEmpty(object, "listener", "conversation line listener", ""),
                lineType(object),
                variants);
    }

    private static LineType lineType(Map<String, Object> object) {
        return JsonData.optionalString(object, "type", "conversation line type")
                .map(LineType::fromJson)
                .orElse(LineType.SAY);
    }

    private static ConversationVariant parseVariant(Map<String, Object> object) {
        return new ConversationVariant(
                requiredStringAllowingEmpty(object, "text", "conversation variant text"),
                JsonData.optionalDouble(object, "weight", 1.0, "conversation variant weight"),
                JsonData.optionalStringList(object, "conditions", "conversation variant conditions"));
    }

    private static String requiredStringAllowingEmpty(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key)) {
            throw new IllegalArgumentException("Missing JSON string for " + description + ".");
        }
        return requiredStringAllowingEmpty(object.get(key), description);
    }

    private static String stringAllowingEmpty(Map<String, Object> object, String key, String description, String defaultValue) {
        if (!object.containsKey(key)) {
            return defaultValue;
        }
        return stringAllowingEmpty(object.get(key), description, defaultValue);
    }

    private static String stringAllowingEmpty(Object value, String description, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        throw new IllegalArgumentException("Expected JSON string for " + description + ".");
    }

    private static String requiredStringAllowingEmpty(Object value, String description) {
        if (value == null) {
            throw new IllegalArgumentException("Missing JSON string for " + description + ".");
        }
        if (value instanceof String stringValue) {
            return stringValue;
        }
        throw new IllegalArgumentException("Expected JSON string for " + description + ".");
    }

    private static void appendConversation(StringBuilder json, ConversationBlock conversation, String indent) {
        json.append(indent).append("{\n")
                .append(indent).append("  \"id\": ").append(JsonStrings.quote(conversation.id())).append(",\n")
                .append(indent).append("  \"description\": ").append(JsonStrings.quote(conversation.description())).append(",\n")
                .append(indent).append("  \"lines\": [\n");
        for (int index = 0; index < conversation.lines().size(); index++) {
            appendLine(json, conversation.lines().get(index), indent + "    ");
            if (index + 1 < conversation.lines().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append(indent).append("  ]\n")
                .append(indent).append('}');
    }

    private static void appendLine(StringBuilder json, ConversationLine line, String indent) {
        json.append(indent).append("{\n")
                .append(indent).append("  \"speaker\": ").append(JsonStrings.quote(line.speaker())).append(",\n")
                .append(indent).append("  \"listener\": ").append(JsonStrings.quote(line.listener())).append(",\n")
                .append(indent).append("  \"type\": ").append(JsonStrings.quote(line.type().jsonValue())).append(",\n")
                .append(indent).append("  \"variants\": [");
        for (int index = 0; index < line.variants().size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            appendVariant(json, line.variants().get(index));
        }
        json.append("]\n")
                .append(indent).append('}');
    }

    private static void appendVariant(StringBuilder json, ConversationVariant variant) {
        json.append("{\"text\": ").append(JsonStrings.quote(variant.text()))
                .append(", \"weight\": ").append(variant.weight())
                .append(", \"conditions\": [");
        for (int index = 0; index < variant.conditions().size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            json.append(JsonStrings.quote(variant.conditions().get(index)));
        }
        json.append("]}");
    }
}
