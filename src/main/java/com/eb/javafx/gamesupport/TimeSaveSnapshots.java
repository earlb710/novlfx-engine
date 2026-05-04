package com.eb.javafx.gamesupport;

import com.eb.javafx.save.SaveSnapshotSection;
import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;

import java.util.Map;

/** JSON snapshot helper for reusable game-clock save sections. */
public final class TimeSaveSnapshots {
    public static final String SNAPSHOT_SECTION_ID = "gameTime";
    public static final int SNAPSHOT_SCHEMA_VERSION = 1;

    private TimeSaveSnapshots() {
    }

    public static String toJson(GameDateTime time) {
        return "{\"day\": " + time.day() + ", \"timeSlotId\": " + JsonStrings.quote(time.timeSlotId()) + "}";
    }

    public static GameDateTime fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        return new GameDateTime(
                JsonData.requiredInt(root, "day", "game time day"),
                JsonData.requiredString(root, "timeSlotId", "game time slot id"));
    }

    public static SaveSnapshotSection toSnapshotSection(GameDateTime time) {
        return new SaveSnapshotSection(SNAPSHOT_SECTION_ID, SNAPSHOT_SCHEMA_VERSION, toJson(time));
    }

    public static GameDateTime fromSnapshotSection(SaveSnapshotSection section) {
        if (!SNAPSHOT_SECTION_ID.equals(section.sectionId())) {
            throw new IllegalArgumentException("Unexpected time snapshot section: " + section.sectionId());
        }
        if (section.schemaVersion() != SNAPSHOT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported time snapshot schema version: " + section.schemaVersion());
        }
        return fromJson(section.payloadJson(), section.sectionId());
    }
}
