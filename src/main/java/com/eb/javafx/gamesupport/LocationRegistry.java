package com.eb.javafx.gamesupport;

import java.util.List;
import java.util.Optional;

/** Registers generic location descriptors and validates references between them. */
public final class LocationRegistry {
    private final DefinitionRegistry<LocationDescriptor> registry = new DefinitionRegistry<>("Location");

    public void register(LocationDescriptor location) {
        registry.register(location);
    }

    public Optional<LocationDescriptor> location(String id) {
        return registry.definition(id);
    }

    public List<LocationDescriptor> locations() {
        return registry.definitions();
    }

    public boolean contains(String id) {
        return registry.contains(id);
    }

    public boolean isEmpty() {
        return registry.isEmpty();
    }

    /** Validates parent location and action references after all static modules have registered definitions. */
    public void validateReferences(ActionRegistry actionRegistry) {
        for (LocationDescriptor location : locations()) {
            if (location.parentLocationId() != null && !contains(location.parentLocationId())) {
                throw new IllegalStateException("Location " + location.id()
                        + " references missing parent location: " + location.parentLocationId());
            }
            for (String actionId : location.actionIds()) {
                if (actionRegistry.action(actionId).isEmpty()) {
                    throw new IllegalStateException("Location " + location.id()
                            + " references missing action: " + actionId);
                }
            }
        }
    }
}
