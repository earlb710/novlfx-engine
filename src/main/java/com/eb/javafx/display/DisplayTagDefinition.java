package com.eb.javafx.display;

import com.eb.javafx.util.Validation;
import java.util.Optional;

/** Maps a semantic author tag to a concrete display ID, layer, optional transform preset, and render order. */
public final class DisplayTagDefinition {
    private final String tag;
    private final String displayId;
    private final DisplayLayer layer;
    private final String transformPresetName;
    private final int zorder;

    public DisplayTagDefinition(String tag, String displayId, DisplayLayer layer, String transformPresetName) {
        this(tag, displayId, layer, transformPresetName, 0);
    }

    public DisplayTagDefinition(String tag, String displayId, DisplayLayer layer, String transformPresetName, int zorder) {
        this.tag = Validation.requireNonBlank(tag, "Display tag is required.");
        this.displayId = Validation.requireNonBlank(displayId, "Display tag displayId is required.");
        this.layer = Validation.requireNonNull(layer, "Display tag layer is required.");
        this.transformPresetName = transformPresetName;
        this.zorder = zorder;
    }

    public String tag() { return tag; }
    public String displayId() { return displayId; }
    public DisplayLayer layer() { return layer; }
    public Optional<String> transformPresetName() { return Optional.ofNullable(transformPresetName); }
    public int zorder() { return zorder; }

    public DisplayTagDefinition withZorder(int zorder) {
        return new DisplayTagDefinition(tag, displayId, layer, transformPresetName, zorder);
    }
}
