package com.eb.javafx.scene;

import java.util.Set;

/** Immutable snapshot of which scene steps have been seen by the player. */
public final class SeenStepSnapshot {
    private final Set<String> seenKeys;

    SeenStepSnapshot(Set<String> seenKeys) {
        this.seenKeys = Set.copyOf(seenKeys);
    }

    Set<String> seenKeys() { return seenKeys; }
}
