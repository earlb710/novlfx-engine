package com.eb.javafx.assets;

import com.eb.javafx.util.ImmutableCollections;

import java.util.List;

/** Deterministic validation report for an asset catalog. */
public record AssetValidationReport(List<AssetValidationProblem> problems) {
    public AssetValidationReport {
        problems = ImmutableCollections.copyList(problems);
    }

    public boolean hasProblems() {
        return !problems.isEmpty();
    }
}
