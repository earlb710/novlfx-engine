package com.eb.javafx.content;

import com.eb.javafx.display.ImageDisplayRegistry;

/**
 * Registers app/game-specific content after reusable platform registries are ready.
 */
public interface StaticContentModule {
    void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry);

    void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry);
}
