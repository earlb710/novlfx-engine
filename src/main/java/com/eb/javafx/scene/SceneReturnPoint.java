package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

/**
 * Call-stack return address for resumable nested scene flows.
 *
 * <p>Return points capture the caller scene id and the next step index after a call transition, allowing return
 * transitions and saved flow state to resume nested scenes correctly.</p>
 */
public final class SceneReturnPoint {
    private final String sceneId;
    private final int stepIndex;

    public SceneReturnPoint(String sceneId, int stepIndex) {
        this.sceneId = Validation.requireNonBlank(sceneId, "Return scene id is required.");
        if (stepIndex < 0) {
            throw new IllegalArgumentException("Return step index must not be negative.");
        }
        this.stepIndex = stepIndex;
    }

    public String sceneId() {
        return sceneId;
    }

    public int stepIndex() {
        return stepIndex;
    }
}
