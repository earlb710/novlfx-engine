package com.eb.javafx.gamesupport;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** JSON-backed localized map text definitions for a single authored language. */
public final class MapTextDefinition {
    private final String language;
    private final DefinitionRegistry<MapTextEntry> maps = new DefinitionRegistry<>("Map text");

    private MapTextDefinition(String language, List<MapTextEntry> maps) {
        this.language = Validation.requireNonBlank(language, "Map text language must not be blank.");
        Validation.requireNonEmpty(maps, "Map text file must contain at least one map.")
                .forEach(this.maps::register);
    }

    public static MapTextDefinition of(String language, List<MapTextEntry> maps) {
        return new MapTextDefinition(language, maps);
    }

    public static MapTextDefinition load(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Map text JSON path is required.");
        try {
            return fromJson(Files.readString(jsonPath, StandardCharsets.UTF_8), jsonPath.toString());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read map text JSON: " + jsonPath, exception);
        }
    }

    public static MapTextDefinition loadResource(String resourceName) {
        String checkedResourceName = Validation.requireNonBlank(resourceName, "Map text resource name is required.");
        try (InputStream inputStream = MapTextDefinition.class.getResourceAsStream(checkedResourceName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Missing map text resource: " + checkedResourceName);
            }
            return fromJson(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), checkedResourceName);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read map text resource: " + checkedResourceName, exception);
        }
    }

    public static MapTextDefinition fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<MapTextEntry> maps = JsonData.requiredList(root, "maps", "map text maps").stream()
                .map(MapTextDefinition::parseMap)
                .toList();
        return new MapTextDefinition(JsonData.requiredString(root, "language", "map text language"), maps);
    }

    public String language() {
        return language;
    }

    public List<MapTextEntry> maps() {
        return maps.definitions();
    }

    public Optional<MapTextEntry> map(String mapId) {
        return maps.definition(mapId);
    }

    public boolean containsMap(String mapId) {
        return maps.contains(mapId);
    }

    public String toJson() {
        StringBuilder json = new StringBuilder("{\n")
                .append("  \"language\": ").append(JsonStrings.quote(language)).append(",\n")
                .append("  \"maps\": [\n");
        List<MapTextEntry> entries = maps();
        for (int index = 0; index < entries.size(); index++) {
            MapTextEntry entry = entries.get(index);
            json.append("    {\"mapId\": ").append(JsonStrings.quote(entry.mapId()))
                    .append(", \"description\": ").append(JsonStrings.quote(entry.description()))
                    .append('}');
            if (index + 1 < entries.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n}\n");
        return json.toString();
    }

    public void save(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Map text JSON path is required.");
        try {
            Files.writeString(jsonPath, toJson(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to write map text JSON: " + jsonPath, exception);
        }
    }

    private static MapTextEntry parseMap(Object value) {
        Map<String, Object> object = JsonData.requireObject(value, "map text map");
        String description = JsonData.optionalString(object, "description", "map text description")
                .orElse(MapTextEntry.DEFAULT_DESCRIPTION);
        return new MapTextEntry(JsonData.requiredString(object, "mapId", "map text mapId"), description);
    }
}
