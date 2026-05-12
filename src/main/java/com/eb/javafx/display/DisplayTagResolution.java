package com.eb.javafx.display;

import java.util.Optional;

/** Result of resolving a semantic display tag to concrete display coordinates. */
public final class DisplayTagResolution {
    private final String displayId;
    private final DisplayLayer layer;
    private final String transformPresetName;

    DisplayTagResolution(String displayId, DisplayLayer layer, String transformPresetName) {
        this.displayId = displayId;
        this.layer = layer;
        this.transformPresetName = transformPresetName;
    }

    public String displayId() { return displayId; }
    public DisplayLayer layer() { return layer; }
    public Optional<String> transformPresetName() { return Optional.ofNullable(transformPresetName); }
}
