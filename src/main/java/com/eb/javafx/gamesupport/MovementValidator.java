package com.eb.javafx.gamesupport;

/** Application-supplied reusable rule for validating generic location movement. */
@FunctionalInterface
public interface MovementValidator {
    MovementValidationResult validate(String characterId, String fromLocationId, String toLocationId);
}
