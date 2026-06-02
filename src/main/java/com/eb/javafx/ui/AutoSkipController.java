package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.scene.SceneSession;
import com.eb.javafx.scene.ScenePlaybackMode;
import javafx.util.Duration;

import java.util.function.BooleanSupplier;

/**
 * Engine-side wiring for the standard scene-flow auto-skip + animated scroll-to-bottom
 * combo.  Hosts that want Renpy-style auto-advance behaviour for a gameplay screen
 * create one of these next to their {@link SceneSession} and call {@link #refresh}
 * on every render — the controller reads the current text-speed preference, applies
 * it to both the auto-advance tick delay AND the dialog block's scroll-to-bottom
 * animation duration, and (when {@link ScenePlaybackMode#AUTO} is active) re-arms
 * the pump with the host-supplied advance callback.
 *
 * <h2>What the host owns vs. what the controller owns</h2>
 *
 * <ul>
 *   <li><b>Host (game) owns</b> the per-tick advance logic — typically "call
 *       {@link com.eb.javafx.scene.SceneExecutor#continueFromText} on the current
 *       state, push the result into the host's step history, and trigger a re-render."
 *       That's necessarily game-specific because step history, render scheduling, and
 *       any per-step side effects (achievements, persistence, transitions) live in
 *       the host.</li>
 *   <li><b>Controller (engine) owns</b> the cadence — delay between ticks, scroll
 *       animation duration, pump lifecycle, mode-flip cancellation.  All of that is
 *       generic and identical across hosts, so concentrating it here avoids each
 *       host having to recreate the wiring (mistimed animation vs. delay, forgetting
 *       to cancel the pump on mode change, etc.).</li>
 * </ul>
 *
 * <h2>Cadence</h2>
 *
 * <p>The auto-skip delay equals {@code prefs.textSpeed().durationMillis()} — the
 * "Slow / Normal / Fast" setting players already configure in the Preferences screen.
 * The scroll animation runs for {@link #DEFAULT_SCROLL_DURATION_FRACTION} of that
 * delay (default 50 %, clamped to at least {@link #MIN_SCROLL_DURATION_MS} ms so the
 * animation stays visible at FAST speed).  The pump tick interval is then
 * {@code scrollMs + 2 × (textSpeed − scrollMs)} — i.e. the animation duration plus
 * twice the natural reading pause — so the reader gets twice as long to absorb each
 * line after the scroll finishes before the next one drops in (NORMAL at 400 ms
 * resolves to 200 ms animation + 400 ms reading pause = 600 ms per line).</p>
 *
 * <h2>Threading</h2>
 *
 * <p>Must be used from the JavaFX application thread — same constraint as
 * {@link SceneSession} and {@link DialogEntriesView}.</p>
 */
public final class AutoSkipController {

    /** Fraction of {@code textSpeed.durationMillis()} used as the scroll animation
     *  duration.  Default 0.5 — see the class Javadoc for the rationale. */
    public static final double DEFAULT_SCROLL_DURATION_FRACTION = 0.5;

    /** Minimum scroll animation duration in milliseconds.  Stops the animation from
     *  collapsing to invisibility at the FAST text-speed setting (200 ms × 0.5 = 100 ms;
     *  still visible, but anything below ~50 ms tends to look like an instant snap). */
    public static final int MIN_SCROLL_DURATION_MS = 50;

    /** Default read-pause multiplier — the gap after the scroll animation is
     *  {@code (textSpeed − scrollMs) × this}. */
    public static final double DEFAULT_READ_PAUSE_MULTIPLIER = 2.0;

    // Config-driven tuning (set at boot from the `autoAdvance` config object); fall back to the
    // defaults above when unset.  Lets a game / mod tune auto-mode cadence without code.
    private static volatile double configuredScrollFraction = DEFAULT_SCROLL_DURATION_FRACTION;
    private static volatile int configuredMinScrollMs = MIN_SCROLL_DURATION_MS;
    private static volatile double configuredReadPauseMultiplier = DEFAULT_READ_PAUSE_MULTIPLIER;

    /** Installs auto-advance cadence tuning from config; any null arg keeps the current value.
     *  {@code scrollFraction} is clamped to (0, 1]; the others to sane minimums. */
    public static void setAutoAdvanceTuning(Double scrollFraction, Integer minScrollMs,
                                            Double readPauseMultiplier) {
        if (scrollFraction != null && scrollFraction > 0.0 && scrollFraction <= 1.0) {
            configuredScrollFraction = scrollFraction;
        }
        if (minScrollMs != null && minScrollMs >= 0) {
            configuredMinScrollMs = minScrollMs;
        }
        if (readPauseMultiplier != null && readPauseMultiplier >= 0.0) {
            configuredReadPauseMultiplier = readPauseMultiplier;
        }
    }

    private final SceneSession session;
    private final DialogEntriesView dialog;
    private final PreferencesService prefs;
    private final double scrollDurationFraction;

