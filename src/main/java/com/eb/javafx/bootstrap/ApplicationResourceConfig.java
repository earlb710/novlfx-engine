package com.eb.javafx.bootstrap;

import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.PathUtils;
import com.eb.javafx.util.SimpleJson;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
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
    private static final String DEFAULT_CATEGORY_CODE_TABLES_PATH = "config/category-code-tables.en.json";
    private static final String DEFAULT_IMAGE_ASSET_ROOT = "game";

    private final String categoryCodeTablesPath;
    private final String imageAssetRoot;
    private final Map<String, String> resources;

    private ApplicationResourceConfig(String categoryCodeTablesPath, String imageAssetRoot, Map<String, String> resources) {
        this.categoryCodeTablesPath = Validation.requireNonBlank(
                categoryCodeTablesPath,
                "Application resource config category code tables path is required.");
        this.imageAssetRoot = Validation.requireNonBlank(
                imageAssetRoot,
                "Application resource config image asset root is required.");
        LinkedHashMap<String, String> validatedResources = new LinkedHashMap<>();
        Validation.requireNonNull(resources, "Application resource config resources map is required.")
                .forEach((key, value) -> validatedResources.put(
                        Validation.requireNonBlank(key, "Application resource config resource ID is required."),
                        Validation.requireNonBlank(value, "Application resource config resource path is required.")));
        this.resources = Map.copyOf(validatedResources);
    }

    public static ApplicationResourceConfig defaults() {
        return new ApplicationResourceConfig(
                DEFAULT_CATEGORY_CODE_TABLES_PATH,
                DEFAULT_IMAGE_ASSET_ROOT,
                Map.of());
    }

    public static ApplicationResourceConfig of(String categoryCodeTablesPath, String imageAssetRoot, Map<String, String> resources) {
        return new ApplicationResourceConfig(categoryCodeTablesPath, imageAssetRoot, resources);
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
                optionalString(root, "categoryCodeTablesPath", "root.categoryCodeTablesPath")
                        .orElse(defaults.categoryCodeTablesPath()),
                optionalString(root, "imageAssetRoot", "root.imageAssetRoot")
                        .orElse(defaults.imageAssetRoot()),
                optionalObject(root, "resources", "root.resources")
                        .map(ApplicationResourceConfig::toStringMap)
                        .orElse(Map.of()));
    }

    public String categoryCodeTablesPath() {
        return categoryCodeTablesPath;
    }

    public String imageAssetRoot() {
        return imageAssetRoot;
    }

    public Map<String, String> resources() {
        return resources;
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

    public ApplicationResourceConfig withCategoryCodeTablesPath(String categoryCodeTablesPath) {
        return new ApplicationResourceConfig(categoryCodeTablesPath, imageAssetRoot, resources);
    }

    public ApplicationResourceConfig withImageAssetRoot(String imageAssetRoot) {
        return new ApplicationResourceConfig(categoryCodeTablesPath, imageAssetRoot, resources);
    }

    public ApplicationResourceConfig putResource(String resourceId, String resourcePath) {
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(resources);
        updated.put(resourceId, resourcePath);
        return new ApplicationResourceConfig(categoryCodeTablesPath, imageAssetRoot, updated);
    }

    public ApplicationResourceConfig removeResource(String resourceId) {
        Validation.requireNonBlank(resourceId, "Application resource config resource ID is required.");
        if (!resources.containsKey(resourceId)) {
            throw new IllegalArgumentException("Unknown application resource: " + resourceId);
        }
        LinkedHashMap<String, String> updated = new LinkedHashMap<>(resources);
        updated.remove(resourceId);
        return new ApplicationResourceConfig(categoryCodeTablesPath, imageAssetRoot, updated);
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
                .append("  \"categoryCodeTablesPath\": ").append(JsonStrings.quote(categoryCodeTablesPath)).append(",\n")
                .append("  \"imageAssetRoot\": ").append(JsonStrings.quote(imageAssetRoot)).append(",\n")
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
        json.append("  }\n")
                .append("}\n");
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
}
