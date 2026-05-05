package com.eb.javafx.ui;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON-backed display defaults for reusable screen rendering and authoring.
 */
public record DisplayDefaults(
        Map<String, String> screen,
        Map<String, String> block,
        Map<String, Map<String, String>> items,
        Map<String, Map<String, String>> labels) {
    public static final String DEFAULT_RESOURCE = "/com/eb/javafx/ui/display-defaults.json";

    public static final String ROLE_HEADING = "heading";
    public static final String ROLE_SUBHEADING = "subheading";
    public static final String ROLE_TEXT = "text";
    public static final String ROLE_FIELD = "field";
    public static final String ROLE_BUTTON = "button";
    public static final String ROLE_FIELD_LABEL = "fieldLabel";

    private static final DisplayDefaults DEFAULTS = loadDefaults();

    public DisplayDefaults {
        screen = Map.copyOf(Validation.requireNonNull(screen, "Display defaults screen metadata is required."));
        block = Map.copyOf(Validation.requireNonNull(block, "Display defaults block metadata is required."));
        items = copyNestedMap(items, "Display defaults item defaults are required.");
        labels = copyNestedMap(labels, "Display defaults label defaults are required.");
    }

    public static DisplayDefaults defaults() {
        return DEFAULTS;
    }

    public static DisplayDefaults fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        return new DisplayDefaults(
                JsonData.optionalObject(root, "screen", "display defaults screen")
                        .map(value -> JsonData.stringMap(value, "display defaults screen"))
                        .orElse(Map.of()),
                JsonData.optionalObject(root, "block", "display defaults block")
                        .map(value -> JsonData.stringMap(value, "display defaults block"))
                        .orElse(Map.of()),
                nestedDefaults(root, "items", "display defaults items"),
                nestedDefaults(root, "labels", "display defaults labels"));
    }

    public Map<String, String> itemDefaults(String role) {
        return items.getOrDefault(Validation.requireNonBlank(role, "Display defaults item role is required."), Map.of());
    }

    public Map<String, String> labelDefaults(String role) {
        return labels.getOrDefault(Validation.requireNonBlank(role, "Display defaults label role is required."), Map.of());
    }

    private static DisplayDefaults loadDefaults() {
        try (InputStream inputStream = DisplayDefaults.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (inputStream == null) {
                throw new StartupFailureException(StartupFailureCategory.MISSING_ASSET, "Missing display defaults JSON.");
            }
            return fromJson(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), DEFAULT_RESOURCE);
        } catch (IOException exception) {
            throw new StartupFailureException(StartupFailureCategory.MISSING_ASSET, "Unable to read display defaults JSON.");
        }
    }

    private static Map<String, Map<String, String>> nestedDefaults(Map<String, Object> root, String key, String description) {
        return JsonData.optionalObject(root, key, description)
                .map(map -> {
                    LinkedHashMap<String, Map<String, String>> values = new LinkedHashMap<>();
                    map.forEach((entryKey, entryValue) -> values.put(
                            entryKey,
                            JsonData.stringMap(JsonData.requireObject(entryValue, description + "." + entryKey),
                                    description + "." + entryKey)));
                    return Map.copyOf(values);
                })
                .orElse(Map.of());
    }

    private static Map<String, Map<String, String>> copyNestedMap(
            Map<String, Map<String, String>> source,
            String message) {
        LinkedHashMap<String, Map<String, String>> copy = new LinkedHashMap<>();
        Validation.requireNonNull(source, message)
                .forEach((key, value) -> copy.put(
                        Validation.requireNonBlank(key, "Display defaults role is required."),
                        Map.copyOf(Validation.requireNonNull(value, "Display defaults role metadata is required."))));
        return Map.copyOf(copy);
    }
}
