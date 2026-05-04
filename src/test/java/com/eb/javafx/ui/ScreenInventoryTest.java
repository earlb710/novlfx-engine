package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ScreenInventoryTest {
    @Test
    void groupsContentNeutralInventoryAssignments() {
        ScreenInventoryScanner scanner = source -> List.of(
                new ScreenInventoryItem("main", source.name(), ScreenInventoryAssignmentCategory.ROUTE_BACKED,
                        "main-menu", null, Map.of("line", "1")),
                new ScreenInventoryItem("old", source.name(), ScreenInventoryAssignmentCategory.DEPRECATED,
                        null, null, Map.of()));

        ScreenInventory inventory = new ScreenInventory(scanner.scan(
                new ScreenInventorySource("synthetic", Path.of("screens.txt"), "screen main")));

        assertEquals(1, inventory.assignedToRoute("main-menu").size());
        assertEquals(1, inventory.byCategory().get(ScreenInventoryAssignmentCategory.DEPRECATED).size());
    }
}
