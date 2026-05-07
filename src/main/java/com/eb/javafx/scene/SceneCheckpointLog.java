package com.eb.javafx.scene;

import com.eb.javafx.save.GameplayStateSnapshot;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;

/** Immutable checkpoint history and rollback cursor for a scene-flow session. */
public final class SceneCheckpointLog {
    private final List<SceneCheckpoint> checkpoints;
    private final int cursor;
    private final int rollbackBarrierIndex;
    private final boolean rollbackFixed;

    public SceneCheckpointLog(List<SceneCheckpoint> checkpoints, int cursor, int rollbackBarrierIndex, boolean rollbackFixed) {
        this.checkpoints = List.copyOf(Validation.requireNonNull(checkpoints, "Scene checkpoints are required."));
        if (cursor < -1 || cursor >= checkpoints.size()) {
            throw new IllegalArgumentException("Scene checkpoint cursor is outside checkpoint history.");
        }
        if (rollbackBarrierIndex < -1 || rollbackBarrierIndex >= checkpoints.size()) {
            throw new IllegalArgumentException("Scene checkpoint rollback barrier is outside checkpoint history.");
        }
        this.cursor = cursor;
        this.rollbackBarrierIndex = rollbackBarrierIndex;
        this.rollbackFixed = rollbackFixed;
    }

    public static SceneCheckpointLog empty() {
        return new SceneCheckpointLog(List.of(), -1, -1, false);
    }

    public List<SceneCheckpoint> checkpoints() {
        return checkpoints;
    }

    public int cursor() {
        return cursor;
    }

    public int rollbackBarrierIndex() {
        return rollbackBarrierIndex;
    }

    public boolean rollbackFixed() {
        return rollbackFixed;
    }

    public SceneCheckpoint currentCheckpoint() {
        return cursor < 0 ? null : checkpoints.get(cursor);
    }

    public boolean rollbackAllowed() {
        return cursor > Math.max(rollbackBarrierIndex, 0);
    }

    public boolean rollForwardAllowed() {
        return currentCheckpoint() != null && currentCheckpoint().payload() != null;
    }

    public SceneCheckpointLog recordVisibleBoundary(SceneExecutionResult result) {
        return recordVisibleBoundary(result, null);
    }

    public SceneCheckpointLog recordVisibleBoundary(SceneExecutionResult result, GameplayStateSnapshot gameStateSnapshot) {
        if (!isCheckpointBoundary(result)) {
            return this;
        }
        if (cursor >= 0 && checkpoints.get(cursor).matches(result)) {
            return this;
        }
        if (cursor + 1 < checkpoints.size() && checkpoints.get(cursor + 1).matches(result)) {
            return new SceneCheckpointLog(checkpoints, cursor + 1, rollbackBarrierIndex, rollbackFixed);
        }
        List<SceneCheckpoint> updated = new ArrayList<>(checkpoints.subList(0, cursor + 1));
        updated.add(SceneCheckpoint.fromResult(updated.size(), result, false, rollbackFixed, gameStateSnapshot));
        int barrier = Math.min(rollbackBarrierIndex, updated.size() - 1);
        return new SceneCheckpointLog(updated, updated.size() - 1, barrier, rollbackFixed);
    }

    public SceneCheckpointLog checkpointCurrentInteractionResult(SceneCheckpointPayload payload) {
        Validation.requireNonNull(payload, "Scene checkpoint payload is required.");
        SceneCheckpoint current = Validation.requireNonNull(currentCheckpoint(), "No current scene checkpoint is available.");
        if (rollbackFixed && current.payload() != null && !current.payload().sameReplayData(payload)) {
            throw new IllegalStateException("Rollback is fixed; prior scene checkpoint choices cannot change.");
        }
        List<SceneCheckpoint> updated = new ArrayList<>(checkpoints.subList(0, cursor + 1));
        updated.set(cursor, current.withPayload(payload).withRollbackFixed(rollbackFixed));
        if (current.payload() != null && current.payload().sameReplayData(payload)) {
            updated.addAll(checkpoints.subList(cursor + 1, checkpoints.size()));
        }
        int barrier = Math.min(rollbackBarrierIndex, updated.size() - 1);
        return new SceneCheckpointLog(updated, cursor, barrier, rollbackFixed);
    }

    public SceneCheckpointLog rollbackOneCheckpoint() {
        if (!rollbackAllowed()) {
            throw new IllegalStateException("Scene checkpoint rollback is not allowed.");
        }
        return new SceneCheckpointLog(checkpoints, cursor - 1, rollbackBarrierIndex, rollbackFixed);
    }

    public SceneCheckpointLog blockRollback() {
        if (cursor < 0) {
            return this;
        }
        List<SceneCheckpoint> updated = new ArrayList<>(checkpoints);
        updated.set(cursor, updated.get(cursor).withRollbackBlocked(true));
        return new SceneCheckpointLog(updated, cursor, cursor, rollbackFixed);
    }

    public SceneCheckpointLog fixRollback() {
        List<SceneCheckpoint> updated = checkpoints.stream()
                .map(checkpoint -> checkpoint.withRollbackFixed(true))
                .toList();
        return new SceneCheckpointLog(updated, cursor, rollbackBarrierIndex, true);
    }

    private boolean isCheckpointBoundary(SceneExecutionResult result) {
        return result != null
                && result.step() != null
                && (result.status() == SceneExecutionStatus.DISPLAYING_TEXT
                || result.status() == SceneExecutionStatus.WAITING_FOR_CHOICE);
    }
}
