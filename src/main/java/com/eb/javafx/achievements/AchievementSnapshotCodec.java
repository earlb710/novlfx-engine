package com.eb.javafx.achievements;

import com.eb.javafx.save.SaveSnapshotCodec;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.SimpleJson;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Serializes and deserializes {@link AchievementSnapshot} to/from a JSON array of unlocked ids. */
public final class AchievementSnapshotCodec implements SaveSnapshotCodec<AchievementSnapshot> {
    public static final String SECTION_ID = "achievements";
    public static final int SCHEMA_VERSION = 1;

    @Override
    public String sectionId() { return SECTION_ID; }

    @Override
    public int schemaVersion() { return SCHEMA_VERSION; }

    @Override
    public String toJson(AchievementSnapshot snapshot) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String id : snapshot.unlockedIds()) {
            if (!first) sb.append(",");
            sb.append(JsonStrings.quote(id));
            first = false;
        }
        return sb.append("]").toString();
    }

    @Override
    public AchievementSnapshot fromJson(String json, String sourceName) {
        Object parsed = SimpleJson.parse(json, sourceName);
        Set<String> ids = new LinkedHashSet<>();
        if (parsed instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String s) ids.add(s);
            }
        }
        return new AchievementSnapshot(ids);
    }
}
