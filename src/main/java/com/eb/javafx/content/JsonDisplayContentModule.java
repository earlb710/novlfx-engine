package com.eb.javafx.content;

import com.eb.javafx.display.DisplayDefinitionJsonLoader;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.resources.ResourceIo;
import com.eb.javafx.util.Validation;

import java.net.URL;
import java.nio.file.Path;

/** Static content module that registers generic display definitions from app-owned JSON. */
public final class JsonDisplayContentModule implements StaticContentModule {
    private final URL jsonUrl;

    public JsonDisplayContentModule(Path jsonPath) {
        this(ResourceIo.toUrl(Validation.requireNonNull(jsonPath, "Display definition JSON path is required.")));
    }

    public JsonDisplayContentModule(URL jsonUrl) {
        this.jsonUrl = Validation.requireNonNull(jsonUrl, "Display definition JSON URL is required.");
    }

    @Override
    public void register(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
        DisplayDefinitionJsonLoader.loadInto(jsonUrl, imageDisplayRegistry);
    }

    @Override
    public void validate(ContentRegistry contentRegistry, ImageDisplayRegistry imageDisplayRegistry) {
        imageDisplayRegistry.validateDisplayContent();
    }
}
