package com.eb.javafx.display;

import com.eb.javafx.util.Validation;

import java.util.Optional;

/** Maps a semantic author tag to a concrete display ID, layer, and optional transform preset. */
public final class DisplayTagDefinition {
    private final String tag;
    private final String displayId;
    private final DisplayLayer layer;
    private final String transformPresetName;

    public DisplayTagDefinition(String tag, String displayId, DisplayLayer layer, String transformPresetName) {
        this.tag = Validation.requireNonBlank(tag, "Display tag is required.");
        this.displayId = Validation.requireNonBlank(displayId, "Display tag displayId is required.");
        this.layer = Validation.requireNonNull(layer, "Display tag layer is required.");
        this.transformPresetName = transformPresetName;
    }

    public String tag() { return tag; }
    public String displayId() { return displayId; }
    public DisplayLayer layer() { return layer; }
    public Optional<String> transformPresetName() { return Optional.ofNullable(transformPresetName); }
}
