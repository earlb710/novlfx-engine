package com.eb.javafx.display;

import com.eb.javafx.util.Validation;

/**
 * Static image alias migrated from visual novel image declarations.
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
        this.id = Validation.requireNonBlank(id, "Image asset id is required.");
        this.sourcePath = Validation.requireNonBlank(sourcePath, "Image asset source path is required.");
        this.transformId = transformId;
        this.layer = Validation.requireNonNull(layer, "Image asset layer is required.");
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
