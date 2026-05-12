package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class RollbackBufferTest {

    @Test
    void contributorCapturesAndRestores() {
        int[] value = {42};
        RollbackContributor<Integer> contributor = new RollbackContributor<>() {
            @Override public Integer capture() { return value[0]; }
            @Override public void restore(Integer snapshot) { value[0] = snapshot; }
        };

        Integer snapshot = contributor.capture();
        value[0] = 99;
        contributor.restore(snapshot);
        assertEquals(42, value[0]);
    }

    @Test
    void bufferSnapshotsAndRestoresContributors() {
        int[] value = {10};
        RollbackContributor<Integer> contributor = new RollbackContributor<>() {
            @Override public Integer capture() { return value[0]; }
            @Override public void restore(Integer snapshot) { value[0] = snapshot; }
        };
        RollbackBuffer buffer = new RollbackBuffer(10);
        buffer.register("counter", contributor);

        SceneFlowState state = SceneFlowState.start("scene1");
        buffer.snapshot(state);
        value[0] = 99;
        buffer.snapshot(SceneFlowState.start("scene2"));

        assertTrue(buffer.canRollback());

        buffer.pop(); // discard current
        RollbackEntry previous = buffer.pop().orElseThrow();
        buffer.restore(previous);

        assertEquals(10, value[0]);
        assertEquals("scene1", previous.flowState().activeSceneId());
    }

    @Test
    void bufferCapacityEvidesOldestEntries() {
        RollbackBuffer buffer = new RollbackBuffer(3);
        for (int i = 0; i < 5; i++) {
            buffer.snapshot(SceneFlowState.start("scene" + i));
        }
        assertEquals(3, buffer.size());
    }

    @Test
    void canRollbackRequiresTwoEntries() {
        RollbackBuffer buffer = new RollbackBuffer(10);
        assertFalse(buffer.canRollback());
        buffer.snapshot(SceneFlowState.start("s1"));
        assertFalse(buffer.canRollback());
        buffer.snapshot(SceneFlowState.start("s2"));
        assertTrue(buffer.canRollback());
    }

    @Test
    void rollbackSnapshotCodecRoundTripsFlowStates() {
        RollbackBuffer buffer = new RollbackBuffer(10);
        buffer.snapshot(SceneFlowState.start("scene1"));
        buffer.snapshot(SceneFlowState.start("scene2"));

        RollbackSnapshotCodec codec = new RollbackSnapshotCodec();
        assertEquals("rollback", codec.sectionId());
        assertEquals(1, codec.schemaVersion());

        String json = codec.toJson(buffer);
        RollbackBuffer restored = codec.fromJson(json, "test");

        assertEquals(2, restored.size());
        restored.pop(); // scene2
        RollbackEntry scene1 = restored.pop().orElseThrow();
        assertEquals("scene1", scene1.flowState().activeSceneId());
    }
}
