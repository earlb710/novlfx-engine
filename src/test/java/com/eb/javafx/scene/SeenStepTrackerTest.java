package com.eb.javafx.scene;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SeenStepTrackerTest {

    @Test
    void markAndCheckSeen() {
        SeenStepTracker tracker = new SeenStepTracker();
        assertFalse(tracker.hasSeen("scene1", "step1"));
        tracker.markSeen("scene1", "step1");
        assertTrue(tracker.hasSeen("scene1", "step1"));
    }

    @Test
    void differentStepsAreIndependent() {
        SeenStepTracker tracker = new SeenStepTracker();
        tracker.markSeen("scene1", "step1");
        assertFalse(tracker.hasSeen("scene1", "step2"));
        assertFalse(tracker.hasSeen("scene2", "step1"));
    }

    @Test
    void snapshotRoundTrip() {
        SeenStepTracker tracker = new SeenStepTracker();
        tracker.markSeen("scene1", "step1");
        tracker.markSeen("scene2", "step3");

        SeenStepSnapshot snapshot = tracker.snapshot();
        SeenStepTracker restored = new SeenStepTracker();
        restored.restore(snapshot);

        assertTrue(restored.hasSeen("scene1", "step1"));
        assertTrue(restored.hasSeen("scene2", "step3"));
        assertFalse(restored.hasSeen("scene1", "step3"));
    }
}
