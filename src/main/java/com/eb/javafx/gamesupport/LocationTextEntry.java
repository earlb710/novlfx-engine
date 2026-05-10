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

    /**
     * Returns the first conditional variant whose conditions all match, otherwise the first unconditional variant,
     * otherwise the first authored description as a final fallback.
     */
    public LocationDescriptionVariant descriptionForConditions(Collection<String> activeConditions) {
        LocationDescriptionVariant fallback = null;
        for (LocationDescriptionVariant variant : descriptions) {
            if (variant.hasConditions() && variant.matchesConditions(activeConditions)) {
                return variant;
            }
            if (!variant.hasConditions() && fallback == null) {
                fallback = variant;
            }
        }
        return fallback == null ? descriptions.get(0) : fallback;
    }
}
