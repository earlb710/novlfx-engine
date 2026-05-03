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
    public static final String SCREEN_ROOT_STYLE_CLASS = "screen-root";
    public static final String SCREEN_TITLE_STYLE_CLASS = "screen-title";
    public static final String SCREEN_PANEL_STYLE_CLASS = "screen-panel";
    public static final String SCENE_STATUS_PANEL_STYLE_CLASS = "scene-status-panel";
    public static final String SCENE_DIALOGUE_PANEL_STYLE_CLASS = "scene-dialogue-panel";
    public static final String SCENE_CHOICES_PANEL_STYLE_CLASS = "scene-choices-panel";
    public static final String SCENE_EFFECTS_PANEL_STYLE_CLASS = "scene-effects-panel";
    public static final double BODY_SPACING = 12;
    public static final Insets OUTER_INSETS = new Insets(16);
    public static final Insets PANEL_INSETS = new Insets(16);

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
        header.getStyleClass().add(SCREEN_TITLE_STYLE_CLASS);

        VBox body = styledPanel(null, content);

        BorderPane root = new BorderPane();
        root.getStyleClass().add(SCREEN_ROOT_STYLE_CLASS);
        root.setTop(header);
        root.setCenter(body);
        BorderPane.setMargin(header, new Insets(OUTER_INSETS.getTop(), OUTER_INSETS.getRight(), 0, OUTER_INSETS.getLeft()));
        BorderPane.setMargin(body, OUTER_INSETS);
        return root;
    }

    public static VBox styledPanel(String styleClass, Node... children) {
        VBox body = new VBox(BODY_SPACING, children);
        body.getStyleClass().add(SCREEN_PANEL_STYLE_CLASS);
        if (styleClass != null && !styleClass.isBlank()) {
            body.getStyleClass().add(styleClass);
        }
        body.setPadding(PANEL_INSETS);
        return body;
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
