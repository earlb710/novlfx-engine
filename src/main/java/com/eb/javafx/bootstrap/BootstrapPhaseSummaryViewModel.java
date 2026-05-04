package com.eb.javafx.bootstrap;

import com.eb.javafx.util.Validation;

/**
 * UI-neutral diagnostic summary for one bootstrap phase.
 */
public record BootstrapPhaseSummaryViewModel(BootstrapPhase phase, boolean completed, String message) {
    public BootstrapPhaseSummaryViewModel {
        Validation.requireNonNull(phase, "Bootstrap phase is required.");
        message = Validation.requireNonBlank(message, "Bootstrap phase message is required.");
    }

    public String line() {
        return phase.name() + ": " + message;
    }
}
