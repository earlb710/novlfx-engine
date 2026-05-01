package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Mutable per-save occupancy container keyed by generic character and location IDs. */
public final class LocationOccupancy {
    private final Map<String, String> characterLocations = new LinkedHashMap<>();
    private final Map<String, LinkedHashSet<String>> occupantsByLocation = new LinkedHashMap<>();

    /** Places or moves a character into a known location. */
    public void place(String characterId, String locationId, LocationRegistry locationRegistry) {
        String checkedCharacterId = Validation.requireNonBlank(characterId, "Character ID must not be blank.");
        String checkedLocationId = Validation.requireNonBlank(locationId, "Location ID must not be blank.");
        if (!locationRegistry.contains(checkedLocationId)) {
            throw new IllegalArgumentException("Unknown location: " + checkedLocationId);
        }

        String previousLocation = characterLocations.put(checkedCharacterId, checkedLocationId);
        if (previousLocation != null) {
            LinkedHashSet<String> previousOccupants = occupantsByLocation.get(previousLocation);
            if (previousOccupants != null) {
                previousOccupants.remove(checkedCharacterId);
            }
        }
        occupantsByLocation.computeIfAbsent(checkedLocationId, ignored -> new LinkedHashSet<>())
                .add(checkedCharacterId);
    }

    public Optional<String> locationOf(String characterId) {
        return Optional.ofNullable(characterLocations.get(characterId));
    }

    public Set<String> charactersAt(String locationId) {
        LinkedHashSet<String> occupants = occupantsByLocation.get(locationId);
        if (occupants == null) {
            return Set.of();
        }
        return Collections.unmodifiableSet(occupants);
    }
}
