package com.eb.javafx.inventory;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;

/** Generic item definition; authored items remain application-owned data. */
public record InventoryItemDefinition(String id, String title, String categoryId, int maxStack, List<String> tags) {
    public InventoryItemDefinition {
        id = Validation.requireNonBlank(id, "Inventory item id is required.");
        title = Validation.requireNonBlank(title, "Inventory item title is required.");
        categoryId = categoryId == null ? "" : categoryId;
        maxStack = Validation.requirePositive(maxStack, "Inventory item max stack must be positive.");
        tags = ImmutableCollections.copyList(tags);
    }
}
