package com.eb.javafx.diagnostics;

import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;

/** Runs registered diagnostic checks and combines their reports in registration order. */
public final class DiagnosticRegistry {
    private final List<DiagnosticCheck> checks = new ArrayList<>();

    public void register(DiagnosticCheck check) {
        checks.add(Validation.requireNonNull(check, "Diagnostic check is required."));
    }

    public DiagnosticReport runChecks() {
        List<DiagnosticProblem> problems = new ArrayList<>();
        for (DiagnosticCheck check : checks) {
            problems.addAll(check.run());
        }
        return new DiagnosticReport(problems);
    }
}
