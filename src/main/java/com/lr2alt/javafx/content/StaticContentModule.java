package com.lr2alt.javafx.content;

import com.lr2alt.javafx.display.ImageDisplayRegistry;

/**
 * Registers app/game-specific content after reusable platform registries are ready.
 */
public interface StaticContentModule {
    void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry);

    void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry);
}
