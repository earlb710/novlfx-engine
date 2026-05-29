package com.eb.javafx.scene;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.function.BooleanSupplier;

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

    /**
     * Schedules a single auto-advance tick.  After {@code delayMs} milliseconds, the
     * supplied {@code advance} Runnable is invoked once.  Use {@link #startAutoAdvance}
     * when you want a self-rescheduling loop driven by an advance result.
     */
    public void scheduleAutoAdvance(int delayMs, Runnable advance) {
        cancelAutoTimer();
        autoTimer = new PauseTransition(Duration.millis(delayMs));
        autoTimer.setOnFinished(e -> advance.run());
        autoTimer.play();
    }

    /**
     * Starts an auto-advance loop that runs while {@link #mode()} is
     * {@link ScenePlaybackMode#AUTO} and the supplied {@code advance} BooleanSupplier
     * keeps returning {@code true}.
     *
     * <p>Each tick:
     * <ol>
     *   <li>The previous timer (if any) is cancelled.</li>
     *   <li>A new {@link PauseTransition} fires after {@code delayMs}.</li>
     *   <li>When it fires, {@code advance.getAsBoolean()} is invoked on the JavaFX
     *       application thread.  If it returns {@code true} and the mode is still
     *       {@code AUTO}, the loop reschedules another tick after the same delay.</li>
     *   <li>A {@code false} return value (advance impossible — waiting on a choice,
     *       hotspot, end of content, …) stops the loop naturally without further
     *       reschedule.  Exceptions are caught, logged, and stop the loop as well so
     *       a single bad tick doesn't crash the JavaFX animation timer.</li>
     * </ol>
     *
     * <p>Calling this method again while a loop is in flight cancels the in-flight
     * timer and starts a fresh loop with the new delay/advance — hosts can safely call
     * it from every render or input handler to keep the pump alive.  Calling
     * {@link #setMode(ScenePlaybackMode)} with a non-AUTO value also stops the loop
     * (via {@link #cancelAutoTimer}), and calling it with AUTO doesn't auto-start the
     * pump — the host must call {@code startAutoAdvance} after the mode flip.</p>
     *
     * <p>Threading: must be called from the JavaFX application thread; the {@code advance}
     * callback also runs on that thread.</p>
     *
     * @param delayMs  delay between ticks in milliseconds; must be positive.  A host
     *                 reading {@code PreferencesService.textSpeed().durationMillis()}
     *                 typically passes values in the 200–800 ms range.
     * @param advance  callback that performs one tick and reports whether an advance
     *                 happened.  Must not be null.
     */
    public void startAutoAdvance(int delayMs, BooleanSupplier advance) {
        if (advance == null) {
            return;
        }
        cancelAutoTimer();
        if (mode != ScenePlaybackMode.AUTO) {
            return;
        }
        scheduleAutoTick(delayMs, advance);
    }

    private void scheduleAutoTick(int delayMs, BooleanSupplier advance) {
        PauseTransition tick = new PauseTransition(Duration.millis(Math.max(1, delayMs)));
        autoTimer = tick;
        tick.setOnFinished(e -> {
            // Re-check mode at fire time — the player may have toggled auto-skip off,
            // or a manual action may have called cancelAutoTimer().  In both cases the
            // current tick should bail out instead of advancing.
            if (mode != ScenePlaybackMode.AUTO || autoTimer != tick) {
                return;
            }
            boolean advanced;
            try {
                advanced = advance.getAsBoolean();
            } catch (RuntimeException ex) {
                System.err.println("[SceneSession] startAutoAdvance: advance callback threw — auto-skip halted: " + ex);
                ex.printStackTrace();
                return;
            }
            // Re-schedule only when we actually advanced AND we're still in AUTO.  No
            // advance (false) lets the pump pause naturally — the host can restart it
            // by calling startAutoAdvance again from a render / state-change handler.
            if (advanced && mode == ScenePlaybackMode.AUTO && autoTimer == tick) {
                scheduleAutoTick(delayMs, advance);
            }
        });
        tick.play();
    }

    public void cancelAutoTimer() {
        if (autoTimer != null) {
            autoTimer.stop();
            autoTimer = null;
        }
    }
}
