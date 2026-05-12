package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashSet;
import java.util.Set;

/** Mutable tracker of which scene steps have been seen; used by skip mode to fast-forward seen text. */
public final class SeenStepTracker {
    private final Set<String> seenKeys = new LinkedHashSet<>();

    public void markSeen(String sceneId, String stepId) {
        Validation.requireNonBlank(sceneId, "sceneId");
        Validation.requireNonBlank(stepId, "stepId");
        seenKeys.add(sceneId + ":" + stepId);
    }

    public boolean hasSeen(String sceneId, String stepId) {
        return seenKeys.contains(sceneId + ":" + stepId);
    }

    public SeenStepSnapshot snapshot() {
        return new SeenStepSnapshot(seenKeys);
    }

    public void restore(SeenStepSnapshot snapshot) {
        Validation.requireNonNull(snapshot, "snapshot");
        seenKeys.clear();
        seenKeys.addAll(snapshot.seenKeys());
    }
}
