package com.eb.javafx.inventory;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Registry for generic inventory item definitions. */
public final class InventoryCatalog {
    private final Map<String, InventoryItemDefinition> items = new LinkedHashMap<>();

    public void register(InventoryItemDefinition item) {
        InventoryItemDefinition checkedItem = Validation.requireNonNull(item, "Inventory item definition is required.");
        if (items.containsKey(checkedItem.id())) {
            throw new IllegalArgumentException("Inventory item already registered: " + checkedItem.id());
        }
        items.put(checkedItem.id(), checkedItem);
    }

    public Optional<InventoryItemDefinition> item(String itemId) {
        return Optional.ofNullable(items.get(itemId));
    }

    public java.util.List<InventoryItemDefinition> items() {
        return Collections.unmodifiableList(new ArrayList<>(items.values()));
    }
}
