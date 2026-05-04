package com.eb.javafx.inventory;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/** Wearable metadata that links an inventory item to supported generic slots. */
public record WearableDefinition(String itemId, List<String> slotIds, List<String> outfitTags, Map<String, String> metadata) {
    public WearableDefinition {
        itemId = Validation.requireNonBlank(itemId, "Wearable item id is required.");
        slotIds = ImmutableCollections.copyList(slotIds);
        if (slotIds.isEmpty()) {
            throw new IllegalArgumentException("Wearable slot ids are required.");
        }
        slotIds.forEach(slotId -> Validation.requireNonBlank(slotId, "Wearable slot id is required."));
        outfitTags = ImmutableCollections.copyList(outfitTags);
        outfitTags.forEach(tag -> Validation.requireNonBlank(tag, "Wearable outfit tag is required."));
        metadata = ImmutableCollections.copyMap(metadata);
    }

    public boolean supportsSlot(String slotId) {
        return slotIds.contains(slotId);
    }
}
