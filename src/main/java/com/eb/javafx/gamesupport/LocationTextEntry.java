package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

import java.util.Collection;
import java.util.List;

/** Localized text variants for a location within a map text file. */
public record LocationTextEntry(String locId, List<LocationDescriptionVariant> descriptions) implements IdentifiedDefinition {
    public LocationTextEntry {
        locId = Validation.requireNonBlank(locId, "Location text locId must not be blank.");
        descriptions = List.copyOf(Validation.requireNonEmpty(
                descriptions,
                "Location text entry must contain at least one description."));
    }

    @Override
    public String id() {
        return locId;
    }

    public String reference(String mapId) {
        return Validation.requireNonBlank(mapId, "Location text mapId must not be blank.") + "." + locId;
    }

    public LocationDescriptionVariant descriptionForConditions(Collection<String> activeConditions) {
        return descriptions.stream()
                .filter(variant -> variant.hasConditions() && variant.matchesConditions(activeConditions))
                .findFirst()
                .or(() -> descriptions.stream().filter(variant -> !variant.hasConditions()).findFirst())
                .orElse(descriptions.get(0));
    }
}
