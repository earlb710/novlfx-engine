package com.eb.javafx.inventory;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Mutable generic outfit state keyed by wearable slot id. */
public final class OutfitState {
    private final Map<String, String> equippedItemBySlot = new LinkedHashMap<>();

    public void equip(String slotId, String itemId, WardrobeCatalog wardrobeCatalog) {
        String checkedSlotId = Validation.requireNonBlank(slotId, "Wearable slot id is required.");
        String checkedItemId = Validation.requireNonBlank(itemId, "Wearable item id is required.");
        WardrobeCatalog checkedCatalog = Validation.requireNonNull(wardrobeCatalog, "Wardrobe catalog is required.");
        WearableDefinition wearable = checkedCatalog.wearable(checkedItemId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown wearable item: " + checkedItemId));
        if (checkedCatalog.slot(checkedSlotId).isEmpty()) {
            throw new IllegalArgumentException("Unknown wearable slot: " + checkedSlotId);
        }
        if (!wearable.supportsSlot(checkedSlotId)) {
            throw new IllegalArgumentException("Wearable " + checkedItemId + " does not support slot: " + checkedSlotId);
        }
        equippedItemBySlot.put(checkedSlotId, checkedItemId);
    }

    public Optional<String> equippedItem(String slotId) {
        return Optional.ofNullable(equippedItemBySlot.get(slotId));
    }

    public void unequip(String slotId) {
        equippedItemBySlot.remove(Validation.requireNonBlank(slotId, "Wearable slot id is required."));
    }

    public Map<String, String> equippedItems() {
        return ImmutableCollections.copyMap(equippedItemBySlot);
    }
}
