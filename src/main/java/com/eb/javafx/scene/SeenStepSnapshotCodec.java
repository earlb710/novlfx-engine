package com.eb.javafx.scene;

import com.eb.javafx.save.SaveSnapshotCodec;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.SimpleJson;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Save snapshot codec for the set of scene steps the player has already seen. */
public final class SeenStepSnapshotCodec implements SaveSnapshotCodec<SeenStepSnapshot> {
    public static final String SECTION_ID = "seenSteps";
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
    public String toJson(SeenStepSnapshot snapshot) {
        Validation.requireNonNull(snapshot, "snapshot");
        String keys = snapshot.seenKeys().stream()
                .map(k -> "\"" + JsonStrings.escape(k) + "\"")
                .collect(Collectors.joining(", ", "[", "]"));
        return "{\n  \"seenKeys\": " + keys + "\n}";
    }

    @Override
    public SeenStepSnapshot fromJson(String json, String sourceName) {
        Object parsed = SimpleJson.parse(json, sourceName);
        if (!(parsed instanceof Map<?, ?> object)) {
            throw new IllegalArgumentException("SeenStepSnapshot JSON root must be an object in: " + sourceName);
        }
        Object raw = object.get("seenKeys");
        if (raw != null && !(raw instanceof List<?>)) {
            throw new IllegalArgumentException("SeenStepSnapshot seenKeys must be an array in: " + sourceName);
        }
        Set<String> keys = new LinkedHashSet<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof String s)) {
                    throw new IllegalArgumentException("SeenStepSnapshot seenKeys entries must be strings in: " + sourceName);
                }
                keys.add(s);
            }
        }
        return new SeenStepSnapshot(keys);
    }
}
