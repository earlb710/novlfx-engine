package com.eb.javafx.progress;

import com.eb.javafx.save.SaveSnapshotCodec;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.SimpleJson;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Save snapshot codec for persistent (cross-session) progress state. */
public final class PersistentProgressSnapshotCodec implements SaveSnapshotCodec<PersistentProgressSnapshot> {
    public static final String SECTION_ID = "persistent-progress";
    public static final int SCHEMA_VERSION = 1;

    @Override
    public String sectionId() {
        return SECTION_ID;
    }

    @Override
    public int schemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public String toJson(PersistentProgressSnapshot snapshot) {
        PersistentProgressSnapshot checked = Validation.requireNonNull(snapshot, "Persistent progress snapshot is required.");
        return "{\n"
                + "  \"flags\": " + stringArray(checked.flags()) + ",\n"
                + "  \"counters\": " + integerMap(checked.counters()) + ",\n"
                + "  \"milestones\": " + stringArray(checked.milestones()) + ",\n"
                + "  \"unlocks\": " + stringArray(checked.unlocks()) + "\n"
                + "}";
    }

    @Override
    public PersistentProgressSnapshot fromJson(String json, String sourceName) {
        Object parsed = SimpleJson.parse(json, sourceName);
        if (!(parsed instanceof Map<?, ?> object)) {
            throw new IllegalArgumentException("Persistent progress snapshot JSON root must be an object.");
        }
        return new PersistentProgressSnapshot(
                stringSet(object.get("flags"), "flags"),
                counters(object.get("counters")),
                stringSet(object.get("milestones"), "milestones"),
                stringSet(object.get("unlocks"), "unlocks"));
    }

    private static String stringArray(Set<String> values) {
        return values.stream()
                .map(value -> "\"" + JsonStrings.escape(value) + "\"")
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }

    private static String integerMap(Map<String, Integer> values) {
        return values.entrySet().stream()
                .map(entry -> "\"" + JsonStrings.escape(entry.getKey()) + "\": " + entry.getValue())
                .collect(java.util.stream.Collectors.joining(", ", "{", "}"));
    }

    private static Set<String> stringSet(Object value, String field) {
        if (value == null) {
            return Set.of();
        }
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("Persistent progress snapshot field must be an array: " + field);
        }
        Set<String> result = new LinkedHashSet<>();
        for (Object item : values) {
            if (!(item instanceof String text)) {
                throw new IllegalArgumentException("Persistent progress snapshot array values must be strings: " + field);
            }
            result.add(text);
        }
        return result;
    }

    private static Map<String, Integer> counters(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> values)) {
            throw new IllegalArgumentException("Persistent progress snapshot counters must be an object.");
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof Number number)) {
                throw new IllegalArgumentException("Persistent progress snapshot counters must contain numeric values.");
            }
            result.put(key, number.intValue());
        }
        return result;
    }
}
