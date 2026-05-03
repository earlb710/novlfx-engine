package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
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

    /**
     * Sets a screen region background image using the requested sizing mode.
     *
     * @param screen region that should receive the image background
     * @param image image to place behind the screen content
     * @param fit background sizing mode
     */
    public static void setBackgroundImage(Region screen, Image image, ScreenBackgroundFit fit) {
        Validation.requireNonNull(screen, "Screen background target is required.");
        screen.setBackground(new Background(backgroundImage(image, fit)));
    }

    /**
     * Creates a reusable JavaFX background image definition for screen roots.
     *
     * @param image image to place behind screen content
     * @param fit background sizing mode
     * @return configured background image
     */
    public static BackgroundImage backgroundImage(Image image, ScreenBackgroundFit fit) {
        Validation.requireNonNull(image, "Screen background image is required.");
        Validation.requireNonNull(fit, "Screen background fit is required.");
        return switch (fit) {
            case STRETCH -> new BackgroundImage(
                    image,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, false, false));
            case CROP_CENTER -> new BackgroundImage(
                    image,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(0, 0, false, false, false, true));
        };
    }
}
