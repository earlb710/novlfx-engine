package com.eb.javafx.inventory;

import com.eb.javafx.util.Validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mutable per-save inventory quantities keyed by catalog item id.
 *
 * <p>{@link #add(String, int, InventoryCatalog)} validates the item against the supplied catalog and caps the updated
 * quantity at the item's maximum stack size. Removal never lets quantities fall below zero, and restore methods are
 * intended for trusted save snapshots.</p>
 */
public final class InventoryState {
    private final Map<String, Integer> quantities = new LinkedHashMap<>();

    public int quantity(String itemId) {
        return quantities.getOrDefault(itemId, 0);
    }

    public void add(String itemId, int quantity, InventoryCatalog catalog) {
        String checkedItemId = CatalogValidation.requireKnownInventoryItem(itemId, catalog);
        Validation.requirePositive(quantity, "Inventory quantity must be positive.");
        InventoryItemDefinition item = catalog.item(checkedItemId).orElseThrow();
        int updated = Math.min(item.maxStack(), quantity(checkedItemId) + quantity);
        quantities.put(checkedItemId, updated);
    }

    public void remove(String itemId, int quantity) {
        String checkedItemId = CatalogValidation.requireInventoryItemId(itemId);
        Validation.requirePositive(quantity, "Inventory quantity must be positive.");
        int updated = Math.max(0, quantity(checkedItemId) - quantity);
        if (updated == 0) {
            quantities.remove(checkedItemId);
        } else {
            quantities.put(checkedItemId, updated);
        }
    }

    public void restoreQuantity(String itemId, int quantity) {
        quantities.put(
                CatalogValidation.requireInventoryItemId(itemId),
                Validation.requirePositive(quantity, "Inventory quantity must be positive."));
    }

    public Map<String, Integer> quantities() {
        return Collections.unmodifiableMap(quantities);
    }

}
