package com.eb.javafx.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * A game-agnostic "time passes" clock overlay: a small analog watch drawn on a dim modal backdrop, its
 * hour hand sweeping {@code fromAngle → target} (with the minute hand tracking it 12×) over a fixed
 * number of discrete ticks, then dissolving into the background. Non-interactive while it plays.
 *
 * <p>Pure JavaFX — all game policy is injected: the parent host {@link StackPane}, an optional skin for
 * the end-of-night <b>Sleep</b> button, and the {@code onFinished} callback that runs once the overlay
 * has fully dissolved (or immediately when there is no host to animate on). The host game decides what
 * the angles mean (a clock dial, a turn's time-of-day slots) and what happens when the sweep finishes.
 *
 * <p>Extracted from AltLife's {@code AltLifeClockOverlay} (see that game's
 * {@code docs/MAINAPPLAYOUT_SPLIT_PLAN.md} Phase 3). Threading: build/play on the JavaFX application
 * thread.</p>
 */
public final class ClockSweepOverlay {

    /** A normal full turn: 3 seconds over 60 ticks. */
    public static final int    TURN_STEPS   = 60;
    public static final double TURN_MILLIS  = 3000;
    /** An overnight sleep cycle: a longer 5 seconds over 100 ticks. */
    public static final int    SLEEP_STEPS  = 100;
    public static final double SLEEP_MILLIS = 5000;
    /** On finish the overlay dissolves: opacity 1 → 0 in {@value #FADE_STEPS} steps of
     *  {@value #FADE_STEP_MILLIS} ms each (half a second). */
    public static final int    FADE_STEPS       = 20;
    public static final double FADE_STEP_MILLIS = 25;

    private ClockSweepOverlay() {
    }

