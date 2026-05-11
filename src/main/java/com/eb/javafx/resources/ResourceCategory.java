package com.eb.javafx.resources;

import java.util.Locale;

/**
 * Strict, library-defined categories of resource directories cataloged by {@link ResourceRegistry}.
 *
 * <p>Lookups are isolated per category: an image lookup only searches roots registered under
 * {@link #IMAGES}, never falling through to {@link #UI} or other categories.</p>
 */
public enum ResourceCategory {
    BOOTSTRAP,
    FONTS,
    IMAGES,
    SUPPORT,
    UI;

    /** Returns the lowercase JSON config key for this category (e.g. {@code "images"}). */
    public String configKey() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Parses a JSON config key into a category, accepting any case. */
    public static ResourceCategory fromConfigKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Resource category key is required.");
        }
        try {
            return ResourceCategory.valueOf(key.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown resource category: " + key, exception);
        }
    }
}
