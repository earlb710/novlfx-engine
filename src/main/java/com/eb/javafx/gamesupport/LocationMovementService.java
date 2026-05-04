package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

import java.util.List;

/** Validates and applies generic character movement between registered locations. */
public final class LocationMovementService {
    private final LocationRegistry locationRegistry;
    private final LocationOccupancy occupancy;
    private final List<MovementValidator> validators;

    public LocationMovementService(LocationRegistry locationRegistry, LocationOccupancy occupancy, List<MovementValidator> validators) {
        this.locationRegistry = Validation.requireNonNull(locationRegistry, "Location registry is required.");
        this.occupancy = Validation.requireNonNull(occupancy, "Location occupancy is required.");
        this.validators = List.copyOf(Validation.requireNonNull(validators, "Movement validators are required."));
    }

    public MovementValidationResult canMove(String characterId, String toLocationId) {
        String checkedCharacterId = Validation.requireNonBlank(characterId, "Character ID must not be blank.");
        String checkedLocationId = Validation.requireNonBlank(toLocationId, "Location ID must not be blank.");
        if (!locationRegistry.contains(checkedLocationId)) {
            return MovementValidationResult.blocked("Unknown location: " + checkedLocationId);
        }
        String fromLocationId = occupancy.locationOf(checkedCharacterId).orElse(null);
        for (MovementValidator validator : validators) {
            MovementValidationResult result = validator.validate(checkedCharacterId, fromLocationId, checkedLocationId);
            if (!result.allowed()) {
                return result;
            }
        }
        return MovementValidationResult.permit();
    }

    public MovementValidationResult move(String characterId, String toLocationId) {
        MovementValidationResult result = canMove(characterId, toLocationId);
        if (result.allowed()) {
            occupancy.place(characterId, toLocationId, locationRegistry);
        }
        return result;
    }
}
