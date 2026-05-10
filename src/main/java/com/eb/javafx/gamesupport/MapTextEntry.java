package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

/** Localized text for a single authored map. */
public record MapTextEntry(String mapId, String description) implements IdentifiedDefinition {
    /** JSON fallback used when a map text entry omits its localized description. */
    public static final String DEFAULT_DESCRIPTION = "Main Map";

    public MapTextEntry {
        mapId = Validation.requireNonBlank(mapId, "Map text mapId must not be blank.");
        description = Validation.requireNonBlank(description, "Map text description must not be blank.");
    }

    @Override
    public String id() {
        return mapId;
    }
}
