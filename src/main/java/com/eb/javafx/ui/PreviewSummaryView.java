package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Simple reusable preview panel for display, scene, and snapshot summaries.
 */
public final class PreviewSummaryView {
    private PreviewSummaryView() {
    }

    public static VBox createPanel(String styleClass, String title, List<String> lines) {
        Validation.requireNonBlank(title, "Preview title is required.");
        Validation.requireNonNull(lines, "Preview lines are required.");
        VBox panel = ScreenShell.styledPanel(styleClass);
        panel.getChildren().add(new Label(title));
        for (String line : lines) {
            panel.getChildren().add(new Label(line));
        }
        return panel;
    }

    public static VBox createPanel(String styleClass, String title, Node content) {
        Validation.requireNonNull(content, "Preview content is required.");
        VBox panel = ScreenShell.styledPanel(styleClass);
        panel.getChildren().addAll(new Label(title), content);
        return panel;
    }
}
