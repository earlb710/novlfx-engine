package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SeenStepSnapshotCodecTest {
    private final SeenStepSnapshotCodec codec = new SeenStepSnapshotCodec();

    @Test
    void sectionIdAndVersionAreStable() {
        assertEquals("seenSteps", codec.sectionId());
        assertEquals(1, codec.schemaVersion());
    }

    @Test
    void roundTripPreservesAllSeenKeys() {
        SeenStepTracker tracker = new SeenStepTracker();
        tracker.markSeen("scene1", "step1");
        tracker.markSeen("scene2", "step3");
        SeenStepSnapshot original = tracker.snapshot();

        String json = codec.toJson(original);
        SeenStepSnapshot restored = codec.fromJson(json, "test");

        SeenStepTracker restoredTracker = new SeenStepTracker();
        restoredTracker.restore(restored);
        assertTrue(restoredTracker.hasSeen("scene1", "step1"));
        assertTrue(restoredTracker.hasSeen("scene2", "step3"));
        assertFalse(restoredTracker.hasSeen("scene1", "step3"));
    }

    @Test
    void roundTripPreservesEmptySnapshot() {
        SeenStepSnapshot empty = new SeenStepTracker().snapshot();

        String json = codec.toJson(empty);
        SeenStepSnapshot restored = codec.fromJson(json, "test");

        SeenStepTracker restoredTracker = new SeenStepTracker();
        restoredTracker.restore(restored);
        assertFalse(restoredTracker.hasSeen("any", "step"));
    }

    @Test
    void fromJsonRejectsNonObjectRoot() {
        assertThrows(IllegalArgumentException.class, () -> codec.fromJson("[]", "test"));
        assertThrows(IllegalArgumentException.class, () -> codec.fromJson("\"string\"", "test"));
    }

    @Test
    void fromJsonRejectsNonArraySeenKeys() {
        assertThrows(IllegalArgumentException.class,
                () -> codec.fromJson("{\"seenKeys\": \"notAnArray\"}", "test"));
    }
}
