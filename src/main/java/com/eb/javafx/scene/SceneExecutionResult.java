package com.eb.javafx.scene;

import com.eb.javafx.audio.SoundRequest;

import java.util.List;
import java.util.Optional;

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
    private final boolean canRollback;
    private final SoundRequest voiceRequest;

    public SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, String message, boolean canRollback) {
        this(status, state, step, availableChoices, message, canRollback, null);
    }

    public SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, String message) {
        this(status, state, step, availableChoices, message, false, null);
    }

    private SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, String message, boolean canRollback, SoundRequest voiceRequest) {
        this.status = status;
        this.state = state;
        this.step = step;
        this.availableChoices = List.copyOf(availableChoices);
        this.message = message;
        this.canRollback = canRollback;
        this.voiceRequest = voiceRequest;
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

    public boolean canRollback() {
        return canRollback;
    }

    public Optional<SoundRequest> voiceRequest() {
        return Optional.ofNullable(voiceRequest);
    }

    public SceneExecutionResult withVoiceRequest(SoundRequest voiceRequest) {
        return new SceneExecutionResult(status, state, step, availableChoices, message, canRollback, voiceRequest);
    }
}
