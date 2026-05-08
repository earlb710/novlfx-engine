package com.eb.javafx.save;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/** Save snapshot codec for generic journal or quest state. */
public final class JournalSnapshotCodec implements SaveSnapshotCodec<JournalSnapshot> {
    public static final String SECTION_ID = "journal";
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
    public String toJson(JournalSnapshot snapshot) {
        JournalSnapshot checkedSnapshot = Validation.requireNonNull(snapshot, "Journal snapshot is required.");
        return SnapshotJson.object(
                SnapshotJson.field("unlockedEntryIds", SnapshotJson.stringArray(checkedSnapshot.unlockedEntryIds())),
                SnapshotJson.field("readEntryIds", SnapshotJson.stringArray(checkedSnapshot.readEntryIds())));
    }

    @Override
    public JournalSnapshot fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        return new JournalSnapshot(
                SnapshotJson.stringSet(root.getOrDefault("unlockedEntryIds", List.of()), "unlocked journal entry ids"),
                SnapshotJson.stringSet(root.getOrDefault("readEntryIds", List.of()), "read journal entry ids"));
    }
}
