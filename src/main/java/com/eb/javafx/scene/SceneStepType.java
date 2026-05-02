package com.eb.javafx.scene;

/**
 * Typed scene command categories consumed by the headless scene executor.
 *
 * <p>The type determines which fields a step must provide and whether execution should pause for text, wait
 * for a choice, apply effects, or immediately follow a transition.</p>
 */
public enum SceneStepType {
    DIALOGUE,
    NARRATION,
    CHOICE,
    ACTION,
    TRANSITION
}
