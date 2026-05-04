package com.eb.javafx.diagnostics;

import com.eb.javafx.util.ImmutableCollections;

import java.util.List;

/** Immutable collection of structured diagnostics. */
public record DiagnosticReport(List<DiagnosticProblem> problems) {
    public DiagnosticReport {
        problems = ImmutableCollections.copyList(problems);
    }

    public boolean hasErrors() {
        return problems.stream().anyMatch(problem -> problem.severity() == DiagnosticSeverity.ERROR);
    }

    public List<DiagnosticProblem> bySeverity(DiagnosticSeverity severity) {
        return problems.stream().filter(problem -> problem.severity() == severity).toList();
    }
}
