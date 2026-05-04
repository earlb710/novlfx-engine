package com.eb.javafx.inventory;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Per-save unlocked wearable and named outfit state. */
public final class WardrobeState {
    private final Set<String> unlockedWearableIds = new LinkedHashSet<>();
    private final Map<String, OutfitState> outfits = new LinkedHashMap<>();

    public void unlock(String itemId, WardrobeCatalog catalog) {
        String checkedItemId = Validation.requireNonBlank(itemId, "Wearable item id is required.");
        if (Validation.requireNonNull(catalog, "Wardrobe catalog is required.").wearable(checkedItemId).isEmpty()) {
            throw new IllegalArgumentException("Unknown wearable item: " + checkedItemId);
        }
        unlockedWearableIds.add(checkedItemId);
    }

    public boolean isUnlocked(String itemId) {
        return unlockedWearableIds.contains(itemId);
    }

    public void saveOutfit(String outfitId, OutfitState outfit) {
        outfits.put(
                Validation.requireNonBlank(outfitId, "Outfit id is required."),
                Validation.requireNonNull(outfit, "Outfit state is required."));
    }

    public Optional<OutfitState> outfit(String outfitId) {
        return Optional.ofNullable(outfits.get(outfitId));
    }

    public Set<String> unlockedWearableIds() {
        return Set.copyOf(unlockedWearableIds);
    }
}
