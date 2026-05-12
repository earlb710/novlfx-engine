package com.eb.javafx.scene;

import java.util.List;
import java.util.Optional;

/**
 * Immutable scene execution result for UI adapters and tests.
 *
 * <p>Each result bundles the executor status, updated flow state, current UI-visible step, available choices,
 * optional hotspot map view model, optional talking animation cue, rollback availability, and any terminal or
 * diagnostic message produced while advancing the scene.</p>
 */
public final class SceneExecutionResult {
    private final SceneExecutionStatus status;
    private final SceneFlowState state;
    private final SceneStep step;
    private final List<SceneChoice> availableChoices;
    private final HotspotMapViewModel hotspotMapViewModel;
    private final TalkingAnimationCue talkingCue;
    private final String message;
    private final boolean canRollback;

    public SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, HotspotMapViewModel hotspotMapViewModel,
            TalkingAnimationCue talkingCue, String message, boolean canRollback) {
        this.status = status;
        this.state = state;
        this.step = step;
        this.availableChoices = List.copyOf(availableChoices);
        this.hotspotMapViewModel = hotspotMapViewModel;
        this.talkingCue = talkingCue;
        this.message = message;
        this.canRollback = canRollback;
    }

    public SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, String message, boolean canRollback) {
        this(status, state, step, availableChoices, null, null, message, canRollback);
    }

    public SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, HotspotMapViewModel hotspotMapViewModel, String message) {
        this(status, state, step, availableChoices, hotspotMapViewModel, null, message, false);
    }

    public SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, HotspotMapViewModel hotspotMapViewModel,
            TalkingAnimationCue talkingCue, String message) {
        this(status, state, step, availableChoices, hotspotMapViewModel, talkingCue, message, false);
    }

    public SceneExecutionResult(SceneExecutionStatus status, SceneFlowState state, SceneStep step,
            List<SceneChoice> availableChoices, String message) {
        this(status, state, step, availableChoices, null, null, message, false);
    }

    public SceneExecutionStatus status() { return status; }
    public SceneFlowState state() { return state; }
    public SceneStep step() { return step; }
    public List<SceneChoice> availableChoices() { return availableChoices; }
    public Optional<HotspotMapViewModel> hotspotMapViewModel() { return Optional.ofNullable(hotspotMapViewModel); }
    public Optional<TalkingAnimationCue> talkingCue() { return Optional.ofNullable(talkingCue); }
    public String message() { return message; }
    public boolean canRollback() { return canRollback; }
}
