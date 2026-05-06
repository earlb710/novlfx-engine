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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

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
    public static final String SCREEN_FOOTER_BAR_STYLE_CLASS = "screen-footer-bar";
    public static final String SCREEN_FOOTER_OPTION_STYLE_CLASS = "screen-footer-option";
    public static final String SCENE_STATUS_PANEL_STYLE_CLASS = "scene-status-panel";
    public static final String SCENE_DIALOGUE_PANEL_STYLE_CLASS = "scene-dialogue-panel";
    public static final String SCENE_CHOICES_PANEL_STYLE_CLASS = "scene-choices-panel";
    public static final String SCENE_EFFECTS_PANEL_STYLE_CLASS = "scene-effects-panel";
    public static final String LAYOUT_CONTENT_STYLE_CLASS = "layout-content";
    public static final String LAYOUT_SUBTITLE_STYLE_CLASS = "layout-subtitle";
    public static final String LAYOUT_FOOTER_STYLE_CLASS = "layout-footer";
    public static final String LAYOUT_TITLED_PANEL_STYLE_CLASS = "layout-titled-panel";
    public static final String LAYOUT_TWO_COLUMN_STYLE_CLASS = "layout-two-column";
    public static final String LAYOUT_COLUMN_STYLE_CLASS = "layout-column";
    public static final String LAYOUT_SIDEBAR_CONTENT_STYLE_CLASS = "layout-sidebar-content";
    public static final String LAYOUT_SIDEBAR_STYLE_CLASS = "layout-sidebar";
    public static final String LAYOUT_SIDEBAR_ENTRY_STYLE_CLASS = "layout-sidebar-entry";
    public static final String LAYOUT_MAIN_CONTENT_STYLE_CLASS = "layout-main-content";
    public static final String LAYOUT_HUD_OVERLAY_STYLE_CLASS = "layout-hud-overlay";
    public static final String LAYOUT_DIALOGUE_STYLE_CLASS = "layout-dialogue";
    public static final String LAYOUT_MENU_STYLE_CLASS = "layout-menu";
    public static final String LAYOUT_FORM_STYLE_CLASS = "layout-form";
    public static final String LAYOUT_PREVIEW_GRID_STYLE_CLASS = "layout-preview-grid";
    public static final String LAYOUT_CARD_STYLE_CLASS = "layout-card";
    public static final String LAYOUT_SECTION_STYLE_CLASS = "layout-section";
    public static final String LAYOUT_SECTION_TITLE_STYLE_CLASS = "layout-section-title";
    public static final String LAYOUT_SECTION_ROW_STYLE_CLASS = "layout-section-row";
    public static final String LAYOUT_ACTION_ROW_STYLE_CLASS = "layout-action-row";
    public static final String LAYOUT_PRIMARY_ACTION_STYLE_CLASS = "layout-primary-action";
    public static final String LAYOUT_SECONDARY_ACTION_STYLE_CLASS = "layout-secondary-action";
    public static final double BODY_SPACING = 12;
    public static final Insets OUTER_INSETS = new Insets(16);
    public static final Insets PANEL_INSETS = new Insets(16);
    private static final double FOOTER_SPACING = 8;
    private static final List<FooterOption> FOOTER_OPTIONS = List.of(
            new FooterOption("‹", "Back", "Backspace"),
            new FooterOption("◷", "History", "Ctrl+H"),
            new FooterOption("⇥", "Skip mode", "Tab"),
            new FooterOption("⇩", "Load", "Ctrl+L"),
            new FooterOption("▣", "Save", "Ctrl+S"),
            new FooterOption("⚡", "Quick save", "Ctrl+Q"),
            new FooterOption("⚙", "Preferences", "Ctrl+P"),
            new FooterOption("›", "Forward", "Space"));

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
        root.setBottom(footerBar());
        BorderPane.setMargin(header, new Insets(OUTER_INSETS.getTop(), OUTER_INSETS.getRight(), 0, OUTER_INSETS.getLeft()));
        BorderPane.setMargin(body, new Insets(OUTER_INSETS.getTop(), OUTER_INSETS.getRight(), 0, OUTER_INSETS.getLeft()));
        BorderPane.setMargin(root.getBottom(), OUTER_INSETS);
        return root;
    }

    public static HBox footerBar() {
        HBox footer = new HBox(FOOTER_SPACING);
        footer.getStyleClass().add(SCREEN_FOOTER_BAR_STYLE_CLASS);
        for (String optionText : footerOptionTexts()) {
            Label label = new Label(optionText);
            label.getStyleClass().add(SCREEN_FOOTER_OPTION_STYLE_CLASS);
            label.setAccessibleText(optionText);
            footer.getChildren().add(label);
        }
        return footer;
    }

    public static List<String> footerOptionTexts() {
        return FOOTER_OPTIONS.stream()
                .map(FooterOption::displayText)
                .toList();
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

    private record FooterOption(String icon, String label, String shortcut) {
        private String displayText() {
            return icon + " " + label + " (" + shortcut + ")";
        }
    }
}
