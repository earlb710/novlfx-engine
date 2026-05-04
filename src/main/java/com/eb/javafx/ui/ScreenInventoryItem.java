package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.Map;

/** One content-neutral screen/style/control inventory decision. */
public record ScreenInventoryItem(
        String id,
        String sourceName,
        ScreenInventoryAssignmentCategory category,
        String assignedRouteId,
        String reusableControlId,
        Map<String, String> metadata) {
    public ScreenInventoryItem {
        id = Validation.requireNonBlank(id, "Screen inventory item id is required.");
        sourceName = Validation.requireNonBlank(sourceName, "Screen inventory source name is required.");
        category = Validation.requireNonNull(category, "Screen inventory assignment category is required.");
        metadata = Map.copyOf(Validation.requireNonNull(metadata, "Screen inventory metadata is required."));
    }
}
