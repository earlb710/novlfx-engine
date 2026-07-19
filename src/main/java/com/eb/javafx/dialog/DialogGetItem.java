package com.eb.javafx.dialog;

import java.util.Optional;

/**
 * Declares that a {@link DialogNode.Line} grants the player an item when it plays.
 *
 * @param itemId      catalogue id of the item (see {@code AltLifeItemCatalog})
 * @param description optional line-specific note about how/why it's acquired; falls back to the
 *                    catalogue item's own description when blank
 */
public record DialogGetItem(String itemId, String description) {

    public DialogGetItem {
        itemId = itemId == null ? null : itemId.trim();
        description = (description == null || description.isBlank()) ? null : description.trim();
        if (itemId == null || itemId.isEmpty()) {
            throw new IllegalArgumentException("getItem requires an itemId.");
        }
    }

    /** Line-specific acquisition note, if authored. */
    public Optional<String> descriptionText() {
        return Optional.ofNullable(description);
    }
}
