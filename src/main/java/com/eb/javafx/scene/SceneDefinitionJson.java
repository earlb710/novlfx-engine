package com.eb.javafx.scene;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** JSON import/export for simple app-authored scene definitions. */
public final class SceneDefinitionJson {
    private SceneDefinitionJson() {
    }

    public static List<SceneDefinition> load(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Scene definition JSON path is required.");
        try {
            return fromJson(Files.readString(jsonPath, StandardCharsets.UTF_8), jsonPath.toString());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read scene definition JSON: " + jsonPath, exception);
        }
    }

    public static List<SceneDefinition> fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        return JsonData.optionalList(root, "scenes", "root.scenes").stream()
                .map(entry -> parseScene(JsonData.requireObject(entry, "root.scenes[]")))
                .toList();
    }

    public static String toJson(List<SceneDefinition> scenes) {
        StringBuilder json = new StringBuilder("{\n  \"scenes\": [\n");
        for (int index = 0; index < scenes.size(); index++) {
            appendScene(json, scenes.get(index), "    ");
            if (index + 1 < scenes.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n}\n");
        return json.toString();
    }

    public static String toJson(SceneDefinition scene) {
        return toJson(List.of(scene));
    }

    private static SceneDefinition parseScene(Map<String, Object> object) {
        rejectExecutableHooks(object, "scene " + object.get("id"));
        List<SceneStep> steps = JsonData.optionalList(object, "steps", "scene steps").stream()
                .map(entry -> parseStep(JsonData.requireObject(entry, "scene steps[]")))
                .toList();
        return new SceneDefinition(
                JsonData.requiredString(object, "id", "scene id"),
                List.of(),
                steps,
                JsonData.optionalObject(object, "metadata", "scene metadata")
                        .map(metadata -> JsonData.stringMap(metadata, "scene metadata"))
                        .orElse(Map.of()));
    }

    private static SceneStep parseStep(Map<String, Object> object) {
        rejectExecutableHooks(object, "scene step " + object.get("id"));
        SceneStepType type = JsonData.enumValue(SceneStepType.class,
                JsonData.requiredString(object, "type", "scene step type"),
                "scene step type");
        List<SceneChoice> choices = JsonData.optionalList(object, "choices", "scene step choices").stream()
                .map(entry -> parseChoice(JsonData.requireObject(entry, "scene step choices[]")))
                .toList();
        return SceneStep.create(
                JsonData.requiredString(object, "id", "scene step id"),
                type,
                JsonData.optionalString(object, "speakerId", "scene step speakerId").orElse(null),
                JsonData.optionalString(object, "textDefinition", "scene step textDefinition").orElse(null),
                JsonData.optionalString(object, "displayReference", "scene step displayReference").orElse(null),
                choices,
                List.of(),
                parseTransition(object),
                JsonData.optionalObject(object, "metadata", "scene step metadata")
                        .map(metadata -> JsonData.stringMap(metadata, "scene step metadata"))
                        .orElse(Map.of()));
    }

    private static SceneChoice parseChoice(Map<String, Object> object) {
        rejectExecutableHooks(object, "scene choice " + object.get("id"));
        return new SceneChoice(
                JsonData.requiredString(object, "id", "scene choice id"),
                JsonData.requiredString(object, "textDefinition", "scene choice textDefinition"),
                List.of(),
                List.of(),
                JsonData.optionalString(object, "disabledReason", "scene choice disabledReason").orElse(null),
                parseTransition(object),
                JsonData.optionalObject(object, "metadata", "scene choice metadata")
                        .map(metadata -> JsonData.stringMap(metadata, "scene choice metadata"))
                        .orElse(Map.of()));
    }

    private static SceneTransition parseTransition(Map<String, Object> object) {
        Map<String, Object> transition = JsonData.optionalObject(object, "transition", "transition").orElse(Map.of());
        SceneTransitionType type = transition.isEmpty()
                ? SceneTransitionType.NEXT
                : JsonData.enumValue(SceneTransitionType.class,
                JsonData.requiredString(transition, "type", "transition type"),
                "scene transition type");
        String target = JsonData.optionalString(transition, "targetSceneId", "transition targetSceneId").orElse(null);
        return switch (type) {
            case NEXT -> SceneTransition.next();
            case JUMP -> SceneTransition.jump(target);
            case CALL -> SceneTransition.call(target);
            case RETURN -> SceneTransition.returnToCaller();
            case COMPLETE -> SceneTransition.complete();
            case FAIL -> SceneTransition.fail(target);
        };
    }

    private static void rejectExecutableHooks(Map<String, Object> object, String description) {
        if (!JsonData.optionalList(object, "requirements", description + " requirements").isEmpty()
                || !JsonData.optionalList(object, "effects", description + " effects").isEmpty()
                || !JsonData.optionalList(object, "entryRequirements", description + " entryRequirements").isEmpty()) {
            throw new IllegalArgumentException("Scene JSON " + description
                    + " cannot create executable requirements or effects; register them in code modules.");
        }
    }

    private static void appendScene(StringBuilder json, SceneDefinition scene, String indent) {
        json.append(indent).append("{\n")
                .append(indent).append("  \"id\": ").append(JsonStrings.quote(scene.id())).append(",\n")
                .append(indent).append("  \"metadata\": ").append(stringMapJson(scene.metadata())).append(",\n")
                .append(indent).append("  \"steps\": [\n");
        for (int index = 0; index < scene.steps().size(); index++) {
            appendStep(json, scene.steps().get(index), indent + "    ");
            if (index + 1 < scene.steps().size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append(indent).append("  ]\n")
                .append(indent).append('}');
    }

    private static void appendStep(StringBuilder json, SceneStep step, String indent) {
        json.append(indent).append("{\n")
                .append(indent).append("  \"id\": ").append(JsonStrings.quote(step.id())).append(",\n")
                .append(indent).append("  \"type\": ").append(JsonStrings.quote(step.type().name())).append(",\n")
                .append(indent).append("  \"speakerId\": ").append(JsonStrings.nullableQuote(step.speakerId())).append(",\n")
                .append(indent).append("  \"textDefinition\": ").append(JsonStrings.nullableQuote(step.textDefinition())).append(",\n")
                .append(indent).append("  \"displayReference\": ").append(JsonStrings.nullableQuote(step.displayReference())).append(",\n")
                .append(indent).append("  \"transition\": ").append(transitionJson(step.transition())).append(",\n")
                .append(indent).append("  \"metadata\": ").append(stringMapJson(step.metadata())).append(",\n")
                .append(indent).append("  \"choices\": [");
        for (int index = 0; index < step.choices().size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            json.append(choiceJson(step.choices().get(index)));
        }
        json.append("]\n")
                .append(indent).append('}');
    }

    private static String choiceJson(SceneChoice choice) {
        return "{"
                + "\"id\": " + JsonStrings.quote(choice.id())
                + ", \"textDefinition\": " + JsonStrings.quote(choice.textDefinition())
                + ", \"disabledReason\": " + JsonStrings.nullableQuote(choice.disabledReason())
                + ", \"transition\": " + transitionJson(choice.transition())
                + ", \"metadata\": " + stringMapJson(choice.metadata())
                + "}";
    }

    private static String transitionJson(SceneTransition transition) {
        return "{\"type\": " + JsonStrings.quote(transition.type().name())
                + ", \"targetSceneId\": " + JsonStrings.nullableQuote(transition.targetSceneId()) + "}";
    }

    private static String stringMapJson(Map<String, String> values) {
        StringBuilder json = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (index++ > 0) {
                json.append(", ");
            }
            json.append(JsonStrings.quote(entry.getKey())).append(": ").append(JsonStrings.quote(entry.getValue()));
        }
        return json.append('}').toString();
    }
}
