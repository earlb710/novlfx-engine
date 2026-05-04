package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

/** Result from content-neutral movement validation. */
public record MovementValidationResult(boolean allowed, String reason) {
    public MovementValidationResult {
        if (!allowed) {
            reason = Validation.requireNonBlank(reason, "Movement block reason is required.");
        } else if (reason == null) {
            reason = "";
        }
    }

    public static MovementValidationResult permit() {
        return new MovementValidationResult(true, "");
    }

    public static MovementValidationResult blocked(String reason) {
        return new MovementValidationResult(false, reason);
    }
}
