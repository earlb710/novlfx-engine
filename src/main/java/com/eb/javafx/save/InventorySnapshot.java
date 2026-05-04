package com.eb.javafx.save;

import com.eb.javafx.inventory.InventoryState;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.Map;

/** Immutable save snapshot of reusable inventory quantities keyed by item id. */
public record InventorySnapshot(Map<String, Integer> quantities) {
    public InventorySnapshot {
        quantities = ImmutableCollections.copyMap(quantities);
        quantities.forEach((itemId, quantity) -> {
            Validation.requireNonBlank(itemId, "Inventory snapshot item id is required.");
            Validation.requirePositive(quantity, "Inventory snapshot quantity must be positive.");
        });
    }

    public static InventorySnapshot empty() {
        return new InventorySnapshot(Map.of());
    }

    public static InventorySnapshot fromState(InventoryState state) {
        return new InventorySnapshot(Validation.requireNonNull(state, "Inventory state is required.").quantities());
    }

    public InventoryState toState() {
        InventoryState state = new InventoryState();
        quantities.forEach(state::restoreQuantity);
        return state;
    }
}
