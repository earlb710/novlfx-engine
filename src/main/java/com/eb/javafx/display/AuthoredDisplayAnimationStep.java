package com.eb.javafx.display;

import com.eb.javafx.util.Validation;

/** One parsed authored animation command with source line metadata. */
public final class AuthoredDisplayAnimationStep {
    private final int lineNumber;
    private final DisplayAnimationStep step;

    public AuthoredDisplayAnimationStep(int lineNumber, DisplayAnimationStep step) {
        this.lineNumber = Validation.requirePositive(lineNumber, "Authored animation step line number must be positive.");
        this.step = Validation.requireNonNull(step, "Authored animation step is required.");
    }

    public int lineNumber() {
        return lineNumber;
    }

    public DisplayAnimationStep step() {
        return step;
    }
}
