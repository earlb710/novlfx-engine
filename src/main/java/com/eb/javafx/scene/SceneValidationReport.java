package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.List;

/** Immutable scene validation report containing graph summaries and diagnostics. */
public record SceneValidationReport(
        List<SceneGraphSummary> summaries,
        List<SceneValidationProblem> problems) {
    public SceneValidationReport {
        summaries = List.copyOf(Validation.requireNonNull(summaries, "Scene graph summaries are required."));
        problems = List.copyOf(Validation.requireNonNull(problems, "Scene validation problems are required."));
    }

    public boolean hasErrors() {
        return problems.stream().anyMatch(problem -> problem.severity() == SceneValidationSeverity.ERROR);
    }

    public boolean hasWarnings() {
        return problems.stream().anyMatch(problem -> problem.severity() == SceneValidationSeverity.WARNING);
    }

    public void throwIfInvalid() {
        problems.stream()
                .filter(problem -> problem.severity() == SceneValidationSeverity.ERROR)
                .findFirst()
                .ifPresent(problem -> {
                    throw new IllegalStateException(problem.message());
                });
    }
}
