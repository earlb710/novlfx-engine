package com.eb.javafx.ui;

import com.eb.javafx.localization.LocalizationService;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.prefs.PreferencesService.FooterShortcutDisplay;
import com.eb.javafx.state.GameState;
import com.eb.javafx.util.Validation;
import com.eb.javafx.util.VectorImage;
import javafx.application.Platform;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.util.Duration;

import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    public static final String SCREEN_BACKGROUND_SVG_STYLE_CLASS = "screen-background-svg";
    public static final String SCREEN_FOOTER_BAR_STYLE_CLASS = "screen-footer-bar";
    public static final String SCREEN_FOOTER_OPTION_STYLE_CLASS = "screen-footer-option";
    public static final String SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS = "screen-footer-option-disabled";
    public static final String SCREEN_FOOTER_COMPACT_STYLE_CLASS = "screen-footer-compact";
    public static final String DEFAULT_FOOTER_ICON_RESOURCE_DIRECTORY = "com/eb/javafx/images/icons";
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
    static final Insets FOOTER_INSETS = new Insets(1);
    private static final Insets OUTER_INSETS_WITHOUT_BOTTOM = new Insets(
            OUTER_INSETS.getTop(),
            OUTER_INSETS.getRight(),
            0,
            OUTER_INSETS.getLeft());
    private static final double FOOTER_SPACING = 14;
    private static final double COMPACT_FOOTER_SPACING = 6;
    private static final int FOOTER_ICON_SIZE = 14;
    private static final double DEFAULT_FOOTER_OPACITY = 0.5;
    private static final double FULL_FOOTER_OPACITY = 1.0;
    private static final double DEFAULT_FOOTER_BACKGROUND_TRANSPARENCY = 0.5;
    private static final Duration DEFAULT_TOOLTIP_SHOW_DELAY = Duration.millis(150);
    private static final Color DEFAULT_FOOTER_BACKGROUND_COLOR = Color.rgb(10, 20, 38);
    private static final Color DEFAULT_FOOTER_BORDER_COLOR = Color.web("#143869");
    private static final CornerRadii FOOTER_CORNER_RADII = new CornerRadii(999);
    private static final int BACKGROUND_SVG_RASTER_MIN_WIDTH = 1920;
    private static final int BACKGROUND_SVG_RASTER_MIN_HEIGHT = 1080;
    private static final String FOOTER_BACKGROUND_COLOR_PROPERTY = "screenFooterBackgroundColor";
    private static final String FOOTER_BACKGROUND_TRANSPARENCY_PROPERTY = "screenFooterBackgroundTransparency";
    private static final String FOOTER_BORDER_STYLE_PROPERTY = "screenFooterBorderStyle";
    private static final String FOOTER_BORDER_COLOR_PROPERTY = "screenFooterBorderColor";
    private static final String FOOTER_BORDER_SIZE_PROPERTY = "screenFooterBorderSize";
    private static final FooterShortcutDisplay DEFAULT_FOOTER_SHORTCUT_DISPLAY = FooterShortcutDisplay.TOOLTIP_ONLY;
    private static final Map<String, Image> FOOTER_ICON_CACHE = new ConcurrentHashMap<>();
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
        root.setMinSize(0, 0);
        body.setMinSize(0, 0);
        root.setTop(header);
        root.setCenter(body);
        if (footerOptions != null && !footerOptions.isEmpty()) {
            root.setBottom(footerBar(footerOptions));
            pinFooterToBottom(root);
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
        HBox footer = new HBox();
        footer.getStyleClass().add(SCREEN_FOOTER_BAR_STYLE_CLASS);
        footer.setMaxHeight(Region.USE_PREF_SIZE);
        configureDefaultFooterPresentation(footer);
        for (FooterOption option : footerOptions) {
            Validation.requireNonNull(option, "Footer option is required.");
            Label label = new Label();
            label.setUserData(option);
            label.getStyleClass().add(SCREEN_FOOTER_OPTION_STYLE_CLASS);
            applyFooterOption(label, option, DEFAULT_FOOTER_SHORTCUT_DISPLAY);
            footer.getChildren().add(label);
        }
        return footer;
    }

    static void configureDefaultFooterPresentation(HBox footer) {
        Validation.requireNonNull(footer, "Footer node is required.");
        footer.setSpacing(FOOTER_SPACING);
        footer.setOpacity(DEFAULT_FOOTER_OPACITY);
        footer.setOnMouseEntered(event -> footer.setOpacity(FULL_FOOTER_OPACITY));
        footer.setOnMouseExited(event -> footer.setOpacity(DEFAULT_FOOTER_OPACITY));
    }

    static void pinFooterToBottom(BorderPane screen) {
        Validation.requireNonNull(screen, "Screen shell is required.");
        if (screen.getBottom() != null) {
            screen.getBottom().setManaged(true);
            BorderPane.setAlignment(screen.getBottom(), Pos.BOTTOM_CENTER);
            BorderPane.setMargin(screen.getBottom(), FOOTER_INSETS);
        }
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

    /** Sets the footer option font size in pixels. */
    public static void setFooterFontSize(Node footer, double fontSize) {
        Validation.requirePositive(fontSize, "Footer font size must be positive.");
        forEachFooterLabel(footer, label ->
                label.setStyle(appendStyle(label.getStyle(), "-fx-font-size: " + fontSize + "px;")));
    }

    /** Sets the footer option text color using a JavaFX web color string. */
    public static void setFooterTextColor(Node footer, String color) {
        Color parsedColor = parseColor(color, "Footer text color is required.");
        forEachFooterLabel(footer, label -> label.setTextFill(parsedColor));
    }

    /** Sets the footer background color while preserving the configured background transparency. */
    public static void setFooterBackgroundColor(Node footer, String color) {
        Validation.requireNonNull(footer, "Footer node is required.");
        footer.getProperties().put(FOOTER_BACKGROUND_COLOR_PROPERTY, parseColor(color, "Footer background color is required."));
        applyFooterBackground(footer);
    }

    /** Sets footer background transparency where {@code 0.0} is opaque and {@code 1.0} is invisible. */
    public static void setFooterBackgroundTransparency(Node footer, double transparency) {
        Validation.requireNonNull(footer, "Footer node is required.");
        footer.getProperties().put(FOOTER_BACKGROUND_TRANSPARENCY_PROPERTY, Validation.requireUnitInterval(
                transparency,
                "Footer background transparency must be between 0.0 and 1.0."));
        applyFooterBackground(footer);
    }

    /** Sets the footer border style: none, solid, dashed, or dotted. */
    public static void setFooterBorderStyle(Node footer, String borderStyle) {
        Validation.requireNonNull(footer, "Footer node is required.");
        footer.getProperties().put(FOOTER_BORDER_STYLE_PROPERTY, parseBorderStyle(borderStyle));
        applyFooterBorder(footer);
    }

    /** Sets the footer border color using a JavaFX web color string. */
    public static void setFooterBorderColor(Node footer, String color) {
        Validation.requireNonNull(footer, "Footer node is required.");
        footer.getProperties().put(FOOTER_BORDER_COLOR_PROPERTY, parseColor(color, "Footer border color is required."));
        applyFooterBorder(footer);
    }

    /** Sets the footer border width in pixels. A size of {@code 0.0} removes the border. */
    public static void setFooterBorderSize(Node footer, double borderSize) {
        Validation.requireNonNull(footer, "Footer node is required.");
        footer.getProperties().put(FOOTER_BORDER_SIZE_PROPERTY, Validation.requireBetween(
                borderSize,
                0.0,
                Double.MAX_VALUE,
                "Footer border size cannot be negative."));
        applyFooterBorder(footer);
    }

    /** Applies compact mobile footer presentation, including icon-only labels and tighter spacing. */
    public static void setFooterCompact(Node footer, boolean compact) {
        Validation.requireNonNull(footer, "Footer node is required.");
        if (compact) {
            if (!footer.getStyleClass().contains(SCREEN_FOOTER_COMPACT_STYLE_CLASS)) {
                footer.getStyleClass().add(SCREEN_FOOTER_COMPACT_STYLE_CLASS);
            }
        } else {
            footer.getStyleClass().remove(SCREEN_FOOTER_COMPACT_STYLE_CLASS);
        }
        if (footer instanceof HBox footerBox) {
            footerBox.setSpacing(compact ? COMPACT_FOOTER_SPACING : FOOTER_SPACING);
        }
        setFooterLabelsVisible(footer, !compact);
    }

    /** Switches footer labels between full text and icon-only presentation. */
    public static void setFooterLabelsVisible(Node footer, boolean labelsVisible) {
        Validation.requireNonNull(footer, "Footer node is required.");
        if (footer instanceof HBox footerBox) {
            footerBox.getChildren().forEach(child -> {
                if (child instanceof Label label && label.getUserData() instanceof FooterOption option) {
                    applyFooterOption(label, option, option.displayText(labelsVisible), option.tooltip());
                }
            });
        }
    }

    /** Switches footer shortcut text between visible, hidden, and tooltip-only presentation. */
    public static void setFooterShortcutDisplay(Node footer, FooterShortcutDisplay shortcutDisplay) {
        Validation.requireNonNull(footer, "Footer node is required.");
        FooterShortcutDisplay checkedDisplay = shortcutDisplay == null
                ? DEFAULT_FOOTER_SHORTCUT_DISPLAY
                : shortcutDisplay;
        if (footer instanceof HBox footerBox) {
            footerBox.getChildren().forEach(child -> {
                if (child instanceof Label label && label.getUserData() instanceof FooterOption option) {
                    applyFooterOption(label, option, checkedDisplay);
                }
            });
        }
    }

    /** Applies the persisted user preference for showing footer labels. */
    public static void applyFooterPreferences(Node footer, PreferencesService preferencesService) {
        Validation.requireNonNull(preferencesService, "Preferences service is required.");
        setFooterShortcutDisplay(footer, preferencesService.footerShortcutDisplay());
    }

    /** Adds responsive compact/mobile footer behavior based on the screen width. */
    public static void configureResponsiveFooter(BorderPane screen, double compactWidth) {
        Validation.requireNonNull(screen, "Screen shell is required.");
        Validation.requirePositive(compactWidth, "Compact footer width must be positive.");
        Node footer = screen.getBottom();
        if (footer == null) {
            return;
        }
        setFooterCompact(footer, isCompactFooterWidth(screen.getWidth(), compactWidth));
        screen.widthProperty().addListener((observable, oldWidth, newWidth) ->
                setFooterCompact(footer, isCompactFooterWidth(newWidth.doubleValue(), compactWidth)));
    }

    public static List<FooterOption> defaultFooterOptions() {
        return FOOTER_OPTIONS;
    }

    public static List<FooterOption> changeFooterIcon(List<FooterOption> options, String id, String icon) {
        return replaceFooterOption(options, id, option -> option.withIcon(icon));
    }

    public static List<FooterOption> changeFooterIconResourcePath(
            List<FooterOption> options,
            String id,
            String iconResourcePath) {
        return replaceFooterOption(options, id, option -> option.withIconResourcePath(iconResourcePath));
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

    public static List<FooterOption> changeFooterEnabled(List<FooterOption> options, String id, boolean enabled) {
        return replaceFooterOption(options, id, option -> option.withEnabled(enabled));
    }

    public static List<FooterOption> footerOptionsForGameState(GameState gameState) {
        boolean historyAvailable = gameState != null && !gameState.conversationHistory().entries().isEmpty();
        return changeFooterEnabled(FOOTER_OPTIONS, "history", historyAvailable);
    }

    public static List<FooterOption> localizeFooterOptions(
            List<FooterOption> options,
            LocalizationService localizationService) {
        Validation.requireNonNull(options, "Footer options are required.");
        Validation.requireNonNull(localizationService, "Localization service is required.");
        return options.stream()
                .map(option -> {
                    Validation.requireNonNull(option, "Footer option is required.");
                    String prefix = "ui.footer." + option.id();
                    return option
                            .withLabel(localizationService.text(prefix + ".label").orElse(option.label()))
                            .withTooltip(localizationService.text(prefix + ".tooltip").orElse(option.tooltip()));
                })
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

    /**
     * Wraps one or more full-screen SVG backgrounds behind a screen root.
     *
     * <p>Each SVG background is mouse-transparent, borderless, and resized with the returned
     * stack pane. Backgrounds are layered in argument order behind the screen content. Use the
     * returned pane as the scene root so the SVG layers cover the entire screen.</p>
     *
     * @param screen screen content to layer above the background
     * @param svgResourcePath first packaged SVG resource path
     * @param additionalSvgResourcePaths additional packaged SVG resource paths to layer above the first
     * @return stack pane containing the background layers and screen content
     */
    public static StackPane withBackgroundSvg(
            Region screen,
            String svgResourcePath,
            String... additionalSvgResourcePaths) {
        Validation.requireNonNull(screen, "Screen background content is required.");
        List<Region> backgrounds = backgroundSvgLayers(svgResourcePath, additionalSvgResourcePaths);
        StackPane root = new StackPane();
        root.getChildren().addAll(backgrounds);
        root.getChildren().add(screen);
        root.setMinSize(0, 0);
        backgrounds.forEach(background -> configureBackgroundLayer(background, root));
        screen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return root;
    }

    /**
     * Creates a screen root with one or more full-screen SVG backgrounds behind the supplied content.
     *
     * @param screen screen content to layer above the background
     * @param svgResourcePath first packaged SVG resource path
     * @param additionalSvgResourcePaths additional packaged SVG resource paths to layer above the first
     * @return stack pane containing the background layers and screen content
     */
    public static StackPane setBackgroundSvg(
            Region screen,
            String svgResourcePath,
            String... additionalSvgResourcePaths) {
        return withBackgroundSvg(screen, svgResourcePath, additionalSvgResourcePaths);
    }

    /**
     * Creates a full-screen SVG background node.
     *
     * <p>The returned node is not clickable and has no border or padding.</p>
     *
     * @param svgResourcePath packaged SVG resource path
     * @return resizable SVG background region
     */
    public static Region backgroundSvg(String svgResourcePath) {
        return backgroundSvg(svgResourcePath, 1.0, Color.TRANSPARENT);
    }

    /**
     * Creates a full-screen SVG background node with configurable image opacity and canvas color.
     *
     * <p>The returned node is not clickable and has no border or padding.</p>
     *
     * @param svgResourcePath packaged SVG resource path
     * @param opacity opacity applied to the SVG image from {@code 0.0} to {@code 1.0}
     * @param canvasBackgroundColor background fill shown behind transparent SVG regions
     * @return resizable SVG background region
     */
    public static Region backgroundSvg(String svgResourcePath, double opacity, Color canvasBackgroundColor) {
        Validation.requireUnitInterval(opacity, "Screen SVG background opacity must be between 0 and 1.");
        Validation.requireNonNull(canvasBackgroundColor, "Screen SVG background canvas color is required.");
        return new SvgBackground(svgResourcePath, opacity, canvasBackgroundColor);
    }

    private static List<Region> backgroundSvgLayers(String svgResourcePath, String... additionalSvgResourcePaths) {
        List<Region> backgrounds = new ArrayList<>();
        backgrounds.add(backgroundSvg(svgResourcePath));
        if (additionalSvgResourcePaths != null) {
            for (String additionalSvgResourcePath : additionalSvgResourcePaths) {
                backgrounds.add(backgroundSvg(additionalSvgResourcePath));
            }
        }
        return backgrounds;
    }

    private static void configureBackgroundLayer(Region background, StackPane root) {
        background.prefWidthProperty().bind(root.widthProperty());
        background.prefHeightProperty().bind(root.heightProperty());
        background.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
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

    private static void forEachFooterLabel(Node footer, java.util.function.Consumer<Label> action) {
        Validation.requireNonNull(footer, "Footer node is required.");
        Validation.requireNonNull(action, "Footer label action is required.");
        if (footer instanceof HBox footerBox) {
            footerBox.getChildren().forEach(child -> {
                if (child instanceof Label label) {
                    action.accept(label);
                }
            });
        }
    }

    private static Color parseColor(String color, String message) {
        try {
            return Color.web(Validation.requireNonBlank(color, message));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(message, exception);
        }
    }

    private static String appendStyle(String existingStyle, String style) {
        if (existingStyle == null || existingStyle.isBlank()) {
            return style;
        }
        return existingStyle.stripTrailing() + " " + style;
    }

    private static void applyFooterBackground(Node footer) {
        if (footer instanceof Region footerRegion) {
            Color color = propertyValue(
                    footer,
                    FOOTER_BACKGROUND_COLOR_PROPERTY,
                    Color.class,
                    DEFAULT_FOOTER_BACKGROUND_COLOR);
            double transparency = propertyValue(
                    footer,
                    FOOTER_BACKGROUND_TRANSPARENCY_PROPERTY,
                    Double.class,
                    DEFAULT_FOOTER_BACKGROUND_TRANSPARENCY);
            footerRegion.setBackground(new Background(new BackgroundFill(
                    new Color(color.getRed(), color.getGreen(), color.getBlue(), 1.0 - transparency),
                    FOOTER_CORNER_RADII,
                    Insets.EMPTY)));
        }
    }

    private static void applyFooterBorder(Node footer) {
        if (footer instanceof Region footerRegion) {
            BorderStrokeStyle style = propertyValue(
                    footer,
                    FOOTER_BORDER_STYLE_PROPERTY,
                    BorderStrokeStyle.class,
                    BorderStrokeStyle.NONE);
            double size = propertyValue(footer, FOOTER_BORDER_SIZE_PROPERTY, Double.class, 0.0);
            if (style == BorderStrokeStyle.NONE || size == 0.0) {
                footerRegion.setBorder(Border.EMPTY);
                return;
            }
            Color color = propertyValue(
                    footer,
                    FOOTER_BORDER_COLOR_PROPERTY,
                    Color.class,
                    DEFAULT_FOOTER_BORDER_COLOR);
            footerRegion.setBorder(new Border(new BorderStroke(
                    color,
                    style,
                    FOOTER_CORNER_RADII,
                    new BorderWidths(size))));
        }
    }

    private static BorderStrokeStyle parseBorderStyle(String borderStyle) {
        String normalizedStyle = Validation.requireNonBlank(borderStyle, "Footer border style is required.")
                .toLowerCase();
        return switch (normalizedStyle) {
            case "none" -> BorderStrokeStyle.NONE;
            case "solid" -> BorderStrokeStyle.SOLID;
            case "dashed" -> BorderStrokeStyle.DASHED;
            case "dotted" -> BorderStrokeStyle.DOTTED;
            default -> throw new IllegalArgumentException("Footer border style must be none, solid, dashed, or dotted.");
        };
    }

    private static <T> T propertyValue(Node node, String key, Class<T> type, T defaultValue) {
        Object value = node.getProperties().get(key);
        return type.isInstance(value) ? type.cast(value) : defaultValue;
    }

    public static void applyFooterOption(Label label, FooterOption option, FooterShortcutDisplay shortcutDisplay) {
        Validation.requireNonNull(label, "Footer label is required.");
        Validation.requireNonNull(option, "Footer option is required.");
        FooterShortcutDisplay checkedDisplay = shortcutDisplay == null
                ? DEFAULT_FOOTER_SHORTCUT_DISPLAY
                : shortcutDisplay;
        applyFooterOption(label, option, option.displayText(checkedDisplay), option.tooltipText(checkedDisplay));
    }

    private static void applyFooterOption(Label label, FooterOption option, String displayText, String tooltipText) {
        Validation.requireNonNull(label, "Footer label is required.");
        Validation.requireNonNull(option, "Footer option is required.");
        label.setUserData(option);
        ImageView graphic = footerGraphic(option);
        label.setGraphic(graphic);
        label.setGraphicTextGap(4);
        label.setText(graphic == null ? displayText : footerTextWithoutFallbackIcon(option, displayText));
        applyFooterOptionState(label, option);
        installFooterTooltip(label, tooltipText);
    }

    static ImageView footerGraphic(FooterOption option) {
        if (option.iconResourcePath().isBlank()) {
            return null;
        }
        Image image = FOOTER_ICON_CACHE.computeIfAbsent(option.iconResourcePath(), ScreenShell::loadFooterIcon);
        if (image == null) {
            return null;
        }
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(FOOTER_ICON_SIZE);
        imageView.setFitHeight(FOOTER_ICON_SIZE);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private static Image loadFooterIcon(String resourcePath) {
        try (InputStream inputStream = ScreenShell.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                return null;
            }
            return VectorImage.fromInputStream(inputStream).toRasterImage(FOOTER_ICON_SIZE, FOOTER_ICON_SIZE);
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            return null;
        }
    }

    private static Image loadBackgroundSvgImage(String resourcePath) {
        URL resource = resolveResource(resourcePath);
        try (InputStream inputStream = resource.openStream()) {
            VectorImage image = VectorImage.fromInputStream(inputStream);
            image.getSvgDocument().getDocumentElement().setAttribute("preserveAspectRatio", "none");
            Dimension2D rasterSize = backgroundSvgRasterSize(image);
            return image.toRasterImage((int) Math.ceil(rasterSize.getWidth()), (int) Math.ceil(rasterSize.getHeight()));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to load SVG background resource: " + resourcePath, exception);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new IllegalArgumentException("SVG background resource has invalid format: " + resourcePath, exception);
        }
    }

    private static Dimension2D backgroundSvgRasterSize(VectorImage image) {
        double width = Math.max(BACKGROUND_SVG_RASTER_MIN_WIDTH, image.getWidth());
        double height = Math.max(BACKGROUND_SVG_RASTER_MIN_HEIGHT, image.getHeight());
        if (!Platform.isFxApplicationThread()) {
            return new Dimension2D(width, height);
        }
        try {
            for (Screen screen : Screen.getScreens()) {
                width = Math.max(width, screen.getBounds().getWidth() * screen.getOutputScaleX());
                height = Math.max(height, screen.getBounds().getHeight() * screen.getOutputScaleY());
            }
        } catch (IllegalStateException ignored) {
            // Screen enumeration can fail before the JavaFX toolkit is fully initialized.
        }
        return new Dimension2D(width, height);
    }

    private static URL resolveResource(String resourcePath) {
        String checkedPath = Validation.requireNonBlank(resourcePath, "Screen SVG background resource is required.");
        URL resource = ScreenShell.class.getResource(checkedPath);
        if (resource == null) {
            String classLoaderPath = checkedPath.startsWith("/") ? checkedPath.substring(1) : checkedPath;
            resource = ScreenShell.class.getClassLoader().getResource(classLoaderPath);
        }
        if (resource == null) {
            throw new IllegalArgumentException("Screen SVG background resource is missing: " + checkedPath);
        }
        return resource;
    }

    static String footerTextWithoutFallbackIcon(FooterOption option, String displayText) {
        if (displayText.equals(option.icon())) {
            return "";
        }
        String iconPrefix = option.icon() + " ";
        if (displayText.startsWith(iconPrefix)) {
            return displayText.substring(iconPrefix.length());
        }
        return displayText;
    }

    private static void applyFooterOptionState(Label label, FooterOption option) {
        label.setAccessibleText(option.accessibleText());
        label.setDisable(false);
        if (option.enabled()) {
            label.getStyleClass().remove(SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS);
        } else if (!label.getStyleClass().contains(SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS)) {
            label.getStyleClass().add(SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS);
        }
    }

    private static void installFooterTooltip(Label label, String tooltip) {
        Tooltip.uninstall(label, label.getTooltip());
        label.setTooltip(null);
        if (tooltip == null || tooltip.isBlank()) {
            return;
        }
        label.setAccessibleHelp(tooltip);
        Tooltip tooltipNode = createTooltip(tooltip);
        Tooltip.install(label, tooltipNode);
        if (Platform.isFxApplicationThread()) {
            label.setTooltip(tooltipNode);
        } else {
            try {
                Platform.runLater(() -> label.setTooltip(tooltipNode));
            } catch (IllegalStateException exception) {
                label.setAccessibleText(label.getAccessibleText() + " - " + tooltip);
            }
        }
    }

    public static boolean isFooterOptionEnabled(Label label) {
        Validation.requireNonNull(label, "Footer label is required.");
        return label.getUserData() instanceof FooterOption option && option.enabled();
    }

    public static Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(DEFAULT_TOOLTIP_SHOW_DELAY);
        return tooltip;
    }

    private static boolean isCompactFooterWidth(double width, double compactWidth) {
        return width > 0.0 && width <= compactWidth;
    }

    public static String defaultFooterIconResourcePath(String id) {
        String checkedId = Validation.requireNonBlank(id, "Footer option id is required.");
        return DEFAULT_FOOTER_ICON_RESOURCE_DIRECTORY + "/footer-" + checkedId + ".svg";
    }

    private static final class SvgBackground extends Region {
        private final ImageView imageView;

        private SvgBackground(String svgResourcePath, double opacity, Color canvasBackgroundColor) {
            imageView = new ImageView(loadBackgroundSvgImage(svgResourcePath));
            getStyleClass().add(SCREEN_BACKGROUND_SVG_STYLE_CLASS);
            setMinSize(0, 0);
            setBorder(Border.EMPTY);
            setBackground(canvasBackgroundColor.equals(Color.TRANSPARENT)
                    ? Background.EMPTY
                    : new Background(new BackgroundFill(canvasBackgroundColor, CornerRadii.EMPTY, Insets.EMPTY)));
            setMouseTransparent(true);
            setFocusTraversable(false);
            imageView.setMouseTransparent(true);
            imageView.setFocusTraversable(false);
            imageView.setOpacity(opacity);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imageView.fitWidthProperty().bind(widthProperty());
            imageView.fitHeightProperty().bind(heightProperty());
            getChildren().add(imageView);
        }

        @Override
        protected void layoutChildren() {
            imageView.resizeRelocate(0, 0, getWidth(), getHeight());
        }

        @Override
        protected double computePrefWidth(double height) {
            return 0;
        }

        @Override
        protected double computePrefHeight(double width) {
            return 0;
        }
    }

    /** One footer action rendered by the shell with icon, shortcut, tooltip, and enabled state metadata. */
    public record FooterOption(
            String id,
            String icon,
            String label,
            String shortcut,
            String tooltip,
            boolean enabled,
            String iconResourcePath) {
        public FooterOption(String id, String icon, String label, String shortcut, String tooltip) {
            this(id, icon, label, shortcut, tooltip, true);
        }

        public FooterOption(String id, String icon, String label, String shortcut, String tooltip, boolean enabled) {
            this(id, icon, label, shortcut, tooltip, enabled, defaultFooterIconResourcePath(id));
        }

        public FooterOption {
            id = Validation.requireNonBlank(id, "Footer option id is required.");
            icon = Validation.requireNonBlank(icon, "Footer option icon is required.");
            label = Validation.requireNonBlank(label, "Footer option label is required.");
            shortcut = Validation.requireNonBlank(shortcut, "Footer option shortcut is required.");
            tooltip = tooltip == null ? "" : tooltip;
            iconResourcePath = iconResourcePath == null ? "" : iconResourcePath;
        }

        public String displayText() {
            return displayText(true);
        }

        public String displayText(boolean labelsVisible) {
            if (!labelsVisible) {
                return icon;
            }
            return icon + " " + label + " (" + shortcut + ")";
        }

        public String displayText(FooterShortcutDisplay shortcutDisplay) {
            FooterShortcutDisplay checkedDisplay = shortcutDisplay == null
                    ? DEFAULT_FOOTER_SHORTCUT_DISPLAY
                    : shortcutDisplay;
            if (checkedDisplay == FooterShortcutDisplay.DISPLAY) {
                return displayText();
            }
            return icon + " " + label;
        }

        public String tooltipText(FooterShortcutDisplay shortcutDisplay) {
            FooterShortcutDisplay checkedDisplay = shortcutDisplay == null
                    ? DEFAULT_FOOTER_SHORTCUT_DISPLAY
                    : shortcutDisplay;
            if (checkedDisplay == FooterShortcutDisplay.HIDE) {
                return tooltip;
            }
            String shortcutText = "Keyboard shortcut: " + shortcut + ".";
            if (tooltip == null || tooltip.isBlank()) {
                return shortcutText;
            }
            return tooltip.endsWith(".") ? tooltip + " " + shortcutText : tooltip + ". " + shortcutText;
        }

        public String accessibleText() {
            return label + " - Keyboard shortcut: " + shortcut;
        }

        public FooterOption withIcon(String icon) {
            return new FooterOption(id, icon, label, shortcut, tooltip, enabled, iconResourcePath);
        }

        public FooterOption withIconResourcePath(String iconResourcePath) {
            return new FooterOption(id, icon, label, shortcut, tooltip, enabled, iconResourcePath);
        }

        public FooterOption withLabel(String label) {
            return new FooterOption(id, icon, label, shortcut, tooltip, enabled, iconResourcePath);
        }

        public FooterOption withShortcut(String shortcut) {
            return new FooterOption(id, icon, label, shortcut, tooltip, enabled, iconResourcePath);
        }

        public FooterOption withTooltip(String tooltip) {
            return new FooterOption(id, icon, label, shortcut, tooltip, enabled, iconResourcePath);
        }

        public FooterOption withEnabled(boolean enabled) {
            return new FooterOption(id, icon, label, shortcut, tooltip, enabled, iconResourcePath);
        }
    }
}
