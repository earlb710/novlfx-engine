package com.eb.javafx.display;

import com.eb.javafx.util.Validation;
import java.util.Optional;

/** Image ref and optional condition string for one variant of a layered image slot. */
public final class LayeredImageVariant {
    private final String imageRef;
    private final String conditionExpression;

    public LayeredImageVariant(String imageRef, String conditionExpression) {
        this.imageRef = Validation.requireNonBlank(imageRef, "LayeredImageVariant imageRef is required.");
        this.conditionExpression = conditionExpression;
    }

    public String imageRef() { return imageRef; }
    public Optional<String> conditionExpression() { return Optional.ofNullable(conditionExpression); }
}
