package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

import java.util.List;

/**
 * Generic room/location metadata without authored map content or movement rules.
 *
 * @param id stable location ID
 * @param title player-facing or localization-key title
 * @param routeId route that can display the location
 * @param parentLocationId optional hub/parent location ID, or {@code null} for roots
 * @param tags neutral metadata used by application-defined filters
 * @param actionIds generic action IDs available from this location
 */
public record LocationDescriptor(
        String id,
        String title,
        String routeId,
        String parentLocationId,
        List<String> tags,
        List<String> actionIds) implements IdentifiedDefinition {

    public LocationDescriptor {
        id = Validation.requireNonBlank(id, "Location ID must not be blank.");
        title = Validation.requireNonBlank(title, "Location title must not be blank.");
        routeId = Validation.requireNonBlank(routeId, "Location route ID must not be blank.");
        if (parentLocationId != null) {
            parentLocationId = Validation.requireNonBlank(parentLocationId, "Parent location ID must not be blank.");
        }
        tags = copyNonBlank(tags, "Location tag must not be blank.");
        actionIds = copyNonBlank(actionIds, "Location action ID must not be blank.");
    }

    private static List<String> copyNonBlank(List<String> values, String message) {
        List<String> copiedValues = List.copyOf(Validation.requireNonNull(values, message));
        copiedValues.forEach(value -> Validation.requireNonBlank(value, message));
        return copiedValues;
    }
}
