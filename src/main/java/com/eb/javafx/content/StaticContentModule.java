package com.eb.javafx.content;

import com.eb.javafx.display.ImageDisplayRegistry;

/**
 * Registers app/game-specific content after reusable platform registries are ready.
 *
 * <p>Modules run after base content has been registered and before validation.
 * Implementations should be deterministic and safe to invoke during startup; they
 * should put authored definitions into the supplied registries without creating
 * mutable save state.</p>
 */
public interface StaticContentModule {
    /** Registers authored static definitions into the supplied content/display registries. */
    void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry);

    /** Validates that module-required definitions exist after all modules have registered. */
    void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry);
}
