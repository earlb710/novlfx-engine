package com.eb.javafx.assets;

import com.eb.javafx.util.Validation;

/** One asset validation problem suitable for startup diagnostics. */
public record AssetValidationProblem(String assetId, String relativePath, String message) {
    public AssetValidationProblem {
        assetId = Validation.requireNonBlank(assetId, "Asset id is required.");
        relativePath = Validation.requireNonBlank(relativePath, "Asset relative path is required.");
        message = Validation.requireNonBlank(message, "Asset validation message is required.");
    }
}
