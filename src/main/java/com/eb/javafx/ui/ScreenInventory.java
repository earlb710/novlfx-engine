package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Immutable screen/style/control migration inventory. */
public record ScreenInventory(List<ScreenInventoryItem> items) {
    public ScreenInventory {
        items = List.copyOf(Validation.requireNonNull(items, "Screen inventory items are required."));
    }

    public Map<ScreenInventoryAssignmentCategory, List<ScreenInventoryItem>> byCategory() {
        return items.stream().collect(Collectors.groupingBy(ScreenInventoryItem::category));
    }

    public List<ScreenInventoryItem> assignedToRoute(String routeId) {
        return items.stream()
                .filter(item -> routeId.equals(item.assignedRouteId()))
                .toList();
    }
}
