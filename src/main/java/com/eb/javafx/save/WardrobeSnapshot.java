package com.eb.javafx.save;

import com.eb.javafx.inventory.OutfitState;
import com.eb.javafx.inventory.WardrobeState;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Immutable save snapshot of reusable wardrobe unlocks and named outfit slot assignments. */
public record WardrobeSnapshot(Set<String> unlockedWearableIds, Map<String, Map<String, String>> outfits) {
    public WardrobeSnapshot {
        unlockedWearableIds = unlockedWearableIds == null || unlockedWearableIds.isEmpty()
                ? Set.of()
                : Set.copyOf(new LinkedHashSet<>(unlockedWearableIds));
        unlockedWearableIds.forEach(id -> Validation.requireNonBlank(id, "Unlocked wearable id is required."));
        outfits = copyOutfits(outfits);
    }

    public static WardrobeSnapshot empty() {
        return new WardrobeSnapshot(Set.of(), Map.of());
    }

    public static WardrobeSnapshot fromState(WardrobeState state) {
        WardrobeState checkedState = Validation.requireNonNull(state, "Wardrobe state is required.");
        Map<String, Map<String, String>> snapshotOutfits = new LinkedHashMap<>();
        checkedState.outfits().forEach((outfitId, outfit) -> snapshotOutfits.put(outfitId, outfit.equippedItems()));
        return new WardrobeSnapshot(checkedState.unlockedWearableIds(), snapshotOutfits);
    }

    public WardrobeState toState() {
        WardrobeState state = new WardrobeState();
        unlockedWearableIds.forEach(state::restoreUnlockedWearableId);
        outfits.keySet().forEach(outfitId -> state.saveOutfit(outfitId, toOutfitState(outfitId)));
        return state;
    }

    public OutfitState toOutfitState(String outfitId) {
        Map<String, String> equippedItems = outfits.get(Validation.requireNonBlank(outfitId, "Outfit id is required."));
        OutfitState outfit = new OutfitState();
        if (equippedItems != null) {
            equippedItems.forEach(outfit::restoreEquippedItem);
        }
        return outfit;
    }

    private static Map<String, Map<String, String>> copyOutfits(Map<String, Map<String, String>> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        values.forEach((outfitId, equippedItems) -> {
            Validation.requireNonBlank(outfitId, "Outfit id is required.");
            result.put(outfitId, ImmutableCollections.copyMap(equippedItems));
        });
        return Map.copyOf(result);
    }
}
