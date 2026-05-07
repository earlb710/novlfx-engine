package com.eb.javafx.scene;

import com.eb.javafx.save.GameplayStateSnapshot;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.Map;

/** Immutable checkpoint at a user-visible scene-flow boundary. */
public final class SceneCheckpoint {
    private final int sequence;
    private final SceneFlowState state;
    private final String sceneId;
    private final int stepIndex;
    private final String stepId;
    private final SceneStepType stepType;
    private final SceneCheckpointPayload payload;
    private final boolean rollbackBlocked;
    private final boolean rollbackFixed;
    private final GameplayStateSnapshot gameStateSnapshot;
    private final Map<String, String> metadata;

    public SceneCheckpoint(
            int sequence,
            SceneFlowState state,
            String sceneId,
            int stepIndex,
            String stepId,
            SceneStepType stepType,
            SceneCheckpointPayload payload,
            boolean rollbackBlocked,
            boolean rollbackFixed,
            Map<String, String> metadata) {
        this(
                sequence,
                state,
                sceneId,
                stepIndex,
                stepId,
                stepType,
                payload,
                rollbackBlocked,
                rollbackFixed,
                null,
                metadata);
    }

    public SceneCheckpoint(
            int sequence,
            SceneFlowState state,
            String sceneId,
            int stepIndex,
            String stepId,
            SceneStepType stepType,
            SceneCheckpointPayload payload,
            boolean rollbackBlocked,
            boolean rollbackFixed,
            GameplayStateSnapshot gameStateSnapshot,
            Map<String, String> metadata) {
        if (sequence < 0) {
            throw new IllegalArgumentException("Scene checkpoint sequence must not be negative.");
        }
        this.sequence = sequence;
        this.state = Validation.requireNonNull(state, "Scene checkpoint state is required.");
        this.sceneId = Validation.requireNonBlank(sceneId, "Scene checkpoint scene id is required.");
        if (stepIndex < 0) {
            throw new IllegalArgumentException("Scene checkpoint step index must not be negative.");
        }
        this.stepIndex = stepIndex;
        this.stepId = Validation.requireNonBlank(stepId, "Scene checkpoint step id is required.");
        this.stepType = Validation.requireNonNull(stepType, "Scene checkpoint step type is required.");
        this.payload = payload;
        this.rollbackBlocked = rollbackBlocked;
        this.rollbackFixed = rollbackFixed;
        this.gameStateSnapshot = gameStateSnapshot;
        this.metadata = ImmutableCollections.copyMap(metadata);
    }

    public static SceneCheckpoint fromResult(int sequence, SceneExecutionResult result, boolean rollbackBlocked, boolean rollbackFixed) {
        return fromResult(sequence, result, rollbackBlocked, rollbackFixed, null);
    }

    public static SceneCheckpoint fromResult(
            int sequence,
            SceneExecutionResult result,
            boolean rollbackBlocked,
            boolean rollbackFixed,
            GameplayStateSnapshot gameStateSnapshot) {
        Validation.requireNonNull(result, "Scene execution result is required.");
        SceneStep step = Validation.requireNonNull(result.step(), "Scene checkpoint step is required.");
        return new SceneCheckpoint(
                sequence,
                result.state(),
                result.state().activeSceneId(),
                result.state().stepIndex(),
                step.id(),
                step.type(),
                null,
                rollbackBlocked,
                rollbackFixed,
                gameStateSnapshot,
                Map.of());
    }

    public int sequence() {
        return sequence;
    }

    public SceneFlowState state() {
        return state;
    }

    public String sceneId() {
        return sceneId;
    }

    public int stepIndex() {
        return stepIndex;
    }

    public String stepId() {
        return stepId;
    }

    public SceneStepType stepType() {
        return stepType;
    }

    public SceneCheckpointPayload payload() {
        return payload;
    }

    public boolean rollbackBlocked() {
        return rollbackBlocked;
    }

    public boolean rollbackFixed() {
        return rollbackFixed;
    }

    public GameplayStateSnapshot gameStateSnapshot() {
        return gameStateSnapshot;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public SceneCheckpoint withPayload(SceneCheckpointPayload payload) {
        return new SceneCheckpoint(sequence, state, sceneId, stepIndex, stepId, stepType, payload, rollbackBlocked, rollbackFixed, gameStateSnapshot, metadata);
    }

    public SceneCheckpoint withRollbackBlocked(boolean rollbackBlocked) {
        return new SceneCheckpoint(sequence, state, sceneId, stepIndex, stepId, stepType, payload, rollbackBlocked, rollbackFixed, gameStateSnapshot, metadata);
    }

    public SceneCheckpoint withRollbackFixed(boolean rollbackFixed) {
        return new SceneCheckpoint(sequence, state, sceneId, stepIndex, stepId, stepType, payload, rollbackBlocked, rollbackFixed, gameStateSnapshot, metadata);
    }

    boolean matches(SceneExecutionResult result) {
        return result.step() != null
                && sceneId.equals(result.state().activeSceneId())
                && stepIndex == result.state().stepIndex()
                && stepId.equals(result.step().id())
                && stepType == result.step().type();
    }
}
