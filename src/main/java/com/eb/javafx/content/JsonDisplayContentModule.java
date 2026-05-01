package com.eb.javafx.content;

import com.eb.javafx.display.DisplayDefinitionJsonLoader;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.util.Validation;

import java.nio.file.Path;

/** Static content module that registers generic display definitions from app-owned JSON. */
public final class JsonDisplayContentModule implements StaticContentModule {
    private final Path jsonPath;

    public JsonDisplayContentModule(Path jsonPath) {
        this.jsonPath = Validation.requireNonNull(jsonPath, "Display definition JSON path is required.");
    }

    @Override
    public void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
        DisplayDefinitionJsonLoader.loadInto(jsonPath, imageDisplayRegistry);
    }

    @Override
    public void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
        imageDisplayRegistry.validateDisplayContent();
    }
}
