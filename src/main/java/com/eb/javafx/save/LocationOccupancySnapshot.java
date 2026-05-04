package com.eb.javafx.save;

import com.eb.javafx.gamesupport.LocationOccupancy;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.Map;

/** Immutable save snapshot of reusable character-to-location occupancy. */
public record LocationOccupancySnapshot(Map<String, String> characterLocations) {
    public LocationOccupancySnapshot {
        characterLocations = ImmutableCollections.copyMap(characterLocations);
        characterLocations.forEach((characterId, locationId) -> {
            Validation.requireNonBlank(characterId, "Occupancy character id is required.");
            Validation.requireNonBlank(locationId, "Occupancy location id is required.");
        });
    }

    public static LocationOccupancySnapshot empty() {
        return new LocationOccupancySnapshot(Map.of());
    }

    public static LocationOccupancySnapshot fromState(LocationOccupancy occupancy) {
        return new LocationOccupancySnapshot(
                Validation.requireNonNull(occupancy, "Location occupancy is required.").characterLocations());
    }

    public LocationOccupancy toState() {
        LocationOccupancy occupancy = new LocationOccupancy();
        characterLocations.forEach(occupancy::restoreLocation);
        return occupancy;
    }
}
