package com.eb.javafx.display;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.Validation;
import javafx.animation.Animation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
        for (Object entry : JsonData.optionalList(root, "animations", "root.animations")) {
            registry.registerAnimation(parseAnimation(JsonData.requireObject(entry, "root.animations[]"), sourceName));
        }
        for (String script : JsonData.optionalStringList(root, "animationScripts", "root.animationScripts")) {
            for (AuthoredDisplayAnimation animation : AuthoredDisplayAnimationParser.parseDocument(script, sourceName)) {
                registry.registerAnimation(animation.compile());
            }
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

    private static DisplayAnimation parseAnimation(Map<String, Object> object, String sourceName) {
        String id = JsonData.requiredString(object, "id", "display animation id");
        int repeatCount = optionalRepeatCount(object, "repeatCount", 1, "display animation repeatCount");
        boolean autoReverse = JsonData.optionalBoolean(object, "autoReverse", false, "display animation autoReverse");
        if (object.containsKey("script") && object.get("script") != null) {
            return parseScriptedAnimation(object.get("script"), id, sourceName, repeatCount, autoReverse);
        }
        return new DisplayAnimation(id, parseAnimationSteps(object), repeatCount, autoReverse);
    }

    private static DisplayAnimation parseScriptedAnimation(
            Object scriptValue,
            String id,
            String sourceName,
            int repeatCount,
            boolean autoReverse) {
        if (scriptValue instanceof String script) {
            return AuthoredDisplayAnimationParser.parseAnimation(id, script, sourceName, 1, repeatCount, autoReverse).compile();
        }
        List<String> scriptLines = new ArrayList<>();
        int index = 0;
        for (Object line : JsonData.requireList(scriptValue, "display animation script")) {
            if (!(line instanceof String scriptLine) || scriptLine.isBlank()) {
                throw new IllegalArgumentException("Expected non-blank JSON string for display animation script[" + index + "].");
            }
            scriptLines.add(scriptLine);
            index++;
        }
        return AuthoredDisplayAnimationParser.parseAnimation(id, scriptLines, sourceName, 1, repeatCount, autoReverse).compile();
    }

    private static List<DisplayAnimationStep> parseAnimationSteps(Map<String, Object> object) {
        List<DisplayAnimationStep> steps = new ArrayList<>();
        int index = 0;
        for (Object entry : JsonData.requiredList(object, "steps", "display animation steps")) {
            steps.add(parseAnimationStep(JsonData.requireObject(entry, "display animation steps[" + index + "]")));
            index++;
        }
        return List.copyOf(steps);
    }

    private static DisplayAnimationStep parseAnimationStep(Map<String, Object> object) {
        return new DisplayAnimationStep(
                optionalLong(object, "durationMillis", 0L, "display animation step durationMillis"),
                optionalLong(object, "pauseBeforeMillis", 0L, "display animation step pauseBeforeMillis"),
                JsonData.optionalDouble(object, "targetOpacity", 1.0, "display animation step targetOpacity"),
                JsonData.optionalDouble(object, "targetScaleX", 1.0, "display animation step targetScaleX"),
                JsonData.optionalDouble(object, "targetScaleY", 1.0, "display animation step targetScaleY"),
                JsonData.optionalDouble(object, "targetTranslateX", 0.0, "display animation step targetTranslateX"),
                JsonData.optionalDouble(object, "targetTranslateY", 0.0, "display animation step targetTranslateY"),
                JsonData.optionalDouble(object, "targetRotate", 0.0, "display animation step targetRotate"),
                JsonData.enumValue(DisplayInterpolation.class,
                        JsonData.optionalString(object, "interpolation", "display animation step interpolation")
                                .orElse(DisplayInterpolation.LINEAR.name()),
                        "display animation interpolation"));
    }

    private static int optionalRepeatCount(Map<String, Object> object, String key, int defaultValue, String description) {
        if (!object.containsKey(key) || object.get(key) == null) {
            return defaultValue;
        }
        Object value = object.get(key);
        if (value instanceof String stringValue && "indefinite".equalsIgnoreCase(stringValue)) {
            return Animation.INDEFINITE;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        throw new IllegalArgumentException("Expected JSON integer or \"indefinite\" for " + description + ".");
    }

    private static long optionalLong(Map<String, Object> object, String key, long defaultValue, String description) {
        if (!object.containsKey(key) || object.get(key) == null) {
            return defaultValue;
        }
        Object value = object.get(key);
        if (value instanceof Integer integer) {
            return integer.longValue();
        }
        throw new IllegalArgumentException("Expected JSON integer for " + description + ".");
    }
}
