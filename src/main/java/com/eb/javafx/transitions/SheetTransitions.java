package com.eb.javafx.transitions;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Show-animation helper for modal sheet / popup overlays — a centralised fade-in + scale
 * choreography so the various popups in a UI feel like one family rather than each rolling its own.
 *
 * <p>The default {@link #fadeIn(Node) fade-in} runs over 180&nbsp;ms with an ease-out curve and a
 * subtle scale-up from {@code 0.95×} → {@code 1.0×}, so cards materialise into place rather than
 * snapping in. Hides are deliberately left to the caller (instantaneous removal) — when a popup is
 * dismissed it should be gone immediately, not linger through a fade-out.</p>
 *
 * <p>Threading: JavaFX application thread only. Stateless helper.</p>
 */
public final class SheetTransitions {

    /** Default fade-in duration — short enough to feel responsive, long enough to read as
     *  deliberate motion. */
    public static final Duration DEFAULT_FADE_DURATION = Duration.millis(180);

    /** Starting scale for the materialise-from-small effect. The fade-in interpolates from this
     *  scale to 1.0× over {@link #DEFAULT_FADE_DURATION}. */
    public static final double STARTING_SCALE = 0.95;

    private SheetTransitions() {
    }

    /** Fades + scales {@code node} into view with the default duration. Sets visible before
     *  animating so the first frame is at the starting (faded / scaled-down) state. */
    public static void fadeIn(Node node) {
        fadeIn(node, DEFAULT_FADE_DURATION);
    }

    /** Fades + scales {@code node} into view with a custom duration. */
    public static void fadeIn(Node node, Duration duration) {
        if (node == null) {
            return;
        }
        node.setOpacity(0.0);
        node.setScaleX(STARTING_SCALE);
        node.setScaleY(STARTING_SCALE);
        node.setVisible(true);
        // Disable caching during the scale animation — the cache hint would lock the node's pixels
        // at the starting (0.95×) raster and bilinear-filter every intermediate frame, but we want
        // JavaFX to re-rasterise text crisply on the final landing. Set explicitly to false so
        // callers that previously bumped this stay reset.
        node.setCache(false);

        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(duration, node);
        scale.setFromX(STARTING_SCALE);
        scale.setFromY(STARTING_SCALE);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition transition = new ParallelTransition(fade, scale);
        // Snap-to-pixels finisher — defensive cleanup that fixes the "post-animation text stays
        // blurry" issue. ScaleTransition lands on the toX/toY values but the transform property can
        // retain residual floating-point drift (e.g. 0.99999998 instead of 1.0); a node carrying a
        // non-identity scale transform stays in the raster-filtered render path, and inline TextFlow
        // children at sub-pixel positions inherit that softness. Two-step cleanup:
        //   1. Reset scaleX/Y to exactly 1.0 — removes lingering FP drift and clears the
        //      non-identity transform flag so subsequent layouts go through the crisp path.
        //   2. Force a fresh CSS application + layout pass — re-renders glyphs at final
        //      integer-pixel positions instead of reusing a sub-pixel snapshot from the scale ramp.
        transition.setOnFinished(event -> {
            node.setScaleX(1.0);
            node.setScaleY(1.0);
            node.applyCss();
            if (node instanceof javafx.scene.Parent parent) {
                parent.requestLayout();
            }
        });
        transition.play();
    }
}
