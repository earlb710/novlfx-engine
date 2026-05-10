package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registers generic location descriptors and validates references between them. */
public final class LocationRegistry {
    private final Map<String, LocationDescriptor> locationsByPath = new LinkedHashMap<>();

    public void register(LocationDescriptor location) {
        LocationDescriptor checkedLocation = Validation.requireNonNull(location, "Location definition must not be null.");
        if (locationsByPath.containsKey(checkedLocation.path())) {
            throw new IllegalArgumentException("Location already registered: " + checkedLocation.path());
        }
        locationsByPath.put(checkedLocation.path(), checkedLocation);
    }

    public Optional<LocationDescriptor> location(String path) {
        return Optional.ofNullable(locationsByPath.get(path));
    }

    public List<LocationDescriptor> locations() {
        return Collections.unmodifiableList(new ArrayList<>(locationsByPath.values()));
    }

    public boolean contains(String path) {
        return locationsByPath.containsKey(path);
    }

    public boolean isEmpty() {
        return locationsByPath.isEmpty();
    }

    /** Validates parent location and action references after all static modules have registered definitions. */
    public void validateReferences(ActionRegistry actionRegistry) {
        for (LocationDescriptor location : locations()) {
            if (location.parentPath() != null && !contains(location.parentPath())) {
                throw new IllegalStateException("Location " + location.path()
                        + " references missing parent location: " + location.parentPath());
            }
            for (String actionId : location.actionIds()) {
                if (actionRegistry.action(actionId).isEmpty()) {
                    throw new IllegalStateException("Location " + location.path()
                            + " references missing action: " + actionId);
                }
            }
        }
    }
}
