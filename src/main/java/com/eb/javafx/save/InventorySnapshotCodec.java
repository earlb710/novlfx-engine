package com.eb.javafx.save;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.Validation;

import java.util.Map;

/** Save snapshot codec for generic inventory state. */
public final class InventorySnapshotCodec implements SaveSnapshotCodec<InventorySnapshot> {
    public static final String SECTION_ID = "inventory";
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
    public String toJson(InventorySnapshot snapshot) {
        InventorySnapshot checkedSnapshot = Validation.requireNonNull(snapshot, "Inventory snapshot is required.");
        return "{\n"
                + "  \"quantities\": " + SnapshotJson.integerMap(checkedSnapshot.quantities()) + "\n"
                + "}";
    }

    @Override
    public InventorySnapshot fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        return new InventorySnapshot(SnapshotJson.integerMap(root.getOrDefault("quantities", Map.of()), "inventory quantities"));
    }
}
