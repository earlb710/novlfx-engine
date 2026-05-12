package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;
import java.util.Optional;

public record CtcIndicatorDefinition(String imageRef, Optional<String> animationId, CtcPosition position) {
    public CtcIndicatorDefinition {
        Validation.requireNonBlank(imageRef, "imageRef");
        Validation.requireNonNull(animationId, "animationId");
        Validation.requireNonNull(position, "position");
    }
}
