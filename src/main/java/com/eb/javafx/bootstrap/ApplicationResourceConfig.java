package com.eb.javafx.bootstrap;

import com.eb.javafx.resources.ResourceCategory;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.PathUtils;
import com.eb.javafx.util.SimpleJson;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JSON-backed application resource configuration.
 *
 * <p>The active model is a per-category {@code resourceRoots} map plus a generic {@code resources} map for named
 * application overrides and a small set of typed startup default values for app/preferences/save-load screen
 * backgrounds. Earlier flat fields ({@code categoryCodeTablesPath}, {@code imageAssetRoot},
 * {@code jsonResourceRoot}) have been replaced by the registry-driven {@code resourceRoots} entries under
 * {@link ResourceCategory#SUPPORT}, {@link ResourceCategory#IMAGES}, and {@link ResourceCategory#UI}.</p>
 */
public final class ApplicationResourceConfig {
    private static final boolean DEFAULT_DEBUG = true;
    private static final String DEFAULT_BACKGROUND_VALUE = "";

    private final boolean debug;
    private final String defaultAppBackgroundColor;
    private final String defaultAppBackgroundImage;
    private final String defaultAppBackgroundImageTransparency;
    private final String defaultPreferencesScreenBackgroundColor;
    private final String defaultPreferencesScreenBackgroundImage;
    private final String defaultPreferencesScreenBackgroundImageTransparency;
    private final String defaultSaveLoadScreenBackgroundColor;
    private final String defaultSaveLoadScreenBackgroundImage;
    private final String defaultSaveLoadScreenBackgroundImageTransparency;
    private final Map<String, String> resources;
    private final Map<ResourceCategory, List<String>> resourceRoots;

    private ApplicationResourceConfig(
            boolean debug,
            String defaultAppBackgroundColor,
            String defaultAppBackgroundImage,
            String defaultAppBackgroundImageTransparency,
            String defaultPreferencesScreenBackgroundColor,
            String defaultPreferencesScreenBackgroundImage,
            String defaultPreferencesScreenBackgroundImageTransparency,
            String defaultSaveLoadScreenBackgroundColor,
            String defaultSaveLoadScreenBackgroundImage,
            String defaultSaveLoadScreenBackgroundImageTransparency,
            Map<String, String> resources,
            Map<ResourceCategory, List<String>> resourceRoots) {
        this.debug = debug;
        this.defaultAppBackgroundColor = Validation.requireNonNull(
                defaultAppBackgroundColor, "Default app background color is required.");
        this.defaultAppBackgroundImage = Validation.requireNonNull(
                defaultAppBackgroundImage, "Default app background image is required.");
        this.defaultAppBackgroundImageTransparency = Validation.requireNonNull(
                defaultAppBackgroundImageTransparency, "Default app background image transparency is required.");
        this.defaultPreferencesScreenBackgroundColor = Validation.requireNonNull(
                defaultPreferencesScreenBackgroundColor, "Default preferences screen background color is required.");
        this.defaultPreferencesScreenBackgroundImage = Validation.requireNonNull(
                defaultPreferencesScreenBackgroundImage, "Default preferences screen background image is required.");
        this.defaultPreferencesScreenBackgroundImageTransparency = Validation.requireNonNull(
                defaultPreferencesScreenBackgroundImageTransparency,
                "Default preferences screen background image transparency is required.");
        this.defaultSaveLoadScreenBackgroundColor = Validation.requireNonNull(
                defaultSaveLoadScreenBackgroundColor, "Default save/load screen background color is required.");
        this.defaultSaveLoadScreenBackgroundImage = Validation.requireNonNull(
                defaultSaveLoadScreenBackgroundImage, "Default save/load screen background image is required.");
        this.defaultSaveLoadScreenBackgroundImageTransparency = Validation.requireNonNull(
                defaultSaveLoadScreenBackgroundImageTransparency,
                "Default save/load screen background image transparency is required.");
        LinkedHashMap<String, String> validatedResources = new LinkedHashMap<>();
        Validation.requireNonNull(resources, "Application resource config resources map is required.")
                .forEach((key, value) -> validatedResources.put(
                        Validation.requireNonBlank(key, "Application resource config resource ID is required."),
                        Validation.requireNonBlank(value, "Application resource config resource path is required.")));
        this.resources = Map.copyOf(validatedResources);
        this.resourceRoots = copyResourceRoots(Validation.requireNonNull(
                resourceRoots, "Application resource config resourceRoots map is required."));
    }

    public static ApplicationResourceConfig defaults() {
        return new ApplicationResourceConfig(
                DEFAULT_DEBUG,
                DEFAULT_BACKGROUND_VALUE, DEFAULT_BACKGROUND_VALUE, DEFAULT_BACKGROUND_VALUE,
                DEFAULT_BACKGROUND_VALUE, DEFAULT_BACKGROUND_VALUE, DEFAULT_BACKGROUND_VALUE,
                DEFAULT_BACKGROUND_VALUE, DEFAULT_BACKGROUND_VALUE, DEFAULT_BACKGROUND_VALUE,
                Map.of(),
                Map.of());
    }

    /** Returns a config carrying only the supplied generic resource overrides. */
    public static ApplicationResourceConfig of(Map<String, String> resources) {
        return defaults().withResources(resources);
    }

    public static ApplicationResourceConfig load(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Application resource config JSON path is required.");
        try {
            return fromJson(Files.readString(jsonPath, StandardCharsets.UTF_8), jsonPath.toString());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read application resource config JSON: " + jsonPath, exception);
        }
    }

    public static ApplicationResourceConfig fromJson(String json, String sourceName) {
        Map<String, Object> root = requireObject(SimpleJson.parse(json, sourceName), "root");
        ApplicationResourceConfig defaults = defaults();
        // First-class modding fields (fonts / assetOverrideRoot / windowTitle / appIcon /
        // uiTheme / themePalette) are folded into the generic `resources` map at parse time, so
        // downstream code keeps reading them by their reserved ids and the equivalent
        // `resources` entries still work for back-compat.  Explicit top-level fields win.
        Map<String, String> resources = promoteFirstClassFields(root,
                optionalObject(root, "resources", "root.resources")
                        .map(ApplicationResourceConfig::toStringMap)
                        .orElse(Map.of()));
        return new ApplicationResourceConfig(
                optionalBoolean(root, "debug", "root.debug").orElse(defaults.debug()),
                optionalStringAllowingBlank(root, "defaultAppBackgroundColor", "root.defaultAppBackgroundColor")
                        .orElse(defaults.defaultAppBackgroundColor()),
                optionalStringAllowingBlank(root, "defaultAppBackgroundImage", "root.defaultAppBackgroundImage")
                        .orElse(defaults.defaultAppBackgroundImage()),
                optionalStringAllowingBlank(root, "defaultAppBackgroundImageTransparency",
                        "root.defaultAppBackgroundImageTransparency")
                        .orElse(defaults.defaultAppBackgroundImageTransparency()),
                optionalStringAllowingBlank(root, "defaultPreferencesScreenBackgroundColor",
                        "root.defaultPreferencesScreenBackgroundColor")
                        .orElse(defaults.defaultPreferencesScreenBackgroundColor()),
                optionalStringAllowingBlank(root, "defaultPreferencesScreenBackgroundImage",
                        "root.defaultPreferencesScreenBackgroundImage")
                        .orElse(defaults.defaultPreferencesScreenBackgroundImage()),
                optionalStringAllowingBlank(root, "defaultPreferencesScreenBackgroundImageTransparency",
                        "root.defaultPreferencesScreenBackgroundImageTransparency")
                        .orElse(defaults.defaultPreferencesScreenBackgroundImageTransparency()),
                optionalStringAllowingBlank(root, "defaultSaveLoadScreenBackgroundColor",
                        "root.defaultSaveLoadScreenBackgroundColor")
                        .orElse(defaults.defaultSaveLoadScreenBackgroundColor()),
                optionalStringAllowingBlank(root, "defaultSaveLoadScreenBackgroundImage",
                        "root.defaultSaveLoadScreenBackgroundImage")
                        .orElse(defaults.defaultSaveLoadScreenBackgroundImage()),
                optionalStringAllowingBlank(root, "defaultSaveLoadScreenBackgroundImageTransparency",
                        "root.defaultSaveLoadScreenBackgroundImageTransparency")
                        .orElse(defaults.defaultSaveLoadScreenBackgroundImageTransparency()),
                resources,
                optionalObject(root, "resourceRoots", "root.resourceRoots")
                        .map(ApplicationResourceConfig::toResourceRootsMap)
                        .orElse(Map.of()));
    }

    /** Reserved single-value first-class field ids that map 1:1 onto a {@code resources} entry. */
    private static final List<String> FIRST_CLASS_STRING_FIELDS = List.of(
            "assetOverrideRoot", "windowTitle", "appIcon", "uiTheme", "themePalette");

    /**
     * Folds the explicit top-level modding fields into a copy of {@code resources}.  String
     * fields become same-named entries; the {@code fonts} array becomes {@code font.cfgN}
     * entries (so {@link com.eb.javafx.util.FontResources}-driven registration picks them up).
     * Explicit top-level values override any same-id {@code resources} entry.  Validates types.
     */
    private static Map<String, String> promoteFirstClassFields(
            Map<String, Object> root, Map<String, String> resources) {
        LinkedHashMap<String, String> merged = new LinkedHashMap<>(resources);
        for (String field : FIRST_CLASS_STRING_FIELDS) {
            optionalStringAllowingBlank(root, field, "root." + field)
                    .filter(value -> !value.isBlank())
                    .ifPresent(value -> merged.put(field, value));
        }
        if (root.containsKey("fonts")) {
            Object value = root.get("fonts");
            if (!(value instanceof List<?> list)) {
                throw new IllegalArgumentException("Expected JSON array for root.fonts.");
            }
            int index = 0;
            for (Object element : list) {
                if (!(element instanceof String fontPath) || fontPath.isBlank()) {
                    throw new IllegalArgumentException(
                            "Expected non-blank JSON string for root.fonts[" + index + "].");
                }
                merged.put("font.cfg" + index, fontPath);
                index++;
            }
        }
        promoteScreenBackgrounds(root, merged);
        promoteFooterStyle(root, merged);
        return merged;
    }

    /** Reserved {@code resources} id prefix for a footer style field, e.g. {@code footer.color}. */
    public static final String FOOTER_STYLE_PREFIX = "footer.";

    /**
     * Folds the optional top-level {@code footer} object — {@code { font, color, selectColor,
     * backgroundColor, transparency }} — into {@code resources} entries {@code footer.<field>}.
     */
    private static void promoteFooterStyle(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("footer")) {
            return;
        }
        Map<String, Object> footer = requireObject(root.get("footer"), "root.footer");
        footer.forEach((rawField, rawValue) -> {
            String field = normaliseFooterField(rawField);
            if (field == null) {
                throw new IllegalArgumentException("Unknown footer style field '" + rawField
                        + "' in root.footer (use font / color / selectColor / backgroundColor / transparency).");
            }
            if (!(rawValue instanceof String stringValue)) {
                throw new IllegalArgumentException("Expected JSON string for root.footer." + rawField + ".");
            }
            if (!stringValue.isBlank()) {
                merged.put(FOOTER_STYLE_PREFIX + field, stringValue);
            }
        });
    }

    private static String normaliseFooterField(String field) {
        return switch (field) {
            case "font", "fontFamily" -> "font";
            case "color", "textColor" -> "color";
            case "selectColor", "activeColor", "highlightColor" -> "selectColor";
            case "backgroundColor", "background" -> "backgroundColor";
            case "transparency", "opacity" -> "transparency";
            default -> null;
        };
    }

    /** Configured footer style field ({@code font} / {@code color} / {@code selectColor} /
     *  {@code backgroundColor} / {@code transparency}), if set. */
    public Optional<String> footerStyle(String field) {
        return Optional.ofNullable(resources.get(FOOTER_STYLE_PREFIX + field));
    }

    /** Reserved {@code resources} id prefix for a per-screen background field, keyed by screen
     *  (route) id, e.g. {@code screenBackground.main-menu.color}. */
    public static final String SCREEN_BACKGROUND_PREFIX = "screenBackground.";

    /**
     * Folds the optional top-level {@code screenBackgrounds} object — a per-screen (route-id)
     * map of {@code { color, image, transparency }} — into {@code resources} entries
     * {@code screenBackground.<key>.<field>}.  Field names accept friendly or long forms.
     */
    private static void promoteScreenBackgrounds(Map<String, Object> root, Map<String, String> merged) {
        if (!root.containsKey("screenBackgrounds")) {
            return;
        }
        Map<String, Object> screens = requireObject(root.get("screenBackgrounds"), "root.screenBackgrounds");
        screens.forEach((screenKey, value) -> {
            Map<String, Object> fields = requireObject(value, "root.screenBackgrounds." + screenKey);
            fields.forEach((rawField, rawValue) -> {
                String field = normaliseBackgroundField(rawField);
                if (field == null) {
                    throw new IllegalArgumentException(
                            "Unknown screen background field '" + rawField + "' in root.screenBackgrounds."
                                    + screenKey + " (use color / image / transparency).");
                }
                if (!(rawValue instanceof String stringValue)) {
                    throw new IllegalArgumentException("Expected JSON string for root.screenBackgrounds."
                            + screenKey + "." + rawField + ".");
                }
                if (!stringValue.isBlank()) {
                    merged.put(SCREEN_BACKGROUND_PREFIX + screenKey + "." + field, stringValue);
                }
            });
        });
    }

    private static String normaliseBackgroundField(String field) {
        return switch (field) {
            case "color", "backgroundColor" -> "color";
            case "image", "backgroundImage" -> "image";
            case "transparency", "imageTransparency", "backgroundImageTransparency" -> "transparency";
            default -> null;
        };
    }

    /** Per-screen background colour for {@code screenKey} (a route id), if configured. */
    public Optional<String> screenBackgroundColor(String screenKey) {
        return screenBackgroundField(screenKey, "color");
    }

    /** Per-screen background image for {@code screenKey} (a route id), if configured. */
    public Optional<String> screenBackgroundImage(String screenKey) {
        return screenBackgroundField(screenKey, "image");
    }

    /** Per-screen background image transparency for {@code screenKey} (a route id), if configured. */
    public Optional<String> screenBackgroundImageTransparency(String screenKey) {
        return screenBackgroundField(screenKey, "transparency");
    }

    private Optional<String> screenBackgroundField(String screenKey, String field) {
        if (screenKey == null || screenKey.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(resources.get(SCREEN_BACKGROUND_PREFIX + screenKey + "." + field));
    }

    public boolean debug() {
        return debug;
    }

    public String defaultAppBackgroundColor() {
        return defaultAppBackgroundColor;
    }

    public String defaultAppBackgroundImage() {
        return defaultAppBackgroundImage;
    }

    public String defaultAppBackgroundImageTransparency() {
        return defaultAppBackgroundImageTransparency;
    }

    public String defaultPreferencesScreenBackgroundColor() {
        return defaultPreferencesScreenBackgroundColor;
    }

    public String defaultPreferencesScreenBackgroundImage() {
        return defaultPreferencesScreenBackgroundImage;
    }

    public String defaultPreferencesScreenBackgroundImageTransparency() {
        return defaultPreferencesScreenBackgroundImageTransparency;
    }

    public String defaultSaveLoadScreenBackgroundColor() {
        return defaultSaveLoadScreenBackgroundColor;
    }

    public String defaultSaveLoadScreenBackgroundImage() {
        return defaultSaveLoadScreenBackgroundImage;
    }

    public String defaultSaveLoadScreenBackgroundImageTransparency() {
        return defaultSaveLoadScreenBackgroundImageTransparency;
    }

    public Map<String, String> resources() {
        return resources;
    }

    public Optional<String> resourcePath(String resourceId) {
        Validation.requireNonBlank(resourceId, "Application resource config resource ID is required.");
        return Optional.ofNullable(resources.get(resourceId));
    }

    public Optional<Path> resolveResource(Path baseDirectory, String resourceId) {
        return resourcePath(resourceId).map(path -> PathUtils.resolveChild(baseDirectory, path));
    }

    /**
     * Returns the configured per-category resource roots in declaration order. Application bootstrap layers these
     * over the library's bundled roots when building a {@code ResourceRegistry}.
     */
    public Map<ResourceCategory, List<String>> resourceRoots() {
        return resourceRoots;
    }

    public List<String> resourceRoots(ResourceCategory category) {
        Validation.requireNonNull(category, "Resource category is required.");
        return resourceRoots.getOrDefault(category, List.of());
    }

    public ApplicationResourceConfig withDebug(boolean debug) {
        return new ApplicationResourceConfig(
                debug,
                defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultAppBackgroundColor(String value) {
        return new ApplicationResourceConfig(
                debug, value, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultAppBackgroundImage(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, value, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultAppBackgroundImageTransparency(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, value,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultPreferencesScreenBackgroundColor(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                value, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultPreferencesScreenBackgroundImage(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, value,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultPreferencesScreenBackgroundImageTransparency(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage, value,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultSaveLoadScreenBackgroundColor(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                value, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultSaveLoadScreenBackgroundImage(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, value,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withDefaultSaveLoadScreenBackgroundImageTransparency(String value) {
        return new ApplicationResourceConfig(
                debug, defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage, value,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig withResources(Map<String, String> resources) {
        return new ApplicationResourceConfig(
                debug,
                defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    public ApplicationResourceConfig putResource(String resourceId, String resourcePath) {
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(resources);
        updated.put(resourceId, resourcePath);
        return withResources(updated);
    }

    public ApplicationResourceConfig removeResource(String resourceId) {
        Validation.requireNonBlank(resourceId, "Application resource config resource ID is required.");
        if (!resources.containsKey(resourceId)) {
            throw new IllegalArgumentException("Unknown application resource: " + resourceId);
        }
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(resources);
        updated.remove(resourceId);
        return withResources(updated);
    }

    /** Returns a copy with the given per-category resource roots, replacing any previously configured roots. */
    public ApplicationResourceConfig withResourceRoots(Map<ResourceCategory, List<String>> resourceRoots) {
        return new ApplicationResourceConfig(
                debug,
                defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, resourceRoots);
    }

    /** Returns a copy with one additional root spec appended to the given category's existing list. */
    public ApplicationResourceConfig withAdditionalResourceRoot(ResourceCategory category, String rootSpec) {
        Validation.requireNonNull(category, "Resource category is required.");
        Validation.requireNonBlank(rootSpec, "Resource root spec is required.");
        EnumMap<ResourceCategory, List<String>> updated = new EnumMap<>(ResourceCategory.class);
        updated.putAll(resourceRoots);
        List<String> existing = updated.getOrDefault(category, List.of());
        List<String> combined = new ArrayList<>(existing.size() + 1);
        combined.addAll(existing);
        combined.add(rootSpec);
        updated.put(category, List.copyOf(combined));
        return withResourceRoots(updated);
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
                .append("  \"debug\": ").append(debug).append(",\n")
                .append("  \"defaultAppBackgroundColor\": ").append(JsonStrings.quote(defaultAppBackgroundColor)).append(",\n")
                .append("  \"defaultAppBackgroundImage\": ").append(JsonStrings.quote(defaultAppBackgroundImage)).append(",\n")
                .append("  \"defaultAppBackgroundImageTransparency\": ")
                .append(JsonStrings.quote(defaultAppBackgroundImageTransparency)).append(",\n")
                .append("  \"defaultPreferencesScreenBackgroundColor\": ")
                .append(JsonStrings.quote(defaultPreferencesScreenBackgroundColor)).append(",\n")
                .append("  \"defaultPreferencesScreenBackgroundImage\": ")
                .append(JsonStrings.quote(defaultPreferencesScreenBackgroundImage)).append(",\n")
                .append("  \"defaultPreferencesScreenBackgroundImageTransparency\": ")
                .append(JsonStrings.quote(defaultPreferencesScreenBackgroundImageTransparency)).append(",\n")
                .append("  \"defaultSaveLoadScreenBackgroundColor\": ")
                .append(JsonStrings.quote(defaultSaveLoadScreenBackgroundColor)).append(",\n")
                .append("  \"defaultSaveLoadScreenBackgroundImage\": ")
                .append(JsonStrings.quote(defaultSaveLoadScreenBackgroundImage)).append(",\n")
                .append("  \"defaultSaveLoadScreenBackgroundImageTransparency\": ")
                .append(JsonStrings.quote(defaultSaveLoadScreenBackgroundImageTransparency)).append(",\n")
                .append("  \"resources\": {\n");
        int index = 0;
        for (Map.Entry<String, String> entry : resources.entrySet()) {
            json.append("    ")
                    .append(JsonStrings.quote(entry.getKey()))
                    .append(": ")
                    .append(JsonStrings.quote(entry.getValue()));
            if (index + 1 < resources.size()) {
                json.append(',');
            }
            json.append('\n');
            index++;
        }
        json.append("  },\n");
        json.append("  \"resourceRoots\": {\n");
        int rootIndex = 0;
        int rootCount = resourceRoots.size();
        for (Map.Entry<ResourceCategory, List<String>> entry : resourceRoots.entrySet()) {
            json.append("    ")
                    .append(JsonStrings.quote(entry.getKey().configKey()))
                    .append(": [");
            List<String> values = entry.getValue();
            for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
                json.append(JsonStrings.quote(values.get(valueIndex)));
                if (valueIndex + 1 < values.size()) {
                    json.append(", ");
                }
            }
            json.append(']');
            if (rootIndex + 1 < rootCount) {
                json.append(',');
            }
            json.append('\n');
            rootIndex++;
        }
        json.append("  }\n");
        json.append("}\n");
        return json.toString();
    }

    public void save(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Application resource config JSON path is required.");
        try {
            Files.writeString(jsonPath, toJson(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to write application resource config JSON: " + jsonPath, exception);
        }
    }

    private static Map<String, Object> requireObject(Object value, String description) {
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> {
                if (!(key instanceof String stringKey)) {
                    throw new IllegalArgumentException("JSON object key must be a string in " + description + ".");
                }
                result.put(stringKey, mapValue);
            });
            return result;
        }
        throw new IllegalArgumentException("Expected JSON object for " + description + ".");
    }

    private static Optional<String> optionalStringAllowingBlank(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key)) {
            return Optional.empty();
        }
        Object value = object.get(key);
        if (value instanceof String stringValue) {
            return Optional.of(stringValue);
        }
        throw new IllegalArgumentException("Expected JSON string for " + description + ".");
    }

    private static Optional<Boolean> optionalBoolean(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key)) {
            return Optional.empty();
        }
        Object value = object.get(key);
        if (value instanceof Boolean booleanValue) {
            return Optional.of(booleanValue);
        }
        throw new IllegalArgumentException("Expected JSON boolean for " + description + ".");
    }

    private static Optional<Map<String, Object>> optionalObject(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key)) {
            return Optional.empty();
        }
        return Optional.of(requireObject(object.get(key), description));
    }

    private static Map<String, String> toStringMap(Map<String, Object> value) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        value.forEach((key, entryValue) -> {
            if (!(entryValue instanceof String stringValue)) {
                throw new IllegalArgumentException("Expected JSON string for root.resources." + key + ".");
            }
            result.put(key, Validation.requireNonBlank(stringValue, "root.resources." + key + " must not be blank."));
        });
        return result;
    }

    private static Map<ResourceCategory, List<String>> toResourceRootsMap(Map<String, Object> value) {
        EnumMap<ResourceCategory, List<String>> result = new EnumMap<>(ResourceCategory.class);
        value.forEach((key, entryValue) -> {
            ResourceCategory category = ResourceCategory.fromConfigKey(key);
            if (!(entryValue instanceof List<?> list)) {
                throw new IllegalArgumentException(
                        "Expected JSON array for root.resourceRoots." + key + ".");
            }
            List<String> entries = new ArrayList<>();
            int index = 0;
            for (Object element : list) {
                if (!(element instanceof String stringValue) || stringValue.isBlank()) {
                    throw new IllegalArgumentException(
                            "Expected non-blank JSON string for root.resourceRoots." + key + "[" + index + "].");
                }
                entries.add(stringValue);
                index++;
            }
            result.put(category, List.copyOf(entries));
        });
        return result;
    }

    private static Map<ResourceCategory, List<String>> copyResourceRoots(
            Map<ResourceCategory, List<String>> source) {
        EnumMap<ResourceCategory, List<String>> copy = new EnumMap<>(ResourceCategory.class);
        source.forEach((category, paths) -> {
            Validation.requireNonNull(category, "Resource category is required.");
            Validation.requireNonNull(paths, "Resource roots list is required for " + category.configKey() + ".");
            List<String> validated = new ArrayList<>(paths.size());
            int index = 0;
            for (String path : paths) {
                validated.add(Validation.requireNonBlank(path,
                        "Resource root spec must not be blank in " + category.configKey() + "[" + index + "]."));
                index++;
            }
            copy.put(category, List.copyOf(validated));
        });
        return Collections.unmodifiableMap(copy);
    }
}
