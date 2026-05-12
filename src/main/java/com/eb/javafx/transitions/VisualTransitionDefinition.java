package com.eb.javafx.transitions;

import com.eb.javafx.util.Validation;

/** Immutable named visual transition effect definition. */
public final class VisualTransitionDefinition {
    private final String id;
    private final SceneTransitionEffect effect;
    private final int durationMs;
    private final String soundRef;

    public VisualTransitionDefinition(String id, SceneTransitionEffect effect, int durationMs) {
        this.id = Validation.requireNonBlank(id, "Visual transition id is required.");
        this.effect = Validation.requireNonNull(effect, "Visual transition effect is required.");
        if (durationMs < 0) {
            throw new IllegalArgumentException("Visual transition durationMs must not be negative.");
        }
        this.durationMs = durationMs;
        this.soundRef = null;
    }

    private VisualTransitionDefinition(String id, SceneTransitionEffect effect, int durationMs, String soundRef) {
        this.id = id;
        this.effect = effect;
        this.durationMs = durationMs;
        this.soundRef = soundRef;
    }

    public String id() { return id; }
    public SceneTransitionEffect effect() { return effect; }
    public int durationMs() { return durationMs; }

    /** Returns the audio asset ref to play during this transition, or null if none. */
    public String soundRef() { return soundRef; }

    /** Returns a copy of this definition with the given audio asset ref attached. */
    public VisualTransitionDefinition withSoundRef(String soundRef) {
        return new VisualTransitionDefinition(id, effect, durationMs,
                Validation.requireNonBlank(soundRef, "Transition sound ref is required."));
    }
}
