package com.eb.javafx.save;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.Validation;

import java.util.Map;

/** Save snapshot codec for generic location occupancy state. */
public final class LocationOccupancySnapshotCodec implements SaveSnapshotCodec<LocationOccupancySnapshot> {
    public static final String SECTION_ID = "locationOccupancy";
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
    public String toJson(LocationOccupancySnapshot snapshot) {
        LocationOccupancySnapshot checkedSnapshot =
                Validation.requireNonNull(snapshot, "Location occupancy snapshot is required.");
        return SnapshotJson.object(SnapshotJson.field(
                "characterLocations",
                SnapshotJson.stringMap(checkedSnapshot.characterLocations())));
    }

    @Override
    public LocationOccupancySnapshot fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        return new LocationOccupancySnapshot(
                SnapshotJson.stringMap(root.getOrDefault("characterLocations", Map.of()), "character locations"));
    }
}
