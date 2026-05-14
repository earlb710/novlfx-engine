package com.eb.javafx.ui;

import com.eb.javafx.scene.ConversationConditionVariables;
import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** JSON import/export for editable screen designs. Temporary preview items are deliberately not saved. */
public final class ScreenDesignJson {
    public static final String DEFAULT_TEXT_LANGUAGE = "en";

    /** Metadata keys emitted as JSON booleans by the serializer and accepted as booleans by the parser. */
    private static final Set<String> BOOLEAN_METADATA_KEYS = Set.of(
            "showTicks", "showLabels", "dialog", "dismissOnClickOutside", "dismissOnEscape");

    /** Metadata keys emitted as JSON numbers by the serializer and accepted as numbers by the parser. */
    private static final Set<String> NUMBER_METADATA_KEYS = Set.of(
            "transparency", "backgroundImageTransparency", "borderThickness", "min", "max", "step");

    /** Boolean string tokens accepted in metadata values (case-insensitive). */
    private static final Set<String> BOOLEAN_TRUE_TOKENS = Set.of("true", "1", "yes", "y", "on");
    private static final Set<String> BOOLEAN_FALSE_TOKENS = Set.of("false", "0", "no", "n", "off");

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
        List<ScreenDesignBlock> blocks = new ArrayList<>();
        List<ScreenDesignItem> items = new ArrayList<>();
        for (Object entry : JsonData.requiredList(root, "blocks", "screen design blocks")) {
            Map<String, Object> blockJson = JsonData.requireObject(entry, "screen design blocks[]");
            blocks.add(parseBlock(blockJson));
            for (Object itemEntry : JsonData.optionalList(blockJson, "items", "screen design block items")) {
                Map<String, Object> itemJson = JsonData.requireObject(itemEntry, "screen design block items[]");
                items.add(parseItem(itemJson, JsonData.requiredString(blockJson, "id", "screen design block id")));
            }
        }
        for (Object entry : JsonData.optionalList(root, "items", "screen design items")) {
            items.add(parseItem(JsonData.requireObject(entry, "screen design items[]"), null));
        }
        ScreenDesignModel design = new ScreenDesignModel(
                JsonData.requiredString(root, "id", "screen design id"),
                JsonData.requiredString(root, "title", "screen design title"),
                JsonData.enumValue(ScreenLayoutType.class,
                        JsonData.requiredString(root, "layoutType", "screen design layoutType"),
                        "screen design layoutType"),
                tolerantStringMap(root.get("metadata"), "screen design metadata"),
                List.copyOf(blocks),
                List.copyOf(items),
                List.of(),
                optionalString(root, "defaultColorTheme", "screen design defaultColorTheme"),
                JsonData.optionalBoolean(root, "overwriteColorTheme", false, "screen design overwriteColorTheme"));
        ScreenDesignValidator.requireValid(design, conditionVariables);
        return design;
    }

    public static String toJson(ScreenDesignModel design) {
        Validation.requireNonNull(design, "Screen design is required.");
        ScreenDesignValidator.requireValidStructureRaw(
                design.blocks(),
                design.items(),
                List.of());
        Map<String, List<ScreenDesignItem>> itemsByBlock = new LinkedHashMap<>();
        for (ScreenDesignBlock block : design.blocks()) {
            itemsByBlock.put(block.id(), new ArrayList<>());
        }
        List<ScreenDesignItem> orphanItems = new ArrayList<>();
        for (ScreenDesignItem item : design.items()) {
            List<ScreenDesignItem> bucket = itemsByBlock.get(item.blockId());
            if (bucket == null) {
                orphanItems.add(item);
            } else {
                bucket.add(item);
            }
        }
        StringBuilder json = new StringBuilder("{\n")
                .append("  \"id\": ").append(JsonStrings.quote(design.id())).append(",\n")
                .append("  \"title\": ").append(JsonStrings.quote(design.title())).append(",\n")
                .append("  \"layoutType\": ").append(JsonStrings.quote(design.layoutType().name())).append(",\n")
                .append("  \"defaultColorTheme\": ").append(JsonStrings.nullableQuote(design.defaultColorTheme())).append(",\n")
                .append("  \"overwriteColorTheme\": ").append(design.overwriteColorTheme()).append(",\n")
                .append("  \"metadata\": ").append(metadataJson(design.metadata(), "    ")).append(",\n")
                .append("  \"blocks\": [\n");
        for (int index = 0; index < design.blocks().size(); index++) {
            ScreenDesignBlock block = design.blocks().get(index);
            appendBlock(json, block, itemsByBlock.getOrDefault(block.id(), List.of()), "    ");
            if (index + 1 < design.blocks().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]");
        if (!orphanItems.isEmpty()) {
            json.append(",\n  \"items\": [\n");
            for (int index = 0; index < orphanItems.size(); index++) {
                appendItem(json, orphanItems.get(index), true, "    ");
                if (index + 1 < orphanItems.size()) {
                    json.append(',');
                }
                json.append('\n');
            }
            json.append("  ]");
        }
        json.append("\n}\n");
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
                tolerantStringMap(object.get("metadata"), "screen design block metadata"));
    }

    private static ScreenDesignItem parseItem(Map<String, Object> object, String parentBlockId) {
        ScreenDesignItemType type = JsonData.enumValue(ScreenDesignItemType.class,
                JsonData.requiredString(object, "type", "screen design item type"),
                "screen design item type");
        String blockId = optionalString(object, "blockId", "screen design item blockId");
        if (blockId == null) {
            blockId = parentBlockId;
        }
        if (blockId == null) {
            throw new IllegalArgumentException("Missing JSON string for screen design item blockId.");
        }
        Map<String, String> metadata = new LinkedHashMap<>(tolerantStringMap(object.get("metadata"), "screen design item metadata"));
        if (object.containsKey(OptionListEncoding.OPTIONS_KEY) && object.get(OptionListEncoding.OPTIONS_KEY) != null) {
            metadata.put(OptionListEncoding.OPTIONS_KEY,
                    canonicalOptions(object.get(OptionListEncoding.OPTIONS_KEY), "screen design item options"));
        }
        return new ScreenDesignItem(
                JsonData.requiredString(object, "id", "screen design item id"),
                blockId,
                type,
                optionalString(object, "label", "screen design item label"),
                optionalString(object, "text", "screen design item text"),
                optionalString(object, "value", "screen design item value"),
                optionalString(object, "defaultValue", "screen design item defaultValue"),
                optionalInteger(object, "sequence", "screen design item sequence"),
                JsonData.optionalBoolean(object, "editable", ScreenDesignItem.defaultEditable(type), "screen design item editable"),
                optionalString(object, "styleClass", "screen design item styleClass"),
                Map.copyOf(metadata));
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

    /** Accepts JSON strings, booleans, numbers, and (for the {@code options} key) string arrays, normalizing to strings. */
    private static Map<String, String> tolerantStringMap(Object value, String description) {
        if (value == null) {
            return Map.of();
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        Map<String, Object> object = JsonData.requireObject(value, description);
        object.forEach((key, entryValue) -> {
            if (entryValue == null) {
                return;
            }
            String stringValue;
            if (entryValue instanceof String string) {
                if (string.isBlank()) {
                    return;
                }
                stringValue = string;
            } else if (entryValue instanceof Boolean booleanValue) {
                stringValue = booleanValue.toString();
            } else if (entryValue instanceof Number number) {
                stringValue = numberToString(number);
            } else if (entryValue instanceof List<?> list && OptionListEncoding.OPTIONS_KEY.equals(key)) {
                stringValue = canonicalOptions(list, description + "." + key);
            } else {
                throw new IllegalArgumentException("Unsupported JSON value for " + description + "." + key + ".");
            }
            result.put(key, stringValue);
        });
        return Map.copyOf(result);
    }

    private static String canonicalOptions(Object value, String description) {
        if (value instanceof String string) {
            return OptionListEncoding.encode(OptionListEncoding.decode(string));
        }
        if (value instanceof List<?> list) {
            List<String> options = new ArrayList<>();
            int index = 0;
            for (Object entry : list) {
                if (!(entry instanceof String optionText) || optionText.isBlank()) {
                    throw new IllegalArgumentException(
                            "Expected non-blank JSON string for " + description + "[" + index + "].");
                }
                options.add(optionText);
                index++;
            }
            return OptionListEncoding.encode(options);
        }
        throw new IllegalArgumentException(
                "Expected JSON array or comma-separated string for " + description + ".");
    }

    private static String numberToString(Number number) {
        if (number instanceof Integer || number instanceof Long) {
            return number.toString();
        }
        double value = number.doubleValue();
        if (Double.isFinite(value) && value == Math.rint(value) && Math.abs(value) < 1.0e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
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
                design.metadata(), blocks, items, List.of(), design.defaultColorTheme(), design.overwriteColorTheme()),
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
                design.metadata(), blocks, items, design.temporaryItems(),
                design.defaultColorTheme(), design.overwriteColorTheme());
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
                            throw new IllegalArgumentException("Expected JSON string for screen design texts. " + key + ".");
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

    private static void appendBlock(
            StringBuilder json,
            ScreenDesignBlock block,
            List<ScreenDesignItem> blockItems,
            String indent) {
        json.append(indent).append("{\n")
                .append(indent).append("  \"id\": ").append(JsonStrings.quote(block.id())).append(",\n")
                .append(indent).append("  \"title\": ").append(JsonStrings.nullableQuote(block.title())).append(",\n")
                .append(indent).append("  \"layoutType\": ").append(block.layoutType() == null
                        ? "null"
                        : JsonStrings.quote(block.layoutType().name())).append(",\n")
                .append(indent).append("  \"parentBlockId\": ").append(JsonStrings.nullableQuote(block.parentBlockId())).append(",\n")
                .append(indent).append("  \"conditions\": ").append(stringListJson(block.conditions())).append(",\n")
                .append(indent).append("  \"styleClass\": ").append(JsonStrings.nullableQuote(block.styleClass())).append(",\n")
                .append(indent).append("  \"metadata\": ").append(metadataJson(block.metadata(), indent + "  "));
        if (!blockItems.isEmpty()) {
            json.append(",\n").append(indent).append("  \"items\": [\n");
            for (int index = 0; index < blockItems.size(); index++) {
                appendItem(json, blockItems.get(index), false, indent + "    ");
                if (index + 1 < blockItems.size()) {
                    json.append(',');
                }
                json.append('\n');
            }
            json.append(indent).append("  ]");
        }
        json.append('\n').append(indent).append('}');
    }

    private static void appendItem(StringBuilder json, ScreenDesignItem item, boolean includeBlockId, String indent) {
        json.append(indent).append("{\n")
                .append(indent).append("  \"id\": ").append(JsonStrings.quote(item.id())).append(",\n");
        if (includeBlockId) {
            json.append(indent).append("  \"blockId\": ").append(JsonStrings.quote(item.blockId())).append(",\n");
        }
        json.append(indent).append("  \"type\": ").append(JsonStrings.quote(item.type().name())).append(",\n")
                .append(indent).append("  \"label\": ").append(JsonStrings.nullableQuote(item.label())).append(",\n")
                .append(indent).append("  \"text\": ").append(JsonStrings.nullableQuote(item.text())).append(",\n")
                .append(indent).append("  \"value\": ").append(JsonStrings.nullableQuote(item.value())).append(",\n")
                .append(indent).append("  \"defaultValue\": ").append(JsonStrings.nullableQuote(item.defaultValue())).append(",\n")
                .append(indent).append("  \"sequence\": ").append(item.sequence() == null ? "null" : item.sequence()).append(",\n")
                .append(indent).append("  \"editable\": ").append(item.editable()).append(",\n")
                .append(indent).append("  \"styleClass\": ").append(JsonStrings.nullableQuote(item.styleClass())).append(",\n");
        Map<String, String> metadataWithoutOptions = item.metadata();
        String optionsValue = metadataWithoutOptions.get(OptionListEncoding.OPTIONS_KEY);
        if (optionsValue != null && !optionsValue.isBlank()) {
            json.append(indent).append("  \"options\": ").append(optionsArrayJson(optionsValue)).append(",\n");
            LinkedHashMap<String, String> filtered = new LinkedHashMap<>(metadataWithoutOptions);
            filtered.remove(OptionListEncoding.OPTIONS_KEY);
            metadataWithoutOptions = filtered;
        }
        json.append(indent).append("  \"metadata\": ").append(metadataJson(metadataWithoutOptions, indent + "  ")).append('\n')
                .append(indent).append('}');
    }

    private static String metadataJson(Map<String, String> metadata, String indent) {
        if (metadata.isEmpty()) {
            return "{}";
        }
        StringBuilder json = new StringBuilder("{\n");
        int index = 0;
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (index++ > 0) {
                json.append(",\n");
            }
            json.append(indent).append("  ")
                    .append(JsonStrings.quote(entry.getKey())).append(": ")
                    .append(metadataValueJson(entry.getKey(), entry.getValue()));
        }
        return json.append('\n').append(indent).append('}').toString();
    }

    private static String metadataValueJson(String key, String value) {
        if (BOOLEAN_METADATA_KEYS.contains(key)) {
            String token = value.trim().toLowerCase(Locale.ROOT);
            if (BOOLEAN_TRUE_TOKENS.contains(token)) {
                return "true";
            }
            if (BOOLEAN_FALSE_TOKENS.contains(token)) {
                return "false";
            }
        } else if (NUMBER_METADATA_KEYS.contains(key)) {
            String trimmed = value.trim();
            try {
                double parsed = Double.parseDouble(trimmed);
                if (Double.isFinite(parsed)) {
                    if (parsed == Math.rint(parsed) && Math.abs(parsed) < 1.0e15 && !trimmed.contains(".")) {
                        return Long.toString((long) parsed);
                    }
                    return trimmed;
                }
            } catch (NumberFormatException ignored) {
                // Fall through to string emission.
            }
        }
        return JsonStrings.quote(value);
    }

    private static String optionsArrayJson(String canonicalOptions) {
        List<String> options = OptionListEncoding.decode(canonicalOptions);
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < options.size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            json.append(JsonStrings.quote(options.get(index)));
        }
        return json.append(']').toString();
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
