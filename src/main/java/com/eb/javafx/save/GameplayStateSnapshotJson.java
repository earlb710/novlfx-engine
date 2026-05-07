package com.eb.javafx.save;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.gamesupport.TimeSaveSnapshots;
import com.eb.javafx.progress.ProgressSnapshotCodec;
import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** JSON serialization for checkpoint gameplay-state snapshots. */
public final class GameplayStateSnapshotJson {
    private static final ProgressSnapshotCodec PROGRESS_CODEC = new ProgressSnapshotCodec();
    private static final InventorySnapshotCodec INVENTORY_CODEC = new InventorySnapshotCodec();
    private static final WardrobeSnapshotCodec WARDROBE_CODEC = new WardrobeSnapshotCodec();
    private static final CharacterStatesSnapshotCodec CHARACTERS_CODEC = new CharacterStatesSnapshotCodec();
    private static final JournalSnapshotCodec JOURNAL_CODEC = new JournalSnapshotCodec();
    private static final LocationOccupancySnapshotCodec LOCATION_OCCUPANCY_CODEC = new LocationOccupancySnapshotCodec();

    private GameplayStateSnapshotJson() {
    }

    public static String toJson(GameplayStateSnapshot snapshot) {
        GameplayStateSnapshot checkedSnapshot =
                Validation.requireNonNull(snapshot, "Gameplay state snapshot is required.");
        return "{"
                + "\"gameTime\": " + TimeSaveSnapshots.toJson(checkedSnapshot.gameTime()) + ", "
                + "\"progress\": " + PROGRESS_CODEC.toJson(checkedSnapshot.progress()) + ", "
                + "\"inventory\": " + INVENTORY_CODEC.toJson(checkedSnapshot.inventory()) + ", "
                + "\"wardrobe\": " + WARDROBE_CODEC.toJson(checkedSnapshot.wardrobe()) + ", "
                + "\"characters\": " + CHARACTERS_CODEC.toJson(checkedSnapshot.characters()) + ", "
                + "\"journal\": " + JOURNAL_CODEC.toJson(checkedSnapshot.journal()) + ", "
                + "\"locationOccupancy\": " + LOCATION_OCCUPANCY_CODEC.toJson(checkedSnapshot.locationOccupancy()) + ", "
                + "\"customSections\": " + customSectionsJson(checkedSnapshot.customSections())
                + "}";
    }

    public static GameplayStateSnapshot fromJson(String json, String sourceName) {
        return fromObject(JsonData.rootObject(json, sourceName), sourceName);
    }

    public static GameplayStateSnapshot fromObject(Map<String, Object> object, String sourceName) {
        Map<String, Object> root = Validation.requireNonNull(object, "Gameplay state snapshot JSON object is required.");
        return new GameplayStateSnapshot(
                timeFromObject(JsonData.requireObject(root.get("gameTime"), "checkpoint game time")),
                PROGRESS_CODEC.fromJson(objectJson(JsonData.requireObject(root.get("progress"), "checkpoint progress")), sourceName + ".progress"),
                INVENTORY_CODEC.fromJson(objectJson(JsonData.requireObject(root.get("inventory"), "checkpoint inventory")), sourceName + ".inventory"),
                WARDROBE_CODEC.fromJson(objectJson(JsonData.requireObject(root.get("wardrobe"), "checkpoint wardrobe")), sourceName + ".wardrobe"),
                CHARACTERS_CODEC.fromJson(objectJson(JsonData.requireObject(root.get("characters"), "checkpoint characters")), sourceName + ".characters"),
                JOURNAL_CODEC.fromJson(objectJson(JsonData.requireObject(root.get("journal"), "checkpoint journal")), sourceName + ".journal"),
                LOCATION_OCCUPANCY_CODEC.fromJson(
                        objectJson(JsonData.requireObject(root.get("locationOccupancy"), "checkpoint location occupancy")),
                        sourceName + ".locationOccupancy"),
                customSectionsFromList(JsonData.optionalList(root, "customSections", "checkpoint custom sections")));
    }

    private static GameDateTime timeFromObject(Map<String, Object> object) {
        return new GameDateTime(
                JsonData.requiredInt(object, "day", "checkpoint game time day"),
                JsonData.requiredString(object, "timeSlotId", "checkpoint game time slot id"));
    }

    private static String objectJson(Map<String, Object> object) {
        return object.entrySet().stream()
                .map(entry -> JsonStrings.quote(entry.getKey()) + ": " + valueJson(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static List<SaveSnapshotSection> customSectionsFromList(List<Object> values) {
        return values.stream()
                .map(entry -> JsonData.requireObject(entry, "checkpoint custom section"))
                .map(object -> new SaveSnapshotSection(
                        JsonData.requiredString(object, "sectionId", "checkpoint custom section id"),
                        JsonData.requiredInt(object, "schemaVersion", "checkpoint custom section schema version"),
                        JsonData.requiredString(object, "payloadJson", "checkpoint custom section payload JSON")))
                .toList();
    }

    private static String customSectionsJson(List<SaveSnapshotSection> sections) {
        return sections.stream()
                .map(section -> "{"
                        + "\"sectionId\": " + JsonStrings.quote(section.sectionId()) + ", "
                        + "\"schemaVersion\": " + section.schemaVersion() + ", "
                        + "\"payloadJson\": " + JsonStrings.quote(section.payloadJson())
                        + "}")
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static String valueJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return JsonStrings.quote(stringValue);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> mapValue) {
            return objectJson(JsonData.requireObject(mapValue, "checkpoint gameplay nested object"));
        }
        if (value instanceof List<?> listValue) {
            return listValue.stream()
                    .map(GameplayStateSnapshotJson::valueJson)
                    .collect(Collectors.joining(", ", "[", "]"));
        }
        throw new IllegalArgumentException("Unsupported checkpoint gameplay JSON value: " + value.getClass().getName());
    }
}
