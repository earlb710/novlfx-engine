package com.eb.javafx.ui;

import com.eb.javafx.display.DisplayLayer;
import com.eb.javafx.util.Validation;

/**
 * Reusable display-preview row for image binding diagnostics and app-owned previews.
 *
 * <p>The record keeps the authored image id, source path, layer, and asset-resolution state together so
 * diagnostic screens can report missing files without depending on application-specific display models.</p>
 */
public record DisplayPreviewBinding(String imageId, String sourcePath, DisplayLayer layer, boolean assetResolved) {
    public DisplayPreviewBinding {
        Validation.requireNonBlank(imageId, "Display preview image id is required.");
        Validation.requireNonBlank(sourcePath, "Display preview source path is required.");
        Validation.requireNonNull(layer, "Display preview layer is required.");
    }
}
