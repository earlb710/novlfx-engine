package com.eb.javafx.scene;

import javafx.animation.PauseTransition;

/**
 * Mutable per-screen-session state for skip/auto playback modes and seen-step tracking.
 * Created once per scene screen construction and shared through closures.
 */
public final class SceneSession {
    private final SeenStepTracker seenSteps = new SeenStepTracker();
    private ScenePlaybackMode mode = ScenePlaybackMode.NORMAL;
    private PauseTransition autoTimer;

    public SeenStepTracker seenSteps() { return seenSteps; }
    public ScenePlaybackMode mode() { return mode; }

    public void setMode(ScenePlaybackMode mode) {
        this.mode = mode;
        cancelAutoTimer();
    }

    public void scheduleAutoAdvance(int delayMs, Runnable advance) {
        cancelAutoTimer();
        autoTimer = new PauseTransition(javafx.util.Duration.millis(delayMs));
        autoTimer.setOnFinished(e -> advance.run());
        autoTimer.play();
    }

    public void cancelAutoTimer() {
        if (autoTimer != null) {
            autoTimer.stop();
            autoTimer = null;
        }
    }
}
