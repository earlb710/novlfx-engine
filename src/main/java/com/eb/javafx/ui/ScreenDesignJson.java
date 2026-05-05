package com.eb.javafx.ui;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** JSON import/export for editable screen designs. Temporary preview items are deliberately not saved. */
public final class ScreenDesignJson {
    private ScreenDesignJson() {
    }

    public static ScreenDesignModel load(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Screen design JSON path is required.");
        try {
            return fromJson(Files.readString(jsonPath, StandardCharsets.UTF_8), jsonPath.toString());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read screen design JSON: " + jsonPath, exception);
        }
    }

    public static void save(Path jsonPath, ScreenDesignModel design) {
        Validation.requireNonNull(jsonPath, "Screen design JSON path is required.");
        Validation.requireNonNull(design, "Screen design is required.");
        try {
            Path parent = jsonPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(jsonPath, toJson(design), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to write screen design JSON: " + jsonPath, exception);
        }
    }

    public static ScreenDesignModel fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<ScreenDesignBlock> blocks = JsonData.requiredList(root, "blocks", "screen design blocks").stream()
                .map(entry -> parseBlock(JsonData.requireObject(entry, "screen design blocks[]")))
                .toList();
        List<ScreenDesignItem> items = JsonData.optionalList(root, "items", "screen design items").stream()
                .map(entry -> parseItem(JsonData.requireObject(entry, "screen design items[]")))
                .toList();
        return new ScreenDesignModel(
                JsonData.requiredString(root, "id", "screen design id"),
                JsonData.requiredString(root, "title", "screen design title"),
                JsonData.enumValue(ScreenLayoutType.class,
                        JsonData.requiredString(root, "layoutType", "screen design layoutType"),
                        "screen design layoutType"),
                JsonData.optionalObject(root, "metadata", "screen design metadata")
                        .map(metadata -> JsonData.stringMap(metadata, "screen design metadata"))
                        .orElse(Map.of()),
                blocks,
                items,
                List.of());
    }

    public static String toJson(ScreenDesignModel design) {
        Validation.requireNonNull(design, "Screen design is required.");
        ScreenDesignValidator.requireValid(design.withoutTemporaryItems());
        StringBuilder json = new StringBuilder("{\n")
                .append("  \"id\": ").append(JsonStrings.quote(design.id())).append(",\n")
                .append("  \"title\": ").append(JsonStrings.quote(design.title())).append(",\n")
                .append("  \"layoutType\": ").append(JsonStrings.quote(design.layoutType().name())).append(",\n")
                .append("  \"metadata\": ").append(stringMapJson(design.metadata())).append(",\n")
                .append("  \"blocks\": [\n");
        for (int index = 0; index < design.blocks().size(); index++) {
            appendBlock(json, design.blocks().get(index), "    ");
            if (index + 1 < design.blocks().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ],\n  \"items\": [\n");
        for (int index = 0; index < design.items().size(); index++) {
            appendItem(json, design.items().get(index), "    ");
            if (index + 1 < design.items().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n}\n");
        return json.toString();
    }

    private static ScreenDesignBlock parseBlock(Map<String, Object> object) {
        return new ScreenDesignBlock(
                JsonData.requiredString(object, "id", "screen design block id"),
                optionalString(object, "title", "screen design block title"),
                JsonData.optionalString(object, "layoutType", "screen design block layoutType")
                        .map(layoutType -> JsonData.enumValue(ScreenLayoutType.class, layoutType, "screen design block layoutType"))
                        .orElse(null),
                optionalString(object, "parentBlockId", "screen design block parentBlockId"),
                optionalString(object, "styleClass", "screen design block styleClass"),
                JsonData.optionalObject(object, "metadata", "screen design block metadata")
                        .map(metadata -> JsonData.stringMap(metadata, "screen design block metadata"))
                        .orElse(Map.of()));
    }

    private static ScreenDesignItem parseItem(Map<String, Object> object) {
        return new ScreenDesignItem(
                JsonData.requiredString(object, "id", "screen design item id"),
                JsonData.requiredString(object, "blockId", "screen design item blockId"),
                JsonData.enumValue(ScreenDesignItemType.class,
                        JsonData.requiredString(object, "type", "screen design item type"),
                        "screen design item type"),
                optionalString(object, "label", "screen design item label"),
                optionalString(object, "text", "screen design item text"),
                optionalString(object, "value", "screen design item value"),
                optionalString(object, "defaultValue", "screen design item defaultValue"),
                optionalString(object, "styleClass", "screen design item styleClass"),
                JsonData.optionalObject(object, "metadata", "screen design item metadata")
                        .map(metadata -> JsonData.stringMap(metadata, "screen design item metadata"))
                        .orElse(Map.of()));
    }

    private static String optionalString(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key) || object.get(key) == null) {
            return null;
        }
        Object value = object.get(key);
        if (value instanceof String stringValue) {
            return stringValue.isBlank() ? null : stringValue;
        }
        throw new IllegalArgumentException("Expected JSON string for " + description + ".");
    }

    private static void appendBlock(StringBuilder json, ScreenDesignBlock block, String indent) {
        json.append(indent).append("{\n")
                .append(indent).append("  \"id\": ").append(JsonStrings.quote(block.id())).append(",\n")
                .append(indent).append("  \"title\": ").append(JsonStrings.nullableQuote(block.title())).append(",\n")
                .append(indent).append("  \"layoutType\": ").append(block.layoutType() == null
                        ? "null"
                        : JsonStrings.quote(block.layoutType().name())).append(",\n")
                .append(indent).append("  \"parentBlockId\": ").append(JsonStrings.nullableQuote(block.parentBlockId())).append(",\n")
                .append(indent).append("  \"styleClass\": ").append(JsonStrings.nullableQuote(block.styleClass())).append(",\n")
                .append(indent).append("  \"metadata\": ").append(stringMapJson(block.metadata())).append('\n')
                .append(indent).append('}');
    }

    private static void appendItem(StringBuilder json, ScreenDesignItem item, String indent) {
        json.append(indent).append("{\n")
                .append(indent).append("  \"id\": ").append(JsonStrings.quote(item.id())).append(",\n")
                .append(indent).append("  \"blockId\": ").append(JsonStrings.quote(item.blockId())).append(",\n")
                .append(indent).append("  \"type\": ").append(JsonStrings.quote(item.type().name())).append(",\n")
                .append(indent).append("  \"label\": ").append(JsonStrings.nullableQuote(item.label())).append(",\n")
                .append(indent).append("  \"text\": ").append(JsonStrings.nullableQuote(item.text())).append(",\n")
                .append(indent).append("  \"value\": ").append(JsonStrings.nullableQuote(item.value())).append(",\n")
                .append(indent).append("  \"defaultValue\": ").append(JsonStrings.nullableQuote(item.defaultValue())).append(",\n")
                .append(indent).append("  \"styleClass\": ").append(JsonStrings.nullableQuote(item.styleClass())).append(",\n")
                .append(indent).append("  \"metadata\": ").append(stringMapJson(item.metadata())).append('\n')
                .append(indent).append('}');
    }

    private static String stringMapJson(Map<String, String> values) {
        StringBuilder json = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (index++ > 0) {
                json.append(", ");
            }
            json.append(JsonStrings.quote(entry.getKey())).append(": ").append(JsonStrings.quote(entry.getValue()));
        }
        return json.append('}').toString();
    }
}
