package com.eb.javafx.scene;

/**
 * Result status after a scene executor advances to the next UI-visible or terminal state.
 *
 * <p>Statuses distinguish text display pauses, choice waits, successful completion, and failures so UI layers
 * can decide whether to render content, prompt for input, or exit the flow.</p>
 */
public enum SceneExecutionStatus {
    DISPLAYING_TEXT,
    WAITING_FOR_CHOICE,
    COMPLETED,
    FAILED
}
