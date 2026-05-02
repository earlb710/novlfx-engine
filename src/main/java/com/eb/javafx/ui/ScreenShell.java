package com.eb.javafx.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Shared layout for reusable screen content with JavaFX frames.
 *
 * <p>The shell creates a root border pane with a title label and styled content
 * panel. It assumes the caller attaches the module stylesheet that defines the
 * {@code screen-root}, {@code screen-title}, and {@code screen-panel} classes.</p>
 */
public final class ScreenShell {
    private ScreenShell() {
    }

    /**
     * Builds a titled panel with consistent spacing and style classes.
     *
     * @param title screen title text shown above the content panel
     * @param content JavaFX node placed in the central panel
     */
    public static BorderPane titled(String title, Node content) {
        Label header = new Label(title);
        header.getStyleClass().add("screen-title");

        VBox body = new VBox(12, content);
        body.getStyleClass().add("screen-panel");
        body.setPadding(new Insets(16));

        BorderPane root = new BorderPane();
        root.getStyleClass().add("screen-root");
        root.setTop(header);
        root.setCenter(body);
        BorderPane.setMargin(header, new Insets(16, 16, 0, 16));
        BorderPane.setMargin(body, new Insets(16));
        return root;
    }
}
