package com.eb.javafx.display;

import com.eb.javafx.util.Validation;
import java.util.List;

/** Named composition slot containing ordered image variants selected by condition. */
public final class LayeredImageLayer {
    private final String name;
    private final List<LayeredImageVariant> variants;

    public LayeredImageLayer(String name, List<LayeredImageVariant> variants) {
        this.name = Validation.requireNonBlank(name, "LayeredImageLayer name is required.");
        this.variants = List.copyOf(Validation.requireNonEmpty(variants, "LayeredImageLayer variants are required."));
    }

    public String name() { return name; }
    public List<LayeredImageVariant> variants() { return variants; }
}
