package com.eb.javafx.storyline;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.SimpleJson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads a {@link Storyline} from a JSON string using the engine's {@link SimpleJson} parser.
 *
 * <h3>Top-level format</h3>
 * <pre>{@code
 * {
 *   "events": [ <event>, ... ]
 * }
 * }</pre>
 *
 * <h3>Event format</h3>
 * <pre>{@code
 * {
 *   "id":              "marsh.intro",
 *   "textKey":         "text.marsh.intro",
 *   "repeatable":      false,
 *   "trigger":         { "type": "dialogChain", "dialogChainId": "dialog.marsh.intro" },
 *   "allowedStatuses": ["accept", "refuse"],
 *   "requirements":   [ <requirement>, ... ]
 * }
 * }</pre>
 *
 * <h3>Trigger types</h3>
 * <ul>
 *   <li>Omit {@code "trigger"} or {@code { "type": "none" }} → {@link EventTrigger.None}</li>
 *   <li>{@code { "type": "scene", "sceneId": "..." }} → {@link EventTrigger.Scene}</li>
 *   <li>{@code { "type": "dialogChain", "dialogChainId": "..." }} → {@link EventTrigger.DialogChain}</li>
 * </ul>
 *
 * <h3>Requirement types</h3>
 * <ul>
 *   <li>{@code { "type": "location",      "locationId": "..." }}</li>
 *   <li>{@code { "type": "character",     "characterId": "..." }}</li>
 *   <li>{@code { "type": "timeOfDay",     "timeBucket": "..." }}</li>
 *   <li>{@code { "type": "eventCompleted","eventId": "..." }}</li>
 *   <li>{@code { "type": "eventStatus",   "eventId": "...", "status": "..." }}</li>
 *   <li>{@code { "type": "flag",          "flag": "..." }}</li>
 *   <li>{@code { "type": "allOf",         "requirements": [...] }}</li>
 *   <li>{@code { "type": "anyOf",         "requirements": [...] }}</li>
 *   <li>{@code { "type": "not",           "requirement": {...} }}</li>
 *   <li>{@code { "type": "custom",        "name": "..." }} — resolved via {@link CustomRequirementRegistry}</li>
 * </ul>
 */
public final class StorylineJsonLoader {

    private final CustomRequirementRegistry customRegistry;

    /** Loader that rejects any {@code custom} requirement type (no registry provided). */
    public StorylineJsonLoader() {
        this(CustomRequirementRegistry.empty());
    }

    public StorylineJsonLoader(CustomRequirementRegistry customRegistry) {
        this.customRegistry = Objects.requireNonNull(customRegistry, "customRegistry is required.");
    }

    /** Parses {@code json} and returns the resulting {@link Storyline}. */
    public Storyline load(String json) {
        return load(json, "storyline");
    }

    /** Parses {@code json} with the given {@code sourceName} in error messages. */
    public Storyline load(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<Object> eventsArray = JsonData.requiredList(root, "events", "events");
        Storyline.Builder builder = Storyline.builder();
        int i = 0;
        for (Object entry : eventsArray) {
            Map<String, Object> obj = JsonData.requireObject(entry, "events[" + i + "]");
            builder.event(parseEvent(obj, "events[" + i + "]"));
            i++;
        }
        return builder.build();
    }

    private StorylineEvent parseEvent(Map<String, Object> obj, String path) {
        String id = JsonData.requiredString(obj, "id", path + ".id");
        String textKey = JsonData.requiredString(obj, "textKey", path + ".textKey");
        boolean repeatable = JsonData.optionalBoolean(obj, "repeatable", false, path + ".repeatable");

        StorylineEvent.Builder event = StorylineEvent.builder(id, textKey).repeatable(repeatable);

        JsonData.optionalString(obj, "description", path + ".description")
                .ifPresent(event::description);

        JsonData.optionalObject(obj, "trigger", path + ".trigger")
                .ifPresent(t -> event.trigger(parseTrigger(t, path + ".trigger")));

        for (String status : JsonData.optionalStringList(obj, "allowedStatuses", path + ".allowedStatuses")) {
            event.allowedStatus(status);
        }

        List<Object> reqs = JsonData.optionalList(obj, "requirements", path + ".requirements");
        for (int ri = 0; ri < reqs.size(); ri++) {
            Map<String, Object> reqObj = JsonData.requireObject(reqs.get(ri), path + ".requirements[" + ri + "]");
            event.requirement(parseRequirement(reqObj, path + ".requirements[" + ri + "]"));
        }

        return event.build();
    }

    private EventTrigger parseTrigger(Map<String, Object> obj, String path) {
        String type = JsonData.requiredString(obj, "type", path + ".type");
        return switch (type) {
            case "none" -> EventTrigger.none();
            case "scene" -> EventTrigger.scene(JsonData.requiredString(obj, "sceneId", path + ".sceneId"));
            case "dialogChain" -> EventTrigger.dialogChain(
                    JsonData.requiredString(obj, "dialogChainId", path + ".dialogChainId"));
            default -> throw new IllegalArgumentException("Unknown trigger type \"" + type + "\" at " + path + ".");
        };
    }

    private EventRequirement parseRequirement(Map<String, Object> obj, String path) {
        String type = JsonData.requiredString(obj, "type", path + ".type");
        return switch (type) {
            case "location" -> EventRequirement.atLocation(
                    JsonData.requiredString(obj, "locationId", path + ".locationId"));
            case "character" -> EventRequirement.withCharacter(
                    JsonData.requiredString(obj, "characterId", path + ".characterId"));
            case "timeOfDay" -> EventRequirement.atTimeOfDay(
                    JsonData.requiredString(obj, "timeBucket", path + ".timeBucket"));
            case "eventCompleted" -> EventRequirement.eventCompleted(
                    JsonData.requiredString(obj, "eventId", path + ".eventId"));
            case "eventStatus" -> EventRequirement.eventStatus(
                    JsonData.requiredString(obj, "eventId", path + ".eventId"),
                    JsonData.requiredString(obj, "status", path + ".status"));
            case "flag" -> EventRequirement.flagSet(
                    JsonData.requiredString(obj, "flag", path + ".flag"));
            case "allOf" -> EventRequirement.allOf(parseRequirementList(obj, path));
            case "anyOf" -> EventRequirement.anyOf(parseRequirementList(obj, path));
            case "not" -> {
                Map<String, Object> inner = JsonData.optionalObject(obj, "requirement", path + ".requirement")
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Missing \"requirement\" for not at " + path + "."));
                yield EventRequirement.not(parseRequirement(inner, path + ".requirement"));
            }
            case "custom" -> customRegistry.resolve(
                    JsonData.requiredString(obj, "name", path + ".name"));
            default -> throw new IllegalArgumentException(
                    "Unknown requirement type \"" + type + "\" at " + path + ".");
        };
    }

    private List<EventRequirement> parseRequirementList(Map<String, Object> obj, String path) {
        List<Object> items = JsonData.requiredList(obj, "requirements", path + ".requirements");
        List<EventRequirement> result = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> child = JsonData.requireObject(items.get(i), path + ".requirements[" + i + "]");
            result.add(parseRequirement(child, path + ".requirements[" + i + "]"));
        }
        return result;
    }
}
