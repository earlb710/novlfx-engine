package com.eb.javafx.save;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Save snapshot codec for generic wardrobe state. */
public final class WardrobeSnapshotCodec implements SaveSnapshotCodec<WardrobeSnapshot> {
    public static final String SECTION_ID = "wardrobe";
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
    public String toJson(WardrobeSnapshot snapshot) {
        WardrobeSnapshot checkedSnapshot = Validation.requireNonNull(snapshot, "Wardrobe snapshot is required.");
        return SnapshotJson.object(
                SnapshotJson.field("unlockedWearableIds", SnapshotJson.stringArray(checkedSnapshot.unlockedWearableIds())),
                SnapshotJson.field("outfits", outfitsJson(checkedSnapshot.outfits())));
    }

    @Override
    public WardrobeSnapshot fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        Map<String, Map<String, String>> outfits = new LinkedHashMap<>();
        JsonData.requireObject(root.getOrDefault("outfits", Map.of()), "wardrobe outfits")
                .forEach((outfitId, equippedItems) -> outfits.put(
                        outfitId,
                        SnapshotJson.stringMap(equippedItems, "wardrobe outfit " + outfitId)));
        return new WardrobeSnapshot(
                SnapshotJson.stringSet(root.getOrDefault("unlockedWearableIds", java.util.List.of()), "unlocked wearable ids"),
                outfits);
    }

    private static String outfitsJson(Map<String, Map<String, String>> outfits) {
        return outfits.entrySet().stream()
                .map(entry -> JsonStrings.quote(entry.getKey()) + ": " + SnapshotJson.stringMap(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
