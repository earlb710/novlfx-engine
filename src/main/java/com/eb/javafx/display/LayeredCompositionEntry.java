package com.eb.javafx.display;

/** One resolved layer in a layered image composition: layer name and selected image ref. */
public final class LayeredCompositionEntry {
    private final String layerName;
    private final String imageRef;

    public LayeredCompositionEntry(String layerName, String imageRef) {
        this.layerName = layerName;
        this.imageRef = imageRef;
    }

    public String layerName() { return layerName; }
    public String imageRef() { return imageRef; }
}
