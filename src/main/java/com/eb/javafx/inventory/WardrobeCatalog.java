package com.eb.javafx.inventory;

import com.eb.javafx.gamesupport.DefinitionRegistry;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registry for wearable slots and wearable item metadata. */
public final class WardrobeCatalog {
    private final DefinitionRegistry<WearableSlotDefinition> slots = new DefinitionRegistry<>("Wearable slot");
    private final Map<String, WearableDefinition> wearables = new LinkedHashMap<>();

    public void registerSlot(WearableSlotDefinition slot) {
        slots.register(slot);
    }

    public void registerWearable(WearableDefinition wearable, InventoryCatalog inventoryCatalog) {
        WearableDefinition checkedWearable = Validation.requireNonNull(wearable, "Wearable definition is required.");
        if (Validation.requireNonNull(inventoryCatalog, "Inventory catalog is required.").item(checkedWearable.itemId()).isEmpty()) {
            throw new IllegalArgumentException("Unknown wearable inventory item: " + checkedWearable.itemId());
        }
        for (String slotId : checkedWearable.slotIds()) {
            if (slots.definition(slotId).isEmpty()) {
                throw new IllegalArgumentException("Unknown wearable slot: " + slotId);
            }
        }
        if (wearables.containsKey(checkedWearable.itemId())) {
            throw new IllegalArgumentException("Wearable already registered: " + checkedWearable.itemId());
        }
        wearables.put(checkedWearable.itemId(), checkedWearable);
    }

    public Optional<WearableSlotDefinition> slot(String slotId) {
        return slots.definition(slotId);
    }

    public Optional<WearableDefinition> wearable(String itemId) {
        return Optional.ofNullable(wearables.get(itemId));
    }

    public List<WearableSlotDefinition> slots() {
        return slots.definitions();
    }

    public List<WearableDefinition> wearables() {
        return List.copyOf(wearables.values());
    }
}
