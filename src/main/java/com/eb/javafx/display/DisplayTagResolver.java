package com.eb.javafx.display;

import java.util.Objects;

/** Resolves semantic display tags to concrete display IDs, layers, and optional transform presets. */
public final class DisplayTagResolver {
    private final DisplayTagRegistry tagRegistry;

    public DisplayTagResolver(DisplayTagRegistry tagRegistry) {
        this.tagRegistry = Objects.requireNonNull(tagRegistry, "tagRegistry");
    }

    public DisplayTagResolution resolve(String tag) {
        DisplayTagDefinition def = tagRegistry.require(tag);
        return new DisplayTagResolution(def.displayId(), def.layer(), def.transformPresetName().orElse(null));
    }
}
