package com.eb.javafx.display;

/**
 * Static image alias migrated from Ren'Py image declarations.
 */
public final class ImageAssetDefinition {
    private final String id;
    private final String sourcePath;
    private final String transformId;
    private final DisplayLayer layer;

    public ImageAssetDefinition(String id, String sourcePath, String transformId, DisplayLayer layer) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Image asset id is required.");
        }
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalArgumentException("Image asset source path is required.");
        }
        if (layer == null) {
            throw new IllegalArgumentException("Image asset layer is required.");
        }
        this.id = id;
        this.sourcePath = sourcePath;
        this.transformId = transformId;
        this.layer = layer;
    }

    public String id() {
        return id;
    }

    public String sourcePath() {
        return sourcePath;
    }

    public String transformId() {
        return transformId;
    }

    public boolean hasTransform() {
        return transformId != null && !transformId.isBlank();
    }

    public DisplayLayer layer() {
        return layer;
    }
}
