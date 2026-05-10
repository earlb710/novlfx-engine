package com.eb.javafx.ui;

import com.eb.javafx.scene.ConversationConditionVariables;
import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON import/export for editable screen designs. Temporary preview items are deliberately not saved. */
public final class ScreenDesignJson {
    public static final String DEFAULT_TEXT_LANGUAGE = "en";

    private ScreenDesignJson() {
    }

    public static ScreenDesignModel load(Path jsonPath) {
        return load(jsonPath, ConversationConditionVariables.fixed());
    }

    public static ScreenDesignModel load(Path jsonPath, ConversationConditionVariables conditionVariables) {
        Validation.requireNonNull(jsonPath, "Screen design JSON path is required.");
        Validation.requireNonNull(conditionVariables, "Screen design condition variables are required.");
        try {
            ScreenDesignModel design = fromJson(
                    Files.readString(jsonPath, StandardCharsets.UTF_8),
                    jsonPath.toString(),
                    conditionVariables);
            Path textPath = textPathFor(jsonPath);
            if (Files.isRegularFile(textPath)) {
                return resolveTextReferences(design, loadText(textPath).texts());
            }
            return design;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read screen design JSON: " + jsonPath, exception);
        }
    }

    public static void save(Path jsonPath, ScreenDesignModel design) {
        save(jsonPath, design, DEFAULT_TEXT_LANGUAGE);
    }

    public static void save(Path jsonPath, ScreenDesignModel design, String language) {
        Validation.requireNonNull(jsonPath, "Screen design JSON path is required.");
        Validation.requireNonNull(design, "Screen design is required.");
        String checkedLanguage = Validation.requireNonBlank(language, "Screen design text language is required.");
        try {
            Path parent = jsonPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ExtractedText extractedText = extractText(design);
            Files.writeString(jsonPath, toJson(extractedText.designWithTextReferences()), StandardCharsets.UTF_8);
            Files.writeString(textPathFor(jsonPath), textJson(checkedLanguage, extractedText.texts()), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to write screen design JSON: " + jsonPath, exception);
        }
    }

    public static Path textPathFor(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Screen design JSON path is required.");
        Path fileName = jsonPath.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Screen design JSON path must include a file name.");
        }
        String name = fileName.toString();
        String textName = name.endsWith(".json")
                ? name.substring(0, name.length() - ".json".length()) + "_text.json"
                : name + "_text.json";
        Path parent = jsonPath.getParent();
        return parent == null ? Path.of(textName) : parent.resolve(textName);
    }

    public static ScreenDesignModel fromJson(String json, String sourceName) {
        return fromJson(json, sourceName, ConversationConditionVariables.fixed());
    }

