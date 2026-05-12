package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.Map;

public record RollbackEntry(SceneFlowState flowState, Map<String, Object> contributorValues) {
    public RollbackEntry {
        Validation.requireNonNull(flowState, "flowState");
        contributorValues = Map.copyOf(Validation.requireNonNull(contributorValues, "contributorValues"));
    }
}
