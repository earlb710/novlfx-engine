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
 * JSON-backed application resource locations for overrideable authored files.
 *
 * <p>Applications can keep a small external {@code config.json} file describing
 * where authored resources live, including category code tables, image roots,
 * and other named override points. Paths are stored as strings so callers can
 * resolve them relative to an application-chosen base directory.</p>
 */
public final class ApplicationResourceConfig {
    public static final String JSON_RESOURCE_ROOT_ID = "jsonResourceRoot";
    private static final boolean DEFAULT_DEBUG = true;
    private static final String DEFAULT_JSON_RESOURCE_ROOT = "resources/json";
    private static final String DEFAULT_CATEGORY_CODE_TABLES_PATH = "config/category-code-tables.en.json";
    private static final String DEFAULT_IMAGE_ASSET_ROOT = "game";
    private static final String DEFAULT_APP_BACKGROUND_COLOR = "";
    private static final String DEFAULT_APP_BACKGROUND_IMAGE = "";
    private static final String DEFAULT_APP_BACKGROUND_IMAGE_TRANSPARENCY = "";
    private static final String DEFAULT_PREFERENCES_SCREEN_BACKGROUND_COLOR = "";
    private static final String DEFAULT_PREFERENCES_SCREEN_BACKGROUND_IMAGE = "";
    private static final String DEFAULT_PREFERENCES_SCREEN_BACKGROUND_IMAGE_TRANSPARENCY = "";
    private static final String DEFAULT_SAVE_LOAD_SCREEN_BACKGROUND_COLOR = "";
    private static final String DEFAULT_SAVE_LOAD_SCREEN_BACKGROUND_IMAGE = "";
    private static final String DEFAULT_SAVE_LOAD_SCREEN_BACKGROUND_IMAGE_TRANSPARENCY = "";

