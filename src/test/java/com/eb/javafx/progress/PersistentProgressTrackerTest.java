package com.eb.javafx.progress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.prefs.Preferences;
import static org.junit.jupiter.api.Assertions.*;

final class PersistentProgressTrackerTest {

    private static final String TEST_NAMESPACE = "com/test/persistent/progress/trackertest";
    private PersistentProgressTracker tracker;

    @BeforeEach
    void setUp() throws Exception {
        Preferences.userRoot().node(TEST_NAMESPACE).removeNode();
        tracker = new PersistentProgressTracker(TEST_NAMESPACE);
    }

    @AfterEach
    void tearDown() throws Exception {
        Preferences.userRoot().node(TEST_NAMESPACE).removeNode();
    }

    @Test
    void flagFalseByDefault() {
        assertFalse(tracker.hasFlag("unlocked"));
    }

    @Test
    void setFlagTruePersistsAcrossInstances() {
        tracker.setFlag("unlocked", true);
        assertTrue(tracker.hasFlag("unlocked"));
        PersistentProgressTracker reloaded = new PersistentProgressTracker(TEST_NAMESPACE);
        assertTrue(reloaded.hasFlag("unlocked"));
    }

    @Test
    void setFlagFalseRemovesIt() {
        tracker.setFlag("unlocked", true);
        tracker.setFlag("unlocked", false);
        assertFalse(tracker.hasFlag("unlocked"));
    }

    @Test
    void counterStartsAtZero() {
        assertEquals(0, tracker.counter("score"));
    }

    @Test
    void incrementCounterAccumulates() {
        tracker.incrementCounter("score", 10);
        tracker.incrementCounter("score", 5);
        assertEquals(15, tracker.counter("score"));
    }

    @Test
    void milestoneStartsUnreached() {
        assertFalse(tracker.hasMilestone("chapter1"));
    }

    @Test
    void completeMilestonePersistsAcrossInstances() {
        tracker.completeMilestone("chapter1");
        assertTrue(tracker.hasMilestone("chapter1"));
        PersistentProgressTracker reloaded = new PersistentProgressTracker(TEST_NAMESPACE);
        assertTrue(reloaded.hasMilestone("chapter1"));
    }

    @Test
    void unlockStartsLocked() {
        assertFalse(tracker.isUnlocked("gallery_1"));
    }

    @Test
    void unlockPersistsAcrossInstances() {
        tracker.unlock("gallery_1");
        assertTrue(tracker.isUnlocked("gallery_1"));
        PersistentProgressTracker reloaded = new PersistentProgressTracker(TEST_NAMESPACE);
        assertTrue(reloaded.isUnlocked("gallery_1"));
    }

    @Test
    void snapshotRoundTripsViaCodec() {
        tracker.setFlag("f1", true);
        tracker.incrementCounter("c1", 3);
        tracker.completeMilestone("m1");
        tracker.unlock("u1");

        PersistentProgressSnapshotCodec codec = new PersistentProgressSnapshotCodec();
        PersistentProgressSnapshot snapshot = tracker.snapshot();
        String json = codec.toJson(snapshot);
        PersistentProgressSnapshot restored = codec.fromJson(json, "test");

        assertTrue(restored.flags().contains("f1"));
        assertEquals(3, restored.counters().get("c1"));
        assertTrue(restored.milestones().contains("m1"));
        assertTrue(restored.unlocks().contains("u1"));
    }

    @Test
    void restoreFromSnapshotOverwritesCurrentState() {
        tracker.setFlag("old-flag", true);
        PersistentProgressSnapshot snapshot = new PersistentProgressSnapshot(
                java.util.Set.of("new-flag"),
                java.util.Map.of("counter", 42),
                java.util.Set.of(),
                java.util.Set.of());
        tracker.restore(snapshot);

        assertFalse(tracker.hasFlag("old-flag"));
        assertTrue(tracker.hasFlag("new-flag"));
        assertEquals(42, tracker.counter("counter"));
    }
}
