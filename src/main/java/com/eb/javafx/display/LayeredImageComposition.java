package com.eb.javafx.display;

import java.util.List;
import java.util.Objects;

/** Resolved ordered list of (layer name → image ref) pairs delivered to the adapter for composite rendering. */
public final class LayeredImageComposition {
    private final String definitionId;
    private final List<LayeredCompositionEntry> entries;

    public LayeredImageComposition(String definitionId, List<LayeredCompositionEntry> entries) {
        this.definitionId = Objects.requireNonNull(definitionId, "definitionId");
        this.entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public String definitionId() { return definitionId; }
    public List<LayeredCompositionEntry> entries() { return entries; }
}
