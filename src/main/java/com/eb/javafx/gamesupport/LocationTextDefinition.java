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

/** JSON-backed localized location text definitions for one map and language. */
public final class LocationTextDefinition {
    private final String language;
    private final String mapId;
    private final DefinitionRegistry<LocationTextEntry> locations = new DefinitionRegistry<>("Location text");

    private LocationTextDefinition(String language, String mapId, List<LocationTextEntry> locations) {
        this.language = Validation.requireNonBlank(language, "Location text language must not be blank.");
        this.mapId = Validation.requireNonBlank(mapId, "Location text mapId must not be blank.");
        Validation.requireNonEmpty(locations, "Location text file must contain at least one location.")
                .forEach(this.locations::register);
    }

    public static LocationTextDefinition of(String language, String mapId, List<LocationTextEntry> locations) {
        return new LocationTextDefinition(language, mapId, locations);
    }

    public static LocationTextDefinition load(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Location text JSON path is required.");
        try {
            return fromJson(Files.readString(jsonPath, StandardCharsets.UTF_8), jsonPath.toString());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read location text JSON: " + jsonPath, exception);
        }
    }

    public static LocationTextDefinition loadResource(String resourceName) {
        String checkedResourceName = Validation.requireNonBlank(resourceName, "Location text resource name is required.");
        try (InputStream inputStream = LocationTextDefinition.class.getResourceAsStream(checkedResourceName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Missing location text resource: " + checkedResourceName);
            }
            return fromJson(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), checkedResourceName);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read location text resource: " + checkedResourceName, exception);
        }
    }

    public static LocationTextDefinition fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<LocationTextEntry> locations = JsonData.requiredList(root, "locations", "location text locations").stream()
                .map(LocationTextDefinition::parseLocation)
                .toList();
        return new LocationTextDefinition(
                JsonData.requiredString(root, "language", "location text language"),
                JsonData.requiredString(root, "mapId", "location text mapId"),
                locations);
    }

    public String language() {
        return language;
    }

    public String mapId() {
        return mapId;
    }

    public List<LocationTextEntry> locations() {
        return locations.definitions();
    }

    public Optional<LocationTextEntry> location(String locId) {
        return locations.definition(locId);
    }

    public boolean containsLocation(String locId) {
        return locations.contains(locId);
    }

    public Optional<LocationTextEntry> locationByReference(String reference) {
        String checkedReference = Validation.requireNonBlank(reference, "Location text reference must not be blank.");
        String prefix = mapId + ".";
        if (!checkedReference.startsWith(prefix)) {
            return Optional.empty();
        }
        return location(checkedReference.substring(prefix.length()));
    }

    public String reference(String locId) {
        return mapId + "." + Validation.requireNonBlank(locId, "Location text locId must not be blank.");
    }

    public String toJson() {
        StringBuilder json = new StringBuilder("{\n")
                .append("  \"language\": ").append(JsonStrings.quote(language)).append(",\n")
                .append("  \"mapId\": ").append(JsonStrings.quote(mapId)).append(",\n")
                .append("  \"locations\": [\n");
        List<LocationTextEntry> entries = locations();
        for (int index = 0; index < entries.size(); index++) {
            appendLocation(json, entries.get(index));
            if (index + 1 < entries.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n}\n");
        return json.toString();
    }

    public void save(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Location text JSON path is required.");
        try {
            Files.writeString(jsonPath, toJson(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to write location text JSON: " + jsonPath, exception);
        }
    }

    private static LocationTextEntry parseLocation(Object value) {
        Map<String, Object> object = JsonData.requireObject(value, "location text location");
        List<LocationDescriptionVariant> descriptions =
                JsonData.requiredList(object, "description", "location text description").stream()
                        .map(LocationTextDefinition::parseDescription)
                        .toList();
        return new LocationTextEntry(
                JsonData.requiredString(object, "locId", "location text locId"),
                descriptions);
    }

    private static LocationDescriptionVariant parseDescription(Object value) {
        Map<String, Object> object = JsonData.requireObject(value, "location text description");
        return new LocationDescriptionVariant(
                JsonData.requiredString(object, "text", "location description text"),
                JsonData.optionalStringList(object, "conditions", "location description conditions"));
    }

    private static void appendLocation(StringBuilder json, LocationTextEntry entry) {
        json.append("    {\n")
                .append("      \"locId\": ").append(JsonStrings.quote(entry.locId())).append(",\n")
                .append("      \"description\": [\n");
        for (int index = 0; index < entry.descriptions().size(); index++) {
            appendDescription(json, entry.descriptions().get(index));
            if (index + 1 < entry.descriptions().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("      ]\n")
                .append("    }");
    }

    private static void appendDescription(StringBuilder json, LocationDescriptionVariant variant) {
        json.append("        {\"text\": ").append(JsonStrings.quote(variant.text()))
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
