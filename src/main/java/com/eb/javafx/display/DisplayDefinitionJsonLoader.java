package com.eb.javafx.display;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Loads app-owned display definitions into the reusable display registry. */
public final class DisplayDefinitionJsonLoader {
    private DisplayDefinitionJsonLoader() {
    }

    public static void loadInto(Path jsonPath, ImageDisplayRegistry registry) {
        Validation.requireNonNull(jsonPath, "Display definition JSON path is required.");
        try {
            loadInto(Files.readString(jsonPath, StandardCharsets.UTF_8), jsonPath.toString(), registry);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read display definition JSON: " + jsonPath, exception);
        }
    }

    public static void loadInto(String json, String sourceName, ImageDisplayRegistry registry) {
        Validation.requireNonNull(registry, "Image display registry is required.");
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        for (Object entry : JsonData.optionalList(root, "transforms", "root.transforms")) {
            registry.registerTransform(parseTransform(JsonData.requireObject(entry, "root.transforms[]")));
        }
        for (Object entry : JsonData.optionalList(root, "images", "root.images")) {
            registry.registerImage(parseImage(JsonData.requireObject(entry, "root.images[]")));
        }
        for (Object entry : JsonData.optionalList(root, "layeredCharacters", "root.layeredCharacters")) {
            registry.registerLayeredCharacter(parseLayeredCharacter(JsonData.requireObject(entry, "root.layeredCharacters[]")));
        }
    }

    private static DisplayTransform parseTransform(Map<String, Object> object) {
        return new DisplayTransform(
                JsonData.requiredString(object, "id", "display transform id"),
                JsonData.requiredInt(object, "fitWidth", "display transform fitWidth"),
                JsonData.requiredInt(object, "fitHeight", "display transform fitHeight"),
                JsonData.optionalDouble(object, "opacity", 1.0, "display transform opacity"),
                JsonData.optionalDouble(object, "xAlign", 0.5, "display transform xAlign"),
                JsonData.optionalDouble(object, "yAlign", 0.5, "display transform yAlign"));
    }

    private static ImageAssetDefinition parseImage(Map<String, Object> object) {
        return new ImageAssetDefinition(
                JsonData.requiredString(object, "id", "image asset id"),
                JsonData.requiredString(object, "sourcePath", "image asset sourcePath"),
                JsonData.optionalString(object, "transformId", "image asset transformId").orElse(null),
                JsonData.enumValue(DisplayLayer.class,
                        JsonData.optionalString(object, "layer", "image asset layer").orElse(DisplayLayer.CHARACTER.name()),
                        "display layer"));
    }

    private static LayeredCharacterDefinition parseLayeredCharacter(Map<String, Object> object) {
        List<String> drawOrder = JsonData.stringList(object.get("drawOrder"), "layered display drawOrder");
        return new LayeredCharacterDefinition(
                JsonData.requiredString(object, "id", "layered display id"),
                drawOrder,
                JsonData.optionalString(object, "defaultTransformId", "layered display defaultTransformId").orElse(null),
                JsonData.optionalObject(object, "metadata", "layered display metadata")
                        .map(metadata -> JsonData.stringMap(metadata, "layered display metadata"))
                        .orElse(Map.of()));
    }
}
