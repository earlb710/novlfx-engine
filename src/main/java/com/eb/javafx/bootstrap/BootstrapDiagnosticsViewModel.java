package com.eb.javafx.bootstrap;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;

/**
 * UI-neutral startup diagnostics for display in application-owned startup or error screens.
 */
public record BootstrapDiagnosticsViewModel(
        boolean complete,
        List<BootstrapPhaseSummaryViewModel> phaseSummaries,
        String elapsedTime) {
    public BootstrapDiagnosticsViewModel {
        phaseSummaries = ImmutableCollections.copyList(
                Validation.requireNonNull(phaseSummaries, "Bootstrap phase summaries are required."));
        elapsedTime = Validation.requireNonBlank(elapsedTime, "Elapsed bootstrap time is required.");
    }

    public List<String> lines() {
        List<String> lines = new ArrayList<>();
        lines.add("Startup diagnostics: " + (complete ? "complete" : "incomplete"));
        lines.addAll(phaseSummaries.stream().map(BootstrapPhaseSummaryViewModel::line).toList());
        lines.add("Elapsed startup time: " + elapsedTime);
        return List.copyOf(lines);
    }
}
