package com.eb.javafx.inventory;

import com.eb.javafx.gamesupport.IdentifiedDefinition;
import com.eb.javafx.util.Validation;

/** Generic equipment slot descriptor for wearable items. */
public record WearableSlotDefinition(String id, String title, boolean exclusive) implements IdentifiedDefinition {
    public WearableSlotDefinition {
        id = Validation.requireNonBlank(id, "Wearable slot id is required.");
        title = Validation.requireNonBlank(title, "Wearable slot title is required.");
    }
}
