package com.eb.javafx.scene;

/** Result status after a scene executor advances to the next UI-visible or terminal state. */
public enum SceneExecutionStatus {
    DISPLAYING_TEXT,
    WAITING_FOR_CHOICE,
    COMPLETED,
    FAILED
}
