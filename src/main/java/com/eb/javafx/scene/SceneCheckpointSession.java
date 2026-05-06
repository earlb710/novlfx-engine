package com.eb.javafx.scene;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.util.Validation;

/** Checkpoint-aware headless session wrapper around {@link SceneExecutor}. */
public final class SceneCheckpointSession {
    private final SceneExecutor executor;
    private final ActionContext context;
    private SceneCheckpointLog checkpointLog = SceneCheckpointLog.empty();
    private SceneExecutionResult currentResult;

    public SceneCheckpointSession(SceneExecutor executor, ActionContext context) {
        this.executor = Validation.requireNonNull(executor, "Scene executor is required.");
        this.context = Validation.requireNonNull(context, "Action context is required.");
    }

    public SceneExecutionResult start(String sceneId) {
        return setCurrentResult(executor.advanceUntilPause(context, executor.start(sceneId)));
    }

    public SceneExecutionResult advanceUntilPause(SceneFlowState state) {
        return setCurrentResult(executor.advanceUntilPause(context, state));
    }

    public SceneExecutionResult continueFromText() {
        SceneCheckpoint checkpoint = requireCurrentCheckpoint();
        checkpointCurrentInteractionResult(SceneCheckpointPayload.textContinuation());
        return setCurrentResult(executor.continueFromText(context, checkpoint.state()));
    }

    public SceneExecutionResult selectChoice(String choiceId) {
        SceneCheckpoint checkpoint = requireCurrentCheckpoint();
        String value = currentResult.availableChoices().stream()
                .filter(choice -> choice.id().equals(choiceId))
                .findFirst()
                .map(choice -> choice.metadata().getOrDefault("value", choice.id()))
                .orElse(choiceId);
        checkpointCurrentInteractionResult(SceneCheckpointPayload.choiceSelection(choiceId, value));
        return setCurrentResult(executor.selectChoice(context, checkpoint.state(), choiceId));
    }

    public SceneCheckpoint checkpointCurrentInteractionResult(SceneCheckpointPayload payload) {
        checkpointLog = checkpointLog.checkpointCurrentInteractionResult(payload);
        return checkpointLog.currentCheckpoint();
    }

    public boolean rollbackAllowed() {
        return checkpointLog.rollbackAllowed();
    }

    public SceneExecutionResult rollbackOneCheckpoint() {
        checkpointLog = checkpointLog.rollbackOneCheckpoint();
        return setCurrentResultWithoutRecording(executor.advanceUntilPause(context, requireCurrentCheckpoint().state()));
    }

    public boolean rollForwardAllowed() {
        return checkpointLog.rollForwardAllowed();
    }

    public SceneExecutionResult rollForwardUsingStoredCheckpointData() {
        SceneCheckpoint checkpoint = requireCurrentCheckpoint();
        SceneCheckpointPayload payload = Validation.requireNonNull(
                checkpoint.payload(),
                "Current scene checkpoint has no replay payload.");
        SceneExecutionResult result = switch (payload.kind()) {
            case TEXT_CONTINUATION -> executor.continueFromText(context, checkpoint.state());
            case CHOICE_SELECTION -> executor.selectChoice(context, checkpoint.state(), payload.choiceId());
            case INPUT_RESULT -> throw new IllegalStateException("Scene input replay is not yet connected to an executor interaction.");
        };
        return setCurrentResult(result);
    }

    public void blockRollback() {
        checkpointLog = checkpointLog.blockRollback();
    }

    public void fixRollback() {
        checkpointLog = checkpointLog.fixRollback();
    }

    public SceneCheckpointLog checkpointLog() {
        return checkpointLog;
    }

    public SceneExecutionResult currentResult() {
        return currentResult;
    }

    private SceneExecutionResult setCurrentResult(SceneExecutionResult result) {
        currentResult = result;
        checkpointLog = checkpointLog.recordVisibleBoundary(result);
        return result;
    }

    private SceneExecutionResult setCurrentResultWithoutRecording(SceneExecutionResult result) {
        currentResult = result;
        return result;
    }

    private SceneCheckpoint requireCurrentCheckpoint() {
        return Validation.requireNonNull(checkpointLog.currentCheckpoint(), "No current scene checkpoint is available.");
    }
}
