package com.eb.javafx.display;

import com.eb.javafx.util.Validation;
import java.util.List;
import java.util.Objects;

/** Condition-based layered sprite definition: id, display tag reference, and ordered layer slots. */
public final class LayeredImageDefinition {
    private final String id;
    private final String displayTagId;
    private final List<LayeredImageLayer> layers;

    public LayeredImageDefinition(String id, String displayTagId, List<LayeredImageLayer> layers) {
        this.id = Validation.requireNonBlank(id, "LayeredImageDefinition id is required.");
        this.displayTagId = Validation.requireNonBlank(displayTagId, "LayeredImageDefinition displayTagId is required.");
        this.layers = List.copyOf(Objects.requireNonNull(layers, "layers"));
    }

    public String id() { return id; }
    public String displayTagId() { return displayTagId; }
    public List<LayeredImageLayer> layers() { return layers; }
}
