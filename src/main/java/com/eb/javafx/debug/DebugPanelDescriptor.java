package com.eb.javafx.debug;

import com.eb.javafx.gamesupport.IdentifiedDefinition;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.Map;

/** Descriptor for application-owned debug panels backed by reusable debug registries. */
public record DebugPanelDescriptor(String id, String title, String routeId, Map<String, String> metadata)
        implements IdentifiedDefinition {
    public DebugPanelDescriptor {
        id = Validation.requireNonBlank(id, "Debug panel id is required.");
        title = Validation.requireNonBlank(title, "Debug panel title is required.");
        routeId = Validation.requireNonBlank(routeId, "Debug panel route id is required.");
        metadata = ImmutableCollections.copyMap(metadata);
    }
}