    public static ScreenDesignModel fromJson(
            String json,
            String sourceName,
            ConversationConditionVariables conditionVariables) {
        Validation.requireNonNull(conditionVariables, "Screen design condition variables are required.");
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<ScreenDesignBlock> blocks = JsonData.requiredList(root, "blocks", "screen design blocks").stream()
                .map(entry -> parseBlock(JsonData.requireObject(entry, "screen design blocks[]")))
                .toList();
        List<ScreenDesignItem> items = JsonData.optionalList(root, "items", "screen design items").stream()
                .map(entry -> parseItem(JsonData.requireObject(entry, "screen design items[]")))
                .toList();
        ScreenDesignModel design = new ScreenDesignModel(
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
        ScreenDesignValidator.requireValid(design, conditionVariables);
        return design;
    }

    public static String toJson(ScreenDesignModel design) {
        Validation.requireNonNull(design, "Screen design is required.");
        ScreenDesignValidator.requireValidStructureRaw(
                design.blocks(),
                design.items(),
                List.of());
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

    public static String textJson(String language, Map<String, String> texts) {
        Validation.requireNonBlank(language, "Screen design text language is required.");
        Validation.requireNonNull(texts, "Screen design texts are required.");
        StringBuilder json = new StringBuilder("{\n")
                .append("  \"language\": ").append(JsonStrings.quote(language)).append(",\n")
                .append("  \"texts\": {\n");
        int index = 0;
        for (Map.Entry<String, String> entry : texts.entrySet()) {
            if (index++ > 0) {
                json.append(",\n");
            }
            json.append("    ").append(JsonStrings.quote(entry.getKey())).append(": ")
                    .append(JsonStrings.quote(entry.getValue()));
        }
        json.append('\n')
                .append("  }\n")
                .append("}\n");
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
                JsonData.optionalStringList(object, "conditions", "screen design block conditions"),
                optionalString(object, "styleClass", "screen design block styleClass"),
                JsonData.optionalObject(object, "metadata", "screen design block metadata")
                        .map(metadata -> JsonData.stringMap(metadata, "screen design block metadata"))
                        .orElse(Map.of()));
    }

    private static ScreenDesignItem parseItem(Map<String, Object> object) {
        ScreenDesignItemType type = JsonData.enumValue(ScreenDesignItemType.class,
                JsonData.requiredString(object, "type", "screen design item type"),
                "screen design item type");
        return new ScreenDesignItem(
                JsonData.requiredString(object, "id", "screen design item id"),
                JsonData.requiredString(object, "blockId", "screen design item blockId"),
                type,
                optionalString(object, "label", "screen design item label"),
                optionalString(object, "text", "screen design item text"),
                optionalString(object, "value", "screen design item value"),
                optionalString(object, "defaultValue", "screen design item defaultValue"),
                optionalInteger(object, "sequence", "screen design item sequence"),
                JsonData.optionalBoolean(object, "editable", ScreenDesignItem.defaultEditable(type), "screen design item editable"),
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

    private static Integer optionalInteger(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key) || object.get(key) == null) {
            return null;
        }
        Object value = object.get(key);
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        throw new IllegalArgumentException("Expected JSON number for " + description + ".");
    }

    private static ExtractedText extractText(ScreenDesignModel design) {
        LinkedHashMap<String, String> texts = new LinkedHashMap<>();
        String screenTitleId = "screen.title";
        texts.put(screenTitleId, design.title());
        List<ScreenDesignBlock> blocks = design.blocks().stream()
                .map(block -> {
                    String title = block.title();
                    if (title != null) {
                        String titleId = blockTextId(block.id(), "title");
                        texts.put(titleId, title);
                        title = titleId;
                    }
                    return new ScreenDesignBlock(block.id(), title, block.layoutType(), block.parentBlockId(),
                            block.conditions(), block.styleClass(), block.metadata());
                })
                .toList();
        List<ScreenDesignItem> items = design.items().stream()
                .map(item -> itemWithTextReferences(item, texts))
                .toList();
        return new ExtractedText(new ScreenDesignModel(design.id(), screenTitleId, design.layoutType(),
                design.metadata(), blocks, items, List.of()),
                Collections.unmodifiableMap(new LinkedHashMap<>(texts)));
    }

    private static ScreenDesignItem itemWithTextReferences(
            ScreenDesignItem item,
            LinkedHashMap<String, String> texts) {
        String label = textReference(item.label(), itemTextId(item.id(), "label"), texts);
        String text = textReference(item.text(), itemTextId(item.id(), "text"), texts);
        String defaultValue = textReference(item.defaultValue(), itemTextId(item.id(), "defaultValue"), texts);
        return new ScreenDesignItem(item.id(), item.blockId(), item.type(), label, text, item.value(),
                defaultValue, item.sequence(), item.editable(), item.styleClass(), item.metadata());
    }

    private static String textReference(String value, String textId, LinkedHashMap<String, String> texts) {
        if (value == null) {
            return null;
        }
        texts.put(textId, value);
        return textId;
    }

    private static ScreenDesignModel resolveTextReferences(ScreenDesignModel design, Map<String, String> texts) {
        List<ScreenDesignBlock> blocks = design.blocks().stream()
                .map(block -> new ScreenDesignBlock(block.id(), resolveText(block.title(), texts), block.layoutType(),
                        block.parentBlockId(), block.conditions(), block.styleClass(), block.metadata()))
                .toList();
        List<ScreenDesignItem> items = design.items().stream()
                .map(item -> new ScreenDesignItem(
                        item.id(),
                        item.blockId(),
                        item.type(),
                        resolveText(item.label(), texts),
                        resolveText(item.text(), texts),
                        item.value(),
                        resolveText(item.defaultValue(), texts),
                        item.sequence(),
                        item.editable(),
                        item.styleClass(),
                        item.metadata()))
                .toList();
        return new ScreenDesignModel(design.id(), resolveText(design.title(), texts), design.layoutType(),
                design.metadata(), blocks, items, design.temporaryItems());
    }

    private static String resolveText(String textReference, Map<String, String> texts) {
        if (textReference == null) {
            return null;
        }
        return texts.getOrDefault(textReference, textReference);
    }

    private static TextBundle loadText(Path textPath) {
        try {
            Map<String, Object> root = JsonData.rootObject(Files.readString(textPath, StandardCharsets.UTF_8),
                    textPath.toString());
            Map<String, String> texts = new LinkedHashMap<>();
            JsonData.optionalObject(root, "texts", "screen design texts")
                    .orElseThrow(() -> new IllegalArgumentException("Missing JSON object for screen design texts."))
                    .forEach((key, value) -> {
                        if (!(value instanceof String stringValue)) {
                            throw new IllegalArgumentException("Expected JSON string for screen design texts." + key + ".");
                        }
                        texts.put(key, stringValue);
                    });
            return new TextBundle(JsonData.requiredString(root, "language", "screen design text language"),
                    Map.copyOf(texts));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read screen design text JSON: " + textPath, exception);
        }
    }

    private static String blockTextId(String blockId, String field) {
        return "block." + blockId + "." + field;
    }

    private static String itemTextId(String itemId, String field) {
        return "item." + itemId + "." + field;
    }

    private static void appendBlock(StringBuilder json, ScreenDesignBlock block, String indent) {
        json.append(indent).append("{\n")
                .append(indent).append("  \"id\": ").append(JsonStrings.quote(block.id())).append(",\n")
                .append(indent).append("  \"title\": ").append(JsonStrings.nullableQuote(block.title())).append(",\n")
                .append(indent).append("  \"layoutType\": ").append(block.layoutType() == null
                        ? "null"
                        : JsonStrings.quote(block.layoutType().name())).append(",\n")
                .append(indent).append("  \"parentBlockId\": ").append(JsonStrings.nullableQuote(block.parentBlockId())).append(",\n")
                .append(indent).append("  \"conditions\": ").append(stringListJson(block.conditions())).append(",\n")
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
                .append(indent).append("  \"sequence\": ").append(item.sequence() == null ? "null" : item.sequence()).append(",\n")
                .append(indent).append("  \"editable\": ").append(item.editable()).append(",\n")
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

    private static String stringListJson(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            json.append(JsonStrings.quote(values.get(index)));
        }
        return json.append(']').toString();
    }

    private record ExtractedText(ScreenDesignModel designWithTextReferences, Map<String, String> texts) {
    }

    private record TextBundle(String language, Map<String, String> texts) {
        private TextBundle {
            Validation.requireNonBlank(language, "Screen design text language is required.");
            texts = Map.copyOf(Validation.requireNonNull(texts, "Screen design texts are required."));
        }
    }
}
