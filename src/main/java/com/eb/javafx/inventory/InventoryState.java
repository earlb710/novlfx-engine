package com.eb.javafx.inventory;

import com.eb.javafx.util.Validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable generic inventory quantities keyed by item id. */
public final class InventoryState {
    private final Map<String, Integer> quantities = new LinkedHashMap<>();

    public int quantity(String itemId) {
        return quantities.getOrDefault(itemId, 0);
    }

    public void add(String itemId, int quantity, InventoryCatalog catalog) {
        String checkedItemId = requireKnownItem(itemId, catalog);
        Validation.requirePositive(quantity, "Inventory quantity must be positive.");
        InventoryItemDefinition item = catalog.item(checkedItemId).orElseThrow();
        int updated = Math.min(item.maxStack(), quantity(checkedItemId) + quantity);
        quantities.put(checkedItemId, updated);
    }

    public void remove(String itemId, int quantity) {
        String checkedItemId = Validation.requireNonBlank(itemId, "Inventory item id is required.");
        Validation.requirePositive(quantity, "Inventory quantity must be positive.");
        int updated = Math.max(0, quantity(checkedItemId) - quantity);
        if (updated == 0) {
            quantities.remove(checkedItemId);
        } else {
            quantities.put(checkedItemId, updated);
        }
    }

    public Map<String, Integer> quantities() {
        return Collections.unmodifiableMap(quantities);
    }

    private static String requireKnownItem(String itemId, InventoryCatalog catalog) {
        String checkedItemId = Validation.requireNonBlank(itemId, "Inventory item id is required.");
        InventoryCatalog checkedCatalog = Validation.requireNonNull(catalog, "Inventory catalog is required.");
        if (checkedCatalog.item(checkedItemId).isEmpty()) {
            throw new IllegalArgumentException("Unknown inventory item: " + checkedItemId);
        }
        return checkedItemId;
    }
}