    private final boolean debug;
    private final String categoryCodeTablesPath;
    private final String imageAssetRoot;
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
            String categoryCodeTablesPath,
            String imageAssetRoot,
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
        this.categoryCodeTablesPath = Validation.requireNonBlank(
                categoryCodeTablesPath,
                "Application resource config category code tables path is required.");
        this.imageAssetRoot = Validation.requireNonBlank(
                imageAssetRoot,
                "Application resource config image asset root is required.");
        this.defaultAppBackgroundColor = Validation.requireNonNull(
                defaultAppBackgroundColor,
                "Application resource config default app background color is required.");
        this.defaultAppBackgroundImage = Validation.requireNonNull(
                defaultAppBackgroundImage,
                "Application resource config default app background image is required.");
        this.defaultAppBackgroundImageTransparency = Validation.requireNonNull(
                defaultAppBackgroundImageTransparency,
                "Application resource config default app background image transparency is required.");
        this.defaultPreferencesScreenBackgroundColor = Validation.requireNonNull(
                defaultPreferencesScreenBackgroundColor,
                "Application resource config default preferences screen background color is required.");
        this.defaultPreferencesScreenBackgroundImage = Validation.requireNonNull(
                defaultPreferencesScreenBackgroundImage,
                "Application resource config default preferences screen background image is required.");
        this.defaultPreferencesScreenBackgroundImageTransparency = Validation.requireNonNull(
                defaultPreferencesScreenBackgroundImageTransparency,
                "Application resource config default preferences screen background image transparency is required.");
        this.defaultSaveLoadScreenBackgroundColor = Validation.requireNonNull(
                defaultSaveLoadScreenBackgroundColor,
                "Application resource config default save/load screen background color is required.");
        this.defaultSaveLoadScreenBackgroundImage = Validation.requireNonNull(
                defaultSaveLoadScreenBackgroundImage,
                "Application resource config default save/load screen background image is required.");
        this.defaultSaveLoadScreenBackgroundImageTransparency = Validation.requireNonNull(
                defaultSaveLoadScreenBackgroundImageTransparency,
                "Application resource config default save/load screen background image transparency is required.");
        LinkedHashMap<String, String> validatedResources = new LinkedHashMap<>();
        Validation.requireNonNull(resources, "Application resource config resources map is required.")
                .forEach((key, value) -> validatedResources.put(
                        Validation.requireNonBlank(key, "Application resource config resource ID is required."),
                        Validation.requireNonBlank(value, "Application resource config resource path is required.")));
        this.resources = Map.copyOf(validatedResources);
        this.resourceRoots = copyResourceRoots(Validation.requireNonNull(
                resourceRoots, "Application resource config resourceRoots map is required."));
    }

    private ApplicationResourceConfig(
            boolean debug,
            String categoryCodeTablesPath,
            String imageAssetRoot,
            String defaultAppBackgroundColor,
            String defaultAppBackgroundImage,
            String defaultAppBackgroundImageTransparency,
            String defaultPreferencesScreenBackgroundColor,
            String defaultPreferencesScreenBackgroundImage,
            String defaultPreferencesScreenBackgroundImageTransparency,
            String defaultSaveLoadScreenBackgroundColor,
            String defaultSaveLoadScreenBackgroundImage,
            String defaultSaveLoadScreenBackgroundImageTransparency,
            Map<String, String> resources) {
        this(debug, categoryCodeTablesPath, imageAssetRoot,
                defaultAppBackgroundColor, defaultAppBackgroundImage, defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor, defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor, defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources, Map.of());
    }

    public static ApplicationResourceConfig defaults() {
        return new ApplicationResourceConfig(
                DEFAULT_DEBUG,
                DEFAULT_CATEGORY_CODE_TABLES_PATH,
                DEFAULT_IMAGE_ASSET_ROOT,
                DEFAULT_APP_BACKGROUND_COLOR,
                DEFAULT_APP_BACKGROUND_IMAGE,
                DEFAULT_APP_BACKGROUND_IMAGE_TRANSPARENCY,
                DEFAULT_PREFERENCES_SCREEN_BACKGROUND_COLOR,
                DEFAULT_PREFERENCES_SCREEN_BACKGROUND_IMAGE,
                DEFAULT_PREFERENCES_SCREEN_BACKGROUND_IMAGE_TRANSPARENCY,
                DEFAULT_SAVE_LOAD_SCREEN_BACKGROUND_COLOR,
                DEFAULT_SAVE_LOAD_SCREEN_BACKGROUND_IMAGE,
                DEFAULT_SAVE_LOAD_SCREEN_BACKGROUND_IMAGE_TRANSPARENCY,
                Map.of());
    }

    public static ApplicationResourceConfig of(String categoryCodeTablesPath, String imageAssetRoot, Map<String, String> resources) {
        return of(
                DEFAULT_DEBUG,
                categoryCodeTablesPath,
                imageAssetRoot,
                DEFAULT_APP_BACKGROUND_COLOR,
                DEFAULT_APP_BACKGROUND_IMAGE,
                DEFAULT_APP_BACKGROUND_IMAGE_TRANSPARENCY,
                DEFAULT_PREFERENCES_SCREEN_BACKGROUND_COLOR,
                DEFAULT_PREFERENCES_SCREEN_BACKGROUND_IMAGE,
                DEFAULT_PREFERENCES_SCREEN_BACKGROUND_IMAGE_TRANSPARENCY,
                DEFAULT_SAVE_LOAD_SCREEN_BACKGROUND_COLOR,
                DEFAULT_SAVE_LOAD_SCREEN_BACKGROUND_IMAGE,
                DEFAULT_SAVE_LOAD_SCREEN_BACKGROUND_IMAGE_TRANSPARENCY,
                resources);
    }

    public static ApplicationResourceConfig of(
            boolean debug,
            String categoryCodeTablesPath,
            String imageAssetRoot,
            Map<String, String> resources) {
        return of(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                DEFAULT_APP_BACKGROUND_COLOR,
                DEFAULT_APP_BACKGROUND_IMAGE,
                DEFAULT_APP_BACKGROUND_IMAGE_TRANSPARENCY,
                DEFAULT_PREFERENCES_SCREEN_BACKGROUND_COLOR,
                DEFAULT_PREFERENCES_SCREEN_BACKGROUND_IMAGE,
                DEFAULT_PREFERENCES_SCREEN_BACKGROUND_IMAGE_TRANSPARENCY,
                DEFAULT_SAVE_LOAD_SCREEN_BACKGROUND_COLOR,
                DEFAULT_SAVE_LOAD_SCREEN_BACKGROUND_IMAGE,
                DEFAULT_SAVE_LOAD_SCREEN_BACKGROUND_IMAGE_TRANSPARENCY,
                resources);
    }

    public static ApplicationResourceConfig of(
            boolean debug,
            String categoryCodeTablesPath,
            String imageAssetRoot,
            String defaultAppBackgroundColor,
            String defaultAppBackgroundImage,
            String defaultAppBackgroundImageTransparency,
            String defaultPreferencesScreenBackgroundColor,
            String defaultPreferencesScreenBackgroundImage,
            String defaultPreferencesScreenBackgroundImageTransparency,
            String defaultSaveLoadScreenBackgroundColor,
            String defaultSaveLoadScreenBackgroundImage,
            String defaultSaveLoadScreenBackgroundImageTransparency,
            Map<String, String> resources) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public static ApplicationResourceConfig load(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Application resource config JSON path is required.");
        try {
            return fromJson(Files.readString(jsonPath, StandardCharsets.UTF_8), jsonPath.toString());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read application resource config JSON: " + jsonPath, exception);
        }
    }

    static ApplicationResourceConfig fromJson(String json, String sourceName) {
        Map<String, Object> root = requireObject(SimpleJson.parse(json, sourceName), "root");
        ApplicationResourceConfig defaults = defaults();
        return new ApplicationResourceConfig(
                optionalBoolean(root, "debug", "root.debug")
                        .orElse(defaults.debug()),
                optionalString(root, "categoryCodeTablesPath", "root.categoryCodeTablesPath")
                        .orElse(defaults.categoryCodeTablesPath()),
                optionalString(root, "imageAssetRoot", "root.imageAssetRoot")
                        .orElse(defaults.imageAssetRoot()),
                optionalStringAllowingBlank(root, "defaultAppBackgroundColor", "root.defaultAppBackgroundColor")
                        .orElse(defaults.defaultAppBackgroundColor()),
                optionalStringAllowingBlank(root, "defaultAppBackgroundImage", "root.defaultAppBackgroundImage")
                        .orElse(defaults.defaultAppBackgroundImage()),
                optionalStringAllowingBlank(root, "defaultAppBackgroundImageTransparency", "root.defaultAppBackgroundImageTransparency")
                        .orElse(defaults.defaultAppBackgroundImageTransparency()),
                optionalStringAllowingBlank(root, "defaultPreferencesScreenBackgroundColor", "root.defaultPreferencesScreenBackgroundColor")
                        .orElse(defaults.defaultPreferencesScreenBackgroundColor()),
                optionalStringAllowingBlank(root, "defaultPreferencesScreenBackgroundImage", "root.defaultPreferencesScreenBackgroundImage")
                        .orElse(defaults.defaultPreferencesScreenBackgroundImage()),
                optionalStringAllowingBlank(root, "defaultPreferencesScreenBackgroundImageTransparency", "root.defaultPreferencesScreenBackgroundImageTransparency")
                        .orElse(defaults.defaultPreferencesScreenBackgroundImageTransparency()),
                optionalStringAllowingBlank(root, "defaultSaveLoadScreenBackgroundColor", "root.defaultSaveLoadScreenBackgroundColor")
                        .orElse(defaults.defaultSaveLoadScreenBackgroundColor()),
                optionalStringAllowingBlank(root, "defaultSaveLoadScreenBackgroundImage", "root.defaultSaveLoadScreenBackgroundImage")
                        .orElse(defaults.defaultSaveLoadScreenBackgroundImage()),
                optionalStringAllowingBlank(root, "defaultSaveLoadScreenBackgroundImageTransparency", "root.defaultSaveLoadScreenBackgroundImageTransparency")
                        .orElse(defaults.defaultSaveLoadScreenBackgroundImageTransparency()),
                optionalObject(root, "resources", "root.resources")
                        .map(ApplicationResourceConfig::toStringMap)
                        .orElse(Map.of()),
                optionalObject(root, "resourceRoots", "root.resourceRoots")
                        .map(ApplicationResourceConfig::toResourceRootsMap)
                        .orElse(Map.of()));
    }

    public boolean debug() {
        return debug;
    }

    public String categoryCodeTablesPath() {
        return categoryCodeTablesPath;
    }

    public String imageAssetRoot() {
        return imageAssetRoot;
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

    /**
     * Returns the configured per-category resource roots in declaration order.
     *
     * <p>Keys are {@link ResourceCategory} values; values are ordered lists of root specs (filesystem paths or
     * {@code classpath:} URLs) that the bootstrap layer feeds into a {@code ResourceRegistry} during startup.</p>
     *
     * <p><b>Note:</b> the {@code with*()} mutators on this class do not preserve the resource roots map. Callers
     * that need both a {@code with*()} change and a non-empty resource roots map should chain
     * {@link #withResourceRoots(Map)} last.</p>
     */
    public Map<ResourceCategory, List<String>> resourceRoots() {
        return resourceRoots;
    }

    /** Returns the configured root specs for one category, or an empty list when none are configured. */
    public List<String> resourceRoots(ResourceCategory category) {
        Validation.requireNonNull(category, "Resource category is required.");
        return resourceRoots.getOrDefault(category, List.of());
    }

    public Optional<String> resourcePath(String resourceId) {
        Validation.requireNonBlank(resourceId, "Application resource config resource ID is required.");
        return Optional.ofNullable(resources.get(resourceId));
    }

    public Path resolveCategoryCodeTables(Path baseDirectory) {
        return PathUtils.resolveChild(baseDirectory, categoryCodeTablesPath);
    }

    public Path resolveImageAssetRoot(Path baseDirectory) {
        return PathUtils.resolveChild(baseDirectory, imageAssetRoot);
    }

    public Optional<Path> resolveResource(Path baseDirectory, String resourceId) {
        return resourcePath(resourceId).map(path -> PathUtils.resolveChild(baseDirectory, path));
    }

    public String jsonResourceRoot() {
        return resourcePath(JSON_RESOURCE_ROOT_ID).orElse(DEFAULT_JSON_RESOURCE_ROOT);
    }

    public Path resolveJsonResourceRoot(Path baseDirectory) {
        return PathUtils.resolveChild(baseDirectory, jsonResourceRoot());
    }

    public ApplicationResourceConfig withJsonResourceRoot(String jsonResourceRoot) {
        return putResource(JSON_RESOURCE_ROOT_ID, jsonResourceRoot);
    }

    /** Returns a copy with the given per-category resource roots, replacing any previously configured roots. */
    public ApplicationResourceConfig withResourceRoots(Map<ResourceCategory, List<String>> resourceRoots) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources,
                resourceRoots);
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

    public ApplicationResourceConfig withDebug(boolean debug) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public ApplicationResourceConfig withCategoryCodeTablesPath(String categoryCodeTablesPath) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public ApplicationResourceConfig withImageAssetRoot(String imageAssetRoot) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public ApplicationResourceConfig withDefaultAppBackgroundColor(String defaultAppBackgroundColor) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public ApplicationResourceConfig withDefaultAppBackgroundImage(String defaultAppBackgroundImage) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public ApplicationResourceConfig withDefaultAppBackgroundImageTransparency(String defaultAppBackgroundImageTransparency) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public ApplicationResourceConfig withDefaultPreferencesScreenBackgroundColor(String defaultPreferencesScreenBackgroundColor) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public ApplicationResourceConfig withDefaultPreferencesScreenBackgroundImage(String defaultPreferencesScreenBackgroundImage) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public ApplicationResourceConfig withDefaultPreferencesScreenBackgroundImageTransparency(
            String defaultPreferencesScreenBackgroundImageTransparency) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public ApplicationResourceConfig withDefaultSaveLoadScreenBackgroundColor(String defaultSaveLoadScreenBackgroundColor) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public ApplicationResourceConfig withDefaultSaveLoadScreenBackgroundImage(String defaultSaveLoadScreenBackgroundImage) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public ApplicationResourceConfig withDefaultSaveLoadScreenBackgroundImageTransparency(
            String defaultSaveLoadScreenBackgroundImageTransparency) {
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                resources);
    }

    public ApplicationResourceConfig putResource(String resourceId, String resourcePath) {
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(resources);
        updated.put(resourceId, resourcePath);
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                updated);
    }

    public ApplicationResourceConfig removeResource(String resourceId) {
        Validation.requireNonBlank(resourceId, "Application resource config resource ID is required.");
        if (!resources.containsKey(resourceId)) {
            throw new IllegalArgumentException("Unknown application resource: " + resourceId);
        }
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(resources);
        updated.remove(resourceId);
        return new ApplicationResourceConfig(
                debug,
                categoryCodeTablesPath,
                imageAssetRoot,
                defaultAppBackgroundColor,
                defaultAppBackgroundImage,
                defaultAppBackgroundImageTransparency,
                defaultPreferencesScreenBackgroundColor,
                defaultPreferencesScreenBackgroundImage,
                defaultPreferencesScreenBackgroundImageTransparency,
                defaultSaveLoadScreenBackgroundColor,
                defaultSaveLoadScreenBackgroundImage,
                defaultSaveLoadScreenBackgroundImageTransparency,
                updated);
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
                .append("  \"debug\": ").append(debug).append(",\n")
                .append("  \"categoryCodeTablesPath\": ").append(JsonStrings.quote(categoryCodeTablesPath)).append(",\n")
                .append("  \"imageAssetRoot\": ").append(JsonStrings.quote(imageAssetRoot)).append(",\n")
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

    private static Optional<String> optionalString(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key)) {
            return Optional.empty();
        }
        Object value = object.get(key);
        if (value instanceof String stringValue) {
            return Optional.of(Validation.requireNonBlank(stringValue, description + " must not be blank."));
        }
        throw new IllegalArgumentException("Expected JSON string for " + description + ".");
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
