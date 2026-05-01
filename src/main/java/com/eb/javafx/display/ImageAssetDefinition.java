package com.eb.javafx.display;

/**
 * Static image alias migrated from Ren'Py image declarations.
 *
 * <p>The ID is the stable name used by migrated screens, {@code sourcePath}
 * points at an authored image asset under the game tree, {@code transformId} is
 * optional, and {@code layer} controls display composition order.</p>
 */
public final class ImageAssetDefinition {
    private final String id;
    private final String sourcePath;
    private final String transformId;
    private final DisplayLayer layer;

    /**
     * Creates a validated static image definition.
     *
     * @param id non-blank image alias
     * @param sourcePath non-blank authored asset path
     * @param transformId optional transform ID, blank/null when no transform applies
     * @param layer display layer used for composition and diagnostics
     */
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

    /** Returns whether this asset references a non-blank transform ID. */
    public boolean hasTransform() {
        return transformId != null && !transformId.isBlank();
    }

    public DisplayLayer layer() {
        return layer;
    }
}
