package com.lr2alt.javafx.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Shared layout for migrated screen placeholders, replacing repeated Ren'Py frames.
 */
public final class ScreenShell {
    private ScreenShell() {
    }

    /** Builds a titled panel with consistent spacing and style classes. */
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
