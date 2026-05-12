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
    private static volatile DisplayDefaults active = DEFAULTS;

    public DisplayDefaults {
        screen = Map.copyOf(Validation.requireNonNull(screen, "Display defaults screen metadata is required."));
        block = Map.copyOf(Validation.requireNonNull(block, "Display defaults block metadata is required."));
        items = copyNestedMap(items, "Display defaults item defaults are required.");
        labels = copyNestedMap(labels, "Display defaults label defaults are required.");
    }

    public static DisplayDefaults defaults() {
        return DEFAULTS;
    }

    /**
     * Returns the currently installed defaults — equal to {@link #defaults()} unless a theme
     * has installed a palette-resolved overlay via {@link #installActive(DisplayDefaults)}.
     */
    public static DisplayDefaults active() {
        return active;
    }

    /**
     * Installs a theme-resolved copy as the active defaults. Subsequent calls to
     * {@link com.eb.javafx.ui.ScreenDesignLayoutAdapter#toLayoutModel(ScreenDesignModel)} (and
     * other no-defaults overloads) will pick up these values.
     */
    public static void installActive(DisplayDefaults overlay) {
        active = Validation.requireNonNull(overlay, "Active display defaults overlay is required.");
    }

    /**
     * Restores {@link #defaults()} as the active instance. Tests that mutate the active
     * defaults via {@link #installActive(DisplayDefaults)} should call this in their teardown.
     */
    public static void resetActive() {
        active = DEFAULTS;
    }

    public static String defaultJson() {
        try (InputStream inputStream = DisplayDefaults.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (inputStream == null) {
                throw new StartupFailureException(
                        StartupFailureCategory.MISSING_ASSET,
                        "Missing display defaults JSON: " + DEFAULT_RESOURCE);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new StartupFailureException(
                    StartupFailureCategory.MISSING_ASSET,
                    "Unable to read display defaults JSON: " + DEFAULT_RESOURCE);
        }
    }

    public static DisplayDefaults fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        return new DisplayDefaults(
                JsonData.optionalObject(root, "screen", "display defaults screen")
                        .map(value -> stringMapAllowingBlank(value, "display defaults screen"))
                        .orElse(Map.of()),
                JsonData.optionalObject(root, "block", "display defaults block")
                        .map(value -> stringMapAllowingBlank(value, "display defaults block"))
                        .orElse(Map.of()),
                nestedDefaults(root, "items", "display defaults items"),
                nestedDefaults(root, "labels", "display defaults labels"));
    }

    /**
     * Returns a copy of this defaults instance with role and block color/background entries
     * overridden by the supplied palette-driven {@link RoleColors}. Other entries
     * (fontSize, fontStyle, borderStyle, transparency, etc.) are preserved.
     */
    public DisplayDefaults withRoleColors(RoleColors roleColors) {
        Validation.requireNonNull(roleColors, "Role colors are required.");
        LinkedHashMap<String, String> newBlock = new LinkedHashMap<>(block);
        newBlock.put("color", roleColors.blockColor());
        newBlock.put("backgroundColor", roleColors.blockBackground());
        newBlock.put("borderColor", roleColors.blockBorderColor());

        LinkedHashMap<String, Map<String, String>> newItems = new LinkedHashMap<>(items);
        newItems.put(ROLE_TEXT, overlayColors(newItems.get(ROLE_TEXT), roleColors.textColor(), roleColors.textBackground()));
        newItems.put(ROLE_HEADING, overlayColors(newItems.get(ROLE_HEADING), roleColors.headingColor(), roleColors.headingBackground()));
        newItems.put(ROLE_SUBHEADING, overlayColors(newItems.get(ROLE_SUBHEADING), roleColors.subheadingColor(), roleColors.subheadingBackground()));
        newItems.put(ROLE_FIELD, overlayColors(newItems.get(ROLE_FIELD), roleColors.fieldColor(), roleColors.fieldBackground()));
        newItems.put(ROLE_BUTTON, overlayColors(newItems.get(ROLE_BUTTON), roleColors.buttonColor(), roleColors.buttonBackground()));

        LinkedHashMap<String, Map<String, String>> newLabels = new LinkedHashMap<>(labels);
        newLabels.put(ROLE_FIELD_LABEL, overlayColor(newLabels.get(ROLE_FIELD_LABEL), roleColors.fieldLabelColor()));

        return new DisplayDefaults(screen, newBlock, newItems, newLabels);
    }

    private static Map<String, String> overlayColors(Map<String, String> base, String color, String backgroundColor) {
        LinkedHashMap<String, String> copy = new LinkedHashMap<>(base == null ? Map.of() : base);
        copy.put("color", color);
        copy.put("backgroundColor", backgroundColor);
        return copy;
    }

    private static Map<String, String> overlayColor(Map<String, String> base, String color) {
        LinkedHashMap<String, String> copy = new LinkedHashMap<>(base == null ? Map.of() : base);
        copy.put("color", color);
        return copy;
    }

    public Map<String, String> itemDefaults(String role) {
        return items.getOrDefault(Validation.requireNonBlank(role, "Display defaults item role is required."), Map.of());
    }

    public Map<String, String> labelDefaults(String role) {
        return labels.getOrDefault(Validation.requireNonBlank(role, "Display defaults label role is required."), Map.of());
    }

    private static DisplayDefaults loadDefaults() {
        return fromJson(defaultJson(), DEFAULT_RESOURCE);
    }

    private static Map<String, Map<String, String>> nestedDefaults(Map<String, Object> root, String key, String description) {
        return JsonData.optionalObject(root, key, description)
                .map(map -> {
                    LinkedHashMap<String, Map<String, String>> values = new LinkedHashMap<>();
                    map.forEach((entryKey, entryValue) -> values.put(
                            entryKey,
                            stringMapAllowingBlank(JsonData.requireObject(entryValue, description + "." + entryKey),
                                    description + "." + entryKey)));
                    return Map.copyOf(values);
                })
                .orElse(Map.of());
    }

    private static Map<String, String> stringMapAllowingBlank(Object value, String description) {
        LinkedHashMap<String, String> strings = new LinkedHashMap<>();
        JsonData.requireObject(value, description).forEach((key, entryValue) -> {
            if (!(entryValue instanceof String stringValue)) {
                throw new IllegalArgumentException("Expected JSON string for " + description + "." + key + ".");
            }
            strings.put(key, stringValue);
        });
        return Map.copyOf(strings);
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
