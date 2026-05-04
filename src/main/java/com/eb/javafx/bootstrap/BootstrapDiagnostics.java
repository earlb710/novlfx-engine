package com.eb.javafx.bootstrap;

import com.eb.javafx.util.Validation;

import java.util.EnumSet;
import java.util.List;

/**
 * Content-neutral helpers for formatting and validating bootstrap diagnostics.
 */
public final class BootstrapDiagnostics {
    private static final String DEFAULT_PHASE_MESSAGE = "No diagnostic message recorded.";

    private BootstrapDiagnostics() {
    }

    public static void requireComplete(BootstrapReport report) {
        Validation.requireNonNull(report, "Bootstrap report is required.");
        if (!report.isComplete()) {
            throw new IllegalStateException("JavaFX bootstrap did not complete required phases: "
                    + missingPhaseNames(report));
        }
    }

    public static BootstrapDiagnosticsViewModel viewModel(BootstrapReport report) {
        Validation.requireNonNull(report, "Bootstrap report is required.");
        return new BootstrapDiagnosticsViewModel(
                report.isComplete(),
                phaseSummaries(report),
                report.elapsedTime());
    }

    public static List<String> phaseLines(BootstrapReport report) {
        return viewModel(report).lines();
    }

    public static List<BootstrapPhaseSummaryViewModel> phaseSummaries(BootstrapReport report) {
        Validation.requireNonNull(report, "Bootstrap report is required.");
        return List.of(BootstrapPhase.values()).stream()
                .map(phase -> new BootstrapPhaseSummaryViewModel(
                        phase,
                        report.completedPhases().contains(phase),
                        report.phaseMessages().getOrDefault(phase, DEFAULT_PHASE_MESSAGE)))
                .toList();
    }

    public static List<BootstrapPhase> missingPhases(BootstrapReport report) {
        Validation.requireNonNull(report, "Bootstrap report is required.");
        EnumSet<BootstrapPhase> missing = EnumSet.allOf(BootstrapPhase.class);
        missing.removeAll(report.completedPhases());
        return missing.stream().toList();
    }

    private static List<String> missingPhaseNames(BootstrapReport report) {
        return missingPhases(report).stream().map(Enum::name).toList();
    }
}