    /** Creates a controller with the {@linkplain #DEFAULT_SCROLL_DURATION_FRACTION
     *  default scroll-duration fraction} ({@value #DEFAULT_SCROLL_DURATION_FRACTION}).
     *
     *  @param session live {@link SceneSession} for the gameplay screen — owns the
     *                 auto-timer that the pump schedules on.
     *  @param dialog  dialog widget whose scroll-to-bottom animation should track the
     *                 text-speed preference.  May be null when the host has no dialog
     *                 block on screen (controller then only manages the auto-advance
     *                 pump, no animation wiring).
     *  @param prefs   preferences service to read {@code textSpeed} from. */
    public AutoSkipController(SceneSession session, DialogEntriesView dialog, PreferencesService prefs) {
        this(session, dialog, prefs, configuredScrollFraction);
    }

    /** Creates a controller with a custom scroll-duration fraction.  Useful for
     *  hosts that want the animation to be faster or slower than half the auto-skip
     *  delay (e.g. a "snappy" feel with 0.25, or a more deliberate slide with 0.75).
     *
     *  @param scrollDurationFraction must be {@code > 0} and {@code <= 1}. */
    public AutoSkipController(SceneSession session, DialogEntriesView dialog, PreferencesService prefs,
                              double scrollDurationFraction) {
        if (session == null) {
            throw new IllegalArgumentException("SceneSession is required.");
        }
        if (prefs == null) {
            throw new IllegalArgumentException("PreferencesService is required.");
        }
        if (scrollDurationFraction <= 0.0 || scrollDurationFraction > 1.0) {
            throw new IllegalArgumentException(
                    "scrollDurationFraction must be in (0, 1]; was " + scrollDurationFraction);
        }
        this.session = session;
        this.dialog = dialog;
        this.prefs = prefs;
        this.scrollDurationFraction = scrollDurationFraction;
    }

    /**
     * Refreshes the auto-skip + animation wiring against the current text-speed
     * preference.  Hosts call this on every render so the controller stays in sync
     * with any preference changes the player makes mid-scene.
     *
     * <p>What this does:</p>
     * <ol>
     *   <li>Reads {@code prefs.textSpeed().durationMillis()} — the player's chosen
     *       delay between auto-advance ticks.</li>
     *   <li>Sets the dialog's scroll-to-bottom animation duration to
     *       {@code max(MIN_SCROLL_DURATION_MS, delay * scrollDurationFraction)} so
     *       the visible scroll animation finishes before the next tick fires.</li>
     *   <li>If {@link SceneSession#mode()} is {@link ScenePlaybackMode#AUTO}, re-arms
     *       the auto-advance pump via {@link SceneSession#startAutoAdvance} with the
     *       supplied {@code advance} callback.  Idempotent in steady state — calling
     *       it on consecutive renders just cancels and restarts the same timer.</li>
     * </ol>
     *
     * <p>When the mode is not AUTO, the pump is left alone — calling this does
     * NOT auto-start the pump.  Hosts toggle AUTO on via their own input handler
     * (typically a footer button) followed by a render, and the next refresh call
     * sees AUTO and starts the pump.</p>
     *
     * @param advance per-tick advance callback.  Returns true to continue the loop,
     *                false to pause it (e.g. waiting on a choice / hotspot / scene
     *                end).  See {@link SceneSession#startAutoAdvance} for the full
     *                contract.  Must not be null.
     */
    public void refresh(BooleanSupplier advance) {
        if (advance == null) {
            throw new IllegalArgumentException("Advance callback is required.");
        }
        int delayMs = prefs.textSpeedMillis();
        double scrollMs = Math.max(configuredMinScrollMs, delayMs * scrollDurationFraction);
        if (dialog != null) {
            dialog.setScrollAnimationDuration(Duration.millis(scrollMs));
        }
        if (session.mode() == ScenePlaybackMode.AUTO) {
            // Pump cadence: scroll animation + 2× the natural reading pause.  The "natural"
            // pause is the original (textSpeed − scrollMs) gap between the end of the
            // animation and the next tick; doubling it gives the reader twice as long to
            // absorb each line after the scroll finishes before the next one drops in.
            //
            // For NORMAL (textSpeed = 400 ms, scrollMs = 200 ms):
            //   old tick interval = 400 ms  (200 ms anim + 200 ms pause)
            //   new tick interval = 600 ms  (200 ms anim + 400 ms pause)
            //
            // The scroll animation duration itself is unchanged so the drop-in cue keeps
            // tracking the text-speed preference — only the read-time pause stretches.
            double readPauseMs = Math.max(0.0, delayMs - scrollMs) * configuredReadPauseMultiplier;
            int tickIntervalMs = (int) Math.round(scrollMs + readPauseMs);
            session.startAutoAdvance(tickIntervalMs, advance);
        }
    }

    /** Cancels any in-flight auto-advance tick.  Equivalent to
     *  {@link SceneSession#cancelAutoTimer()} but exposed here so hosts that only
     *  interact with the controller don't need a separate session reference. */
    public void cancel() {
        session.cancelAutoTimer();
    }

    /** Returns the dialog this controller animates, or null when none was supplied. */
    public DialogEntriesView dialog() {
        return dialog;
    }

    /** Returns the session this controller drives. */
    public SceneSession session() {
        return session;
    }
}
