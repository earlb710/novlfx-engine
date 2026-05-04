package com.eb.javafx.bootstrap;

import com.eb.javafx.util.Validation;

import java.util.List;

/** Immutable result of applying an app-defined bootstrap completeness policy. */
public record BootstrapCompletenessReport(List<BootstrapCompletenessProblem> problems) {
    public BootstrapCompletenessReport {
        problems = List.copyOf(Validation.requireNonNull(problems, "Bootstrap completeness problems are required."));
    }

    public boolean complete() {
        return problems.isEmpty();
    }

    public List<String> lines() {
        if (complete()) {
            return List.of("Bootstrap completeness policy passed.");
        }
        return problems.stream()
                .map(problem -> problem.category() + " " + problem.id() + ": " + problem.message())
                .toList();
    }

    public void requireComplete() {
        if (!complete()) {
            throw new IllegalStateException(String.join("; ", lines()));
        }
    }
}
