package com.eb.javafx.transitions;

import com.eb.javafx.util.Validation;

/** Immutable named visual transition effect definition. */
public final class VisualTransitionDefinition {
    private final String id;
    private final SceneTransitionEffect effect;
    private final int durationMs;

    public VisualTransitionDefinition(String id, SceneTransitionEffect effect, int durationMs) {
        this.id = Validation.requireNonBlank(id, "Visual transition id is required.");
        this.effect = Validation.requireNonNull(effect, "Visual transition effect is required.");
        if (durationMs < 0) {
            throw new IllegalArgumentException("Visual transition durationMs must not be negative.");
        }
        this.durationMs = durationMs;
    }

    public String id() { return id; }
    public SceneTransitionEffect effect() { return effect; }
    public int durationMs() { return durationMs; }
}