    /**
     * Plays the clock sweep on {@code parent}.
     *
     * @param parent          the host overlay stack the animation mounts on; when {@code null} nothing
     *                        is drawn and {@code onFinished} runs immediately (so callers never strand
     *                        work queued behind the clock)
     * @param caption         the action text shown under the clock (may be {@code null}/blank)
     * @param fromAngle       starting hour-hand angle (degrees clockwise from 12)
     * @param target          absolute forward end angle — may exceed 360 for an overnight revolution
     * @param steps           number of discrete ticks
     * @param totalMillis     total sweep duration in ms
     * @param sleeping        when {@code true} a white "Zzz" sits on the clock face
     * @param sleepButtonSkin optional skin applied to the end-of-night Sleep button (may be {@code null})
     * @param sleepAction     when non-{@code null} (the night's last turn) the overlay stays open on
     *                        finish and the caption is replaced by a Sleep button running this; otherwise
     *                        the overlay auto-dissolves
     * @param onSleepDissolve optional cleanup run just before the dissolve when {@code sleeping} and no
     *                        {@code sleepAction} (e.g. the host clearing popups so it wakes to a clean view)
     * @param onFinished      run once the overlay has fully dissolved (or immediately when {@code parent}
     *                        is {@code null})
     */
    public static void play(StackPane parent, String caption,
            double fromAngle, double target, int steps, double totalMillis,
            boolean sleeping, Consumer<Button> sleepButtonSkin,
            Runnable sleepAction, Runnable onSleepDissolve, Runnable onFinished) {
        if (parent == null) {
            if (onFinished != null) {
                onFinished.run();   // no host to animate on — don't strand anything queued behind the clock
            }
            return;
        }

        // ── Clock face ──
        double size = 120, c = size / 2;
        Pane clock = new Pane();
        clock.setPrefSize(size, size);
        clock.setMinSize(size, size);
        clock.setMaxSize(size, size);
        Circle face = new Circle(c, c, c - 6);
        face.setFill(Color.web("#11151c"));
        face.setStroke(Color.web("#e8e8e8"));
        face.setStrokeWidth(3);
        clock.getChildren().add(face);
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30);
            double r1 = c - 11, r2 = c - 7;
            Line tick = new Line(
                    c + r1 * Math.sin(a), c - r1 * Math.cos(a),
                    c + r2 * Math.sin(a), c - r2 * Math.cos(a));
            tick.setStroke(Color.web("#9aa3ad"));
            tick.setStrokeWidth(i % 3 == 0 ? 2.4 : 1.2);
            clock.getChildren().add(tick);
        }
        // Minute hand — tracks the hour hand realistically (12× its angle), so it sweeps a full turn
        // per hour the hour hand advances and lands on 12 whenever the hour hand sits on an exact hour.
        Line minute = new Line(c, c, c, 18);
        minute.setStroke(Color.web("#f2f2f2"));
        minute.setStrokeWidth(2.4);
        minute.setStrokeLineCap(StrokeLineCap.ROUND);
        Rotate minuteRotate =
                new Rotate((fromAngle * 12) % 360, c, c);
        minute.getTransforms().add(minuteRotate);
        // Hour hand — shorter, rotates to show the time of day.
        Line hour = new Line(c, c, c, 36);
        hour.setStroke(Color.web("#ffd54a"));
        hour.setStrokeWidth(3.4);
        hour.setStrokeLineCap(StrokeLineCap.ROUND);
        Rotate hourRotate = new Rotate(fromAngle, c, c);
        hour.getTransforms().add(hourRotate);
        Circle pin = new Circle(c, c, 4,
                Color.web("#ffd54a"));
        clock.getChildren().addAll(hour, minute, pin);
        // Sleeping: a white "Zzz" sits on the upper face, tilted, so the clock reads as "asleep".
        if (sleeping) {
            Text zzz = new Text("Zzz");
            zzz.setFill(Color.WHITE);
            zzz.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-font-style: italic;");
            zzz.setRotate(-15);
            // Centre the text on the upper third of the face.
            zzz.setLayoutX(c - 16);
            zzz.setLayoutY(c - 6);
            clock.getChildren().add(zzz);
        }

        Label captionLabel = new Label(caption == null ? "" : caption);
        captionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        captionLabel.setWrapText(true);
        captionLabel.setMaxWidth(220);
        captionLabel.setAlignment(Pos.CENTER);
        captionLabel.setTextAlignment(TextAlignment.CENTER);

        VBox card = new VBox(12, clock, captionLabel);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(18, 22, 18, 22));
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        card.setStyle("-fx-background-color: rgba(12,14,20,0.96); -fx-background-radius: 14;"
                + " -fx-border-color: rgba(255,255,255,0.5); -fx-border-width: 1.5; -fx-border-radius: 14;");

        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        overlay.setOnMouseClicked(Event::consume);   // non-interactive while it plays
        StackPane.setAlignment(card, Pos.CENTER);

        parent.getChildren().add(overlay);
        // Re-assert top-most after the current event cycle (a panel refresh may toFront its own popup).
        Platform.runLater(overlay::toFront);

        // {@code steps} discrete ticks over {@code totalMillis} (3s/60 for a turn, 5s/100 for a sleep).
        Timeline tl = new Timeline();
        double stepMillis = totalMillis / steps;
        for (int i = 1; i <= steps; i++) {
            final double angle = fromAngle + (target - fromAngle) * (i / (double) steps);
            tl.getKeyFrames().add(new KeyFrame(
                    Duration.millis(stepMillis * i), e -> {
                        hourRotate.setAngle(angle);
                        minuteRotate.setAngle((angle * 12) % 360);   // minute hand follows the hour hand
                    }));
        }
        tl.setOnFinished(e -> {
            if (sleepAction != null) {
                // Night's last turn: keep the popup open and swap the action text for a Sleep button.
                // The backdrop stays modal (clicks consumed), so it can't be dismissed any other way.
                Button sleepBtn = new Button("Sleep");
                if (sleepButtonSkin != null) {
                    sleepButtonSkin.accept(sleepBtn);
                }
                sleepBtn.setOnAction(ev -> {
                    parent.getChildren().remove(overlay);
                    sleepAction.run();   // roll the day + play the sleep-cycle animation
                });
                card.getChildren().set(card.getChildren().indexOf(captionLabel), sleepBtn);
                return;
            }
            // After sleeping, let the host clear the screen — a post-action refresh may have reopened a
            // popup behind the animation; clear it so the game wakes to a clean view.
            if (sleeping && onSleepDissolve != null) {
                onSleepDissolve.run();
            }
            // All animations (sleep and regular turns) dissolve the overlay instead of cutting it:
            // opacity 1 → 0 in FADE_STEPS discrete steps over half a second, then remove the overlay.
            Timeline fade = new Timeline();
            for (int s = 1; s <= FADE_STEPS; s++) {
                final double opacity = 1.0 - s / (double) FADE_STEPS;
                fade.getKeyFrames().add(new KeyFrame(
                        Duration.millis(FADE_STEP_MILLIS * s),
                        ev -> overlay.setOpacity(opacity)));
            }
            fade.setOnFinished(ev -> {
                parent.getChildren().remove(overlay);
                // Animation is fully gone now — run anything that was waiting for it (e.g. the
                // appointment-arrival prompt) so it appears on a clean screen.
                if (onFinished != null) {
                    onFinished.run();
                }
            });
            fade.play();
        });
        tl.play();
    }
}
