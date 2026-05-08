package com.eb.javafx.inventory;

import com.eb.javafx.util.Validation;

/** Shared inventory and wardrobe catalog argument validation helpers. */
final class CatalogValidation {
    private CatalogValidation() {
    }

    static String requireInventoryItemId(String itemId) {
        return Validation.requireNonBlank(itemId, "Inventory item id is required.");
    }

    static String requireKnownInventoryItem(String itemId, InventoryCatalog catalog) {
        String checkedItemId = requireInventoryItemId(itemId);
        InventoryCatalog checkedCatalog = Validation.requireNonNull(catalog, "Inventory catalog is required.");
        if (checkedCatalog.item(checkedItemId).isEmpty()) {
            throw new IllegalArgumentException("Unknown inventory item: " + checkedItemId);
        }
        return checkedItemId;
    }

    static String requireWearableItemId(String itemId) {
        return Validation.requireNonBlank(itemId, "Wearable item id is required.");
    }

    static String requireKnownWearableItem(String itemId, WardrobeCatalog catalog) {
        String checkedItemId = requireWearableItemId(itemId);
        WardrobeCatalog checkedCatalog = Validation.requireNonNull(catalog, "Wardrobe catalog is required.");
        if (checkedCatalog.wearable(checkedItemId).isEmpty()) {
            throw new IllegalArgumentException("Unknown wearable item: " + checkedItemId);
        }
        return checkedItemId;
    }
}
