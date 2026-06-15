package com.eb.javafx.ui;

import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * Reusable popup-panel chrome: a small red circular <b>✕</b> close pip pinned to the top-right
 * corner — the same look as the saved-games delete button on {@link SaveScreen}.
 *
 * <p>Host apps wrap their popup content with {@link #wrap(Node, Runnable)} so every popup gets a
 * consistent "close" affordance, or grab the bare button with {@link #closeButton(Runnable)} when
 * they want to place it themselves.</p>
 *
 * <p>The pip suppresses its own focus ring and consumes its mouse events, so a click on it never
 * bubbles to the panel/tile underneath.  Threading: build on the JavaFX application thread.</p>
 */
public final class PanelPopup {

    /** Resting / hover red — matches {@code SaveScreen}'s delete pip so the two read in family. */
    private static final String PIP_REST_BG  = "#c0392b";
    private static final String PIP_HOVER_BG = "#e74c3c";

    private PanelPopup() {
    }

    /**
     * A 20×20 red circular <b>✕</b> close pip wired to {@code onClose}.  Non-focusable, hover-lit,
     * and consumes its click so it doesn't trigger anything behind it.
     */
    public static Button closeButton(Runnable onClose) {
        Button btn = new Button("✕");
        btn.setFocusTraversable(false);
        String rest = pipStyle(PIP_REST_BG);
        String hover = pipStyle(PIP_HOVER_BG);
        btn.setStyle(rest);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(rest));
        btn.addEventFilter(MouseEvent.MOUSE_CLICKED, Event::consume);
        btn.setOnAction(e -> {
            e.consume();
            if (onClose != null) {
                onClose.run();
            }
        });
        Tooltip.install(btn, new Tooltip("Close"));
        return btn;
    }

    /**
     * Wraps {@code content} in a {@link StackPane} with the close pip overlaid in the top-right
     * corner.  The pip runs {@code onClose} (typically the host's "hide popup" call).
     */
    public static StackPane wrap(Node content, Runnable onClose) {
        StackPane stack = new StackPane();
        if (content != null) {
            stack.getChildren().add(content);
        }
        Button close = closeButton(onClose);
        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        StackPane.setMargin(close, new Insets(6, 6, 0, 0));
        stack.getChildren().add(close);
        return stack;
    }

    /** Inline style for the pip — same template for the rest / hover fills (only the colour swaps). */
    private static String pipStyle(String backgroundColor) {
        return "-fx-background-color: " + backgroundColor + ";"
                + " -fx-text-fill: white;"
                + " -fx-font-weight: bold;"
                + " -fx-font-size: 11px;"
                + " -fx-background-radius: 10;"
                + " -fx-border-color: transparent;"
                + " -fx-border-width: 0;"
                + " -fx-focus-color: transparent;"
                + " -fx-faint-focus-color: transparent;"
                + " -fx-background-insets: 0;"
                + " -fx-min-width: 20px;"
                + " -fx-min-height: 20px;"
                + " -fx-max-width: 20px;"
                + " -fx-max-height: 20px;"
                + " -fx-padding: 0 2 0 0;"
                + " -fx-cursor: hand;";
    }
}
