package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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
    private static final Insets OUTER_INSETS_WITHOUT_BOTTOM = new Insets(
            OUTER_INSETS.getTop(),
            OUTER_INSETS.getRight(),
            0,
            OUTER_INSETS.getLeft());
    private static final double FOOTER_SPACING = 8;
    private static final List<FooterOption> FOOTER_OPTIONS = List.of(
            new FooterOption("back", "‹", "Back", "Backspace", "Return to the previous screen."),
            new FooterOption("history", "◷", "History", "Ctrl+H", "Open conversation history."),
            new FooterOption("skip-mode", "⇥", "Skip mode", "Tab", "Toggle skip mode."),
            new FooterOption("load", "⇩", "Load", "Ctrl+L", "Open the load screen."),
            new FooterOption("save", "▣", "Save", "Ctrl+S", "Open the save screen."),
            new FooterOption("quick-save", "⚡", "Quick save", "Ctrl+Q", "Save to the quick-save slot."),
            new FooterOption("preferences", "⚙", "Preferences", "Ctrl+P", "Open preferences."),
            new FooterOption("forward", "›", "Forward", "Space", "Advance the scene."));

    private ScreenShell() {
    }

    /**
     * Builds a titled panel with consistent spacing and style classes.
     *
     * @param title screen title text shown above the content panel
     * @param content JavaFX node placed in the central panel
     */
    public static BorderPane titled(String title, Node content) {
        return titled(title, content, FOOTER_OPTIONS);
    }

    /**
     * Builds a titled panel with caller-supplied footer options.
     *
     * @param title screen title text shown above the content panel
     * @param content JavaFX node placed in the central panel
     * @param footerOptions footer options to show, or an empty list to omit the footer
     */
    public static BorderPane titled(String title, Node content, List<FooterOption> footerOptions) {
        Label header = new Label(title);
        header.getStyleClass().add(SCREEN_TITLE_STYLE_CLASS);

        VBox body = styledPanel(null, content);

        BorderPane root = new BorderPane();
        root.getStyleClass().add(SCREEN_ROOT_STYLE_CLASS);
        root.setTop(header);
        root.setCenter(body);
        if (footerOptions != null && !footerOptions.isEmpty()) {
            root.setBottom(footerBar(footerOptions));
            BorderPane.setMargin(root.getBottom(), OUTER_INSETS);
        }
        BorderPane.setMargin(header, OUTER_INSETS_WITHOUT_BOTTOM);
        BorderPane.setMargin(body, OUTER_INSETS_WITHOUT_BOTTOM);
        return root;
    }

    /**
     * Creates a footer bar node for one screen shell.
     *
     * <p>A fresh JavaFX node is returned for every shell because JavaFX nodes cannot be shared
     * across multiple parents.</p>
     */
    public static HBox footerBar() {
        return footerBar(FOOTER_OPTIONS);
    }

    /**
     * Creates a footer bar node from caller-supplied footer options.
     *
     * @param footerOptions options to render left-to-right in the footer
     */
    public static HBox footerBar(List<FooterOption> footerOptions) {
        Validation.requireNonNull(footerOptions, "Footer options are required.");
        HBox footer = new HBox(FOOTER_SPACING);
        footer.getStyleClass().add(SCREEN_FOOTER_BAR_STYLE_CLASS);
        for (FooterOption option : footerOptions) {
            Validation.requireNonNull(option, "Footer option is required.");
            Label label = new Label(option.displayText());
            label.getStyleClass().add(SCREEN_FOOTER_OPTION_STYLE_CLASS);
            label.setAccessibleText(option.accessibleText());
            if (option.tooltip() != null && !option.tooltip().isBlank()) {
                label.setTooltip(new Tooltip(option.tooltip()));
            }
            footer.getChildren().add(label);
        }
        return footer;
    }

    /** Shows or hides the footer node while keeping layout management in sync. */
    public static void setFooterVisible(Node footer, boolean visible) {
        Validation.requireNonNull(footer, "Footer node is required.");
        footer.setVisible(visible);
        footer.setManaged(visible);
    }

    /** Shows or hides the footer attached to the supplied screen shell, when present. */
    public static void setFooterVisible(BorderPane screen, boolean visible) {
        Validation.requireNonNull(screen, "Screen shell is required.");
        if (screen.getBottom() != null) {
            setFooterVisible(screen.getBottom(), visible);
        }
    }

    /** Sets footer transparency where {@code 0.0} is fully opaque and {@code 1.0} is invisible. */
    public static void setFooterTransparency(Node footer, double transparency) {
        Validation.requireNonNull(footer, "Footer node is required.");
        footer.setOpacity(1.0 - Validation.requireUnitInterval(
                transparency,
                "Footer transparency must be between 0.0 and 1.0."));
    }

    /** Sets transparency on the footer attached to the supplied screen shell, when present. */
    public static void setFooterTransparency(BorderPane screen, double transparency) {
        Validation.requireNonNull(screen, "Screen shell is required.");
        if (screen.getBottom() != null) {
            setFooterTransparency(screen.getBottom(), transparency);
        }
    }

    public static List<FooterOption> defaultFooterOptions() {
        return FOOTER_OPTIONS;
    }

    public static List<FooterOption> changeFooterIcon(List<FooterOption> options, String id, String icon) {
        return replaceFooterOption(options, id, option -> option.withIcon(icon));
    }

    public static List<FooterOption> changeFooterLabel(List<FooterOption> options, String id, String label) {
        return replaceFooterOption(options, id, option -> option.withLabel(label));
    }

    public static List<FooterOption> changeFooterShortcut(List<FooterOption> options, String id, String shortcut) {
        return replaceFooterOption(options, id, option -> option.withShortcut(shortcut));
    }

    public static List<FooterOption> changeFooterTooltip(List<FooterOption> options, String id, String tooltip) {
        return replaceFooterOption(options, id, option -> option.withTooltip(tooltip));
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

    private static List<FooterOption> replaceFooterOption(
            List<FooterOption> options,
            String id,
            java.util.function.UnaryOperator<FooterOption> replacement) {
        Validation.requireNonNull(options, "Footer options are required.");
        String normalizedId = Validation.requireNonBlank(id, "Footer option id is required.");
        Validation.requireNonNull(replacement, "Footer option replacement is required.");
        return options.stream()
                .map(option -> {
                    Validation.requireNonNull(option, "Footer option is required.");
                    return option.id().equals(normalizedId) ? replacement.apply(option) : option;
                })
                .toList();
    }

    public record FooterOption(String id, String icon, String label, String shortcut, String tooltip) {
        public FooterOption {
            id = Validation.requireNonBlank(id, "Footer option id is required.");
            icon = Validation.requireNonBlank(icon, "Footer option icon is required.");
            label = Validation.requireNonBlank(label, "Footer option label is required.");
            shortcut = Validation.requireNonBlank(shortcut, "Footer option shortcut is required.");
            tooltip = tooltip == null ? "" : tooltip;
        }

        public String displayText() {
            return icon + " " + label + " (" + shortcut + ")";
        }

        public String accessibleText() {
            return label + " - Keyboard shortcut: " + shortcut;
        }

        public FooterOption withIcon(String icon) {
            return new FooterOption(id, icon, label, shortcut, tooltip);
        }

        public FooterOption withLabel(String label) {
            return new FooterOption(id, icon, label, shortcut, tooltip);
        }

        public FooterOption withShortcut(String shortcut) {
            return new FooterOption(id, icon, label, shortcut, tooltip);
        }

        public FooterOption withTooltip(String tooltip) {
            return new FooterOption(id, icon, label, shortcut, tooltip);
        }
    }
}
