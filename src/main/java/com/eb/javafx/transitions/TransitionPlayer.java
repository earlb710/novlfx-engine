package com.eb.javafx.transitions;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/** Plays JavaFX animations for named visual transition definitions. */
public final class TransitionPlayer {
    private TransitionPlayer() {}

    /**
     * Plays the animation described by {@code definition} on {@code node}, then calls {@code onComplete}.
     * If the effect is instant ({@code NONE}), calls {@code onComplete} immediately.
     */
    public static void play(VisualTransitionDefinition definition, Node node, Runnable onComplete) {
        SceneTransitionEffect effect = definition.effect();
        if (effect.isInstant()) {
            onComplete.run();
            return;
        }
        Duration duration = Duration.millis(definition.durationMs());
        javafx.animation.Transition anim = switch (effect) {
            case DISSOLVE -> dissolve(node, duration);
            case FADE_BLACK -> fadeBlack(node, duration);
            case WIPE_LEFT -> translateOut(node, duration, -node.getBoundsInLocal().getWidth());
            case WIPE_RIGHT -> translateOut(node, duration, node.getBoundsInLocal().getWidth());
            case MOVE_IN_LEFT -> translateIn(node, duration, -400);
            case MOVE_IN_RIGHT -> translateIn(node, duration, 400);
            default -> dissolve(node, duration);
        };
        anim.setOnFinished(e -> {
            node.setOpacity(1.0);
            node.setTranslateX(0);
            onComplete.run();
        });
        anim.play();
    }

    private static FadeTransition dissolve(Node node, Duration duration) {
        FadeTransition ft = new FadeTransition(duration, node);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        return ft;
    }

    private static SequentialTransition fadeBlack(Node node, Duration duration) {
        Duration half = duration.divide(2);
        FadeTransition out = new FadeTransition(half, node);
        out.setFromValue(1.0);
        out.setToValue(0.0);
        PauseTransition hold = new PauseTransition(Duration.millis(80));
        FadeTransition in = new FadeTransition(half, node);
        in.setFromValue(0.0);
        in.setToValue(1.0);
        return new SequentialTransition(out, hold, in);
    }

    private static TranslateTransition translateOut(Node node, Duration duration, double byX) {
        TranslateTransition tt = new TranslateTransition(duration, node);
        tt.setByX(byX);
        return tt;
    }

    private static TranslateTransition translateIn(Node node, Duration duration, double fromX) {
        node.setTranslateX(fromX);
        TranslateTransition tt = new TranslateTransition(duration, node);
        tt.setFromX(fromX);
        tt.setToX(0);
        return tt;
    }
}
