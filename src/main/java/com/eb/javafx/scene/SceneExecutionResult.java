package com.eb.javafx.scene;

import java.util.List;

/**
 * Immutable scene execution result for UI adapters and tests.
 *
 * <p>Each result bundles the executor status, updated flow state, current UI-visible step, available choices,
 * and any terminal or diagnostic message produced while advancing the scene.</p>
 */
public final class SceneExecutionResult {
    private final SceneExecutionStatus status;
    private final SceneFlowState state;
    private final SceneStep step;
    private final List<SceneChoice> availableChoices;
    private final String message;

    public SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step, List<SceneChoice> availableChoices, String message) {
        this.status = status;
        this.state = state;
        this.step = step;
        this.availableChoices = List.copyOf(availableChoices);
        this.message = message;
    }

    public SceneExecutionStatus status() {
        return status;
    }

    public SceneFlowState state() {
        return state;
    }

    public SceneStep step() {
        return step;
    }

    public List<SceneChoice> availableChoices() {
        return availableChoices;
    }

    public String message() {
        return message;
    }
}
