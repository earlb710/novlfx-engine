package com.eb.javafx.ui;

import com.eb.javafx.localization.LocalizationService;
import com.eb.javafx.gamesupport.SystemCodeTables;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.prefs.PreferencesService.FooterIconDisplay;
import com.eb.javafx.prefs.PreferencesService.FooterShortcutDisplay;
import com.eb.javafx.state.GameState;
import com.eb.javafx.util.Validation;
import com.eb.javafx.util.VectorImage;
import javafx.application.Platform;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
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

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    public static final String SCREEN_SUBTITLE_STYLE_CLASS = "screen-subtitle";
    public static final String SCREEN_TEXT_STYLE_CLASS = "screen-text";
    public static final String SCREEN_TEXT_HIGHLIGHT_STYLE_CLASS = "screen-text-highlight";
    public static final String SCREEN_VALUE_STYLE_CLASS = "screen-value";
    public static final String SCREEN_BACKGROUND_SVG_STYLE_CLASS = "screen-background-svg";
    public static final String SCREEN_FOOTER_BAR_STYLE_CLASS = "screen-footer-bar";
    public static final String SCREEN_FOOTER_OPTION_STYLE_CLASS = "screen-footer-option";
    public static final String SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS = "screen-footer-option-disabled";
    /**
     * Applied to a footer-option label when its underlying mode is currently <em>active</em>
     * (e.g. auto-skip is engaged). Distinct from disabled — the option is still clickable and
     * should look prominent. The default stylesheet renders this as bright white + bold.
     */
    public static final String SCREEN_FOOTER_OPTION_ACTIVE_STYLE_CLASS = "screen-footer-option-active";
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
    public static final String LAYOUT_TEXT_HIGHLIGHT_STYLE_CLASS = "layout-text-highlight";
    public static final String LAYOUT_VALUE_STYLE_CLASS = "layout-value";
    public static final String LAYOUT_ACTION_ROW_STYLE_CLASS = "layout-action-row";
    public static final String LAYOUT_PRIMARY_ACTION_STYLE_CLASS = "layout-primary-action";
    public static final String LAYOUT_SECONDARY_ACTION_STYLE_CLASS = "layout-secondary-action";
    public static final double DEFAULT_BODY_SPACING = 12;
    public static final double DEFAULT_OUTER_INSET = 16;
    public static final double DEFAULT_PANEL_INSET = 16;
    private static final double DEFAULT_FOOTER_SPACING = 14;
    private static final double DEFAULT_FOOTER_REST_OPACITY = 0.5;
    private static final double DEFAULT_FOOTER_HOVER_OPACITY = 1.0;

    // Config-overridable spacing / insets (the `ui.spacing` config object, applied at boot before
    // any scene is built).  Non-final so a mod can retune the body gap and outer/panel margins;
    // defaults preserve the original 12 / 16 / 16 layout.
    public static double BODY_SPACING = DEFAULT_BODY_SPACING;
    public static Insets OUTER_INSETS = new Insets(DEFAULT_OUTER_INSET);
    public static Insets PANEL_INSETS = new Insets(DEFAULT_PANEL_INSET);
    static final Insets FOOTER_INSETS = new Insets(1);
    private static Insets OUTER_INSETS_WITHOUT_BOTTOM = outerInsetsWithoutBottom(OUTER_INSETS);
    private static double FOOTER_SPACING = DEFAULT_FOOTER_SPACING;
    private static final double COMPACT_FOOTER_SPACING = 6;
    private static final int FOOTER_ICON_SIZE = 14;
    // Config-overridable footer rest / hover opacity (`footer.restOpacity` / `footer.hoverOpacity`).
    private static double footerRestOpacity = DEFAULT_FOOTER_REST_OPACITY;
    private static double footerHoverOpacity = DEFAULT_FOOTER_HOVER_OPACITY;

    /** The outer margin with the bottom edge zeroed (header/body sit flush to the footer). */
    private static Insets outerInsetsWithoutBottom(Insets outer) {
        return new Insets(outer.getTop(), outer.getRight(), 0, outer.getLeft());
    }

    /** Overrides screen spacing / insets (the {@code ui.spacing} config object): body gap, outer
     *  margin, panel padding, footer gap.  Null args keep the current value; negatives are ignored.
     *  Call once at boot before scenes are built. */
    public static void setSpacing(Double body, Double outer, Double panel, Double footer) {
        if (body != null && body >= 0) {
            BODY_SPACING = body;
        }
        if (outer != null && outer >= 0) {
            OUTER_INSETS = new Insets(outer);
            OUTER_INSETS_WITHOUT_BOTTOM = outerInsetsWithoutBottom(OUTER_INSETS);
        }
        if (panel != null && panel >= 0) {
            PANEL_INSETS = new Insets(panel);
        }
        if (footer != null && footer >= 0) {
            FOOTER_SPACING = footer;
        }
    }

    /** Overrides the footer resting / hover opacity (`footer.restOpacity` / `footer.hoverOpacity`).
     *  Null args keep the current value; others are clamped to {@code [0, 1]}.  Call once at boot. */
    public static void setFooterOpacity(Double rest, Double hover) {
        if (rest != null) {
            footerRestOpacity = Math.max(0.0, Math.min(1.0, rest));
        }
        if (hover != null) {
            footerHoverOpacity = Math.max(0.0, Math.min(1.0, hover));
        }
    }
    private static final double DEFAULT_FOOTER_BACKGROUND_TRANSPARENCY = 0.5;
    private static final Duration DEFAULT_TOOLTIP_SHOW_DELAY = Duration.millis(150);
    /** Config-driven tooltip show-delay override (null = use {@link #DEFAULT_TOOLTIP_SHOW_DELAY}). */
    private static volatile Duration tooltipShowDelayOverride;

    /** Sets the global tooltip show-delay (milliseconds) from config; null/non-positive clears. */
    public static void setTooltipShowDelayMillis(Double millis) {
        tooltipShowDelayOverride = (millis == null || millis <= 0) ? null : Duration.millis(millis);
    }

    private static Duration tooltipShowDelay() {
        Duration override = tooltipShowDelayOverride;
        return override != null ? override : DEFAULT_TOOLTIP_SHOW_DELAY;
    }
    private static final Color DEFAULT_FOOTER_BACKGROUND_COLOR = Color.rgb(10, 20, 38);
    private static final Color DEFAULT_FOOTER_BORDER_COLOR = Color.web("#143869");
    private static final CornerRadii FOOTER_CORNER_RADII = new CornerRadii(999);
    private static final int DEFAULT_BACKGROUND_SVG_RASTER_MIN_WIDTH = 1920;
    private static final int DEFAULT_BACKGROUND_SVG_RASTER_MIN_HEIGHT = 1080;
    /** Config-overridable minimum raster size for SVG backgrounds (the `display.svgBackgroundMinRaster`
     *  config object). Defaults to 1920×1080. */
    private static volatile int backgroundSvgRasterMinWidth = DEFAULT_BACKGROUND_SVG_RASTER_MIN_WIDTH;
    private static volatile int backgroundSvgRasterMinHeight = DEFAULT_BACKGROUND_SVG_RASTER_MIN_HEIGHT;

    /** Sets the minimum raster dimensions used when rasterising an SVG background; null / non-positive
     *  args keep the current value. Called once at boot. */
    public static void setBackgroundSvgRasterMinSize(Integer width, Integer height) {
        if (width != null && width > 0) {
            backgroundSvgRasterMinWidth = width;
        }
        if (height != null && height > 0) {
            backgroundSvgRasterMinHeight = height;
        }
    }
    private static final String FOOTER_BACKGROUND_COLOR_PROPERTY = "screenFooterBackgroundColor";
    private static final String FOOTER_BACKGROUND_TRANSPARENCY_PROPERTY = "screenFooterBackgroundTransparency";
    private static final String FOOTER_BORDER_STYLE_PROPERTY = "screenFooterBorderStyle";
    private static final String FOOTER_BORDER_COLOR_PROPERTY = "screenFooterBorderColor";
    private static final String FOOTER_BORDER_SIZE_PROPERTY = "screenFooterBorderSize";
    private static final FooterShortcutDisplay DEFAULT_FOOTER_SHORTCUT_DISPLAY = FooterShortcutDisplay.TOOLTIP_ONLY;
    private static final FooterIconDisplay DEFAULT_FOOTER_ICON_DISPLAY = FooterIconDisplay.ICONS_WITH_TEXT;
    static final String FOOTER_ICON_SOURCE_COLOR = "#ffcc00";
    private static final Map<String, Image> FOOTER_ICON_CACHE = new ConcurrentHashMap<>();
    private static final List<FooterOption> FOOTER_OPTIONS = List.of(
            footerOption("back", "‹", "Backspace"),
            footerOption("history", "◷", "Ctrl+H"),
            footerOption("skip-mode", "⇥", "Tab"),
            footerOption("load", "⇩", "Ctrl+L"),
            footerOption("save", "▣", "Ctrl+S"),
            footerOption("quick-save", "⚡", "Ctrl+Q"),
            footerOption("preferences", "⚙", "Ctrl+P"),
            footerOption("forward", "›", "Space"));

    private ScreenShell() {
    }

    private static FooterOption footerOption(String id, String icon, String shortcut) {
        return new FooterOption(
                id,
                icon,
                SystemCodeTables.defaultMessage("footer." + id + ".label"),
                shortcut,
                SystemCodeTables.defaultMessage("footer." + id + ".tooltip"));
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
        footer.setOpacity(footerRestOpacity);
        footer.setOnMouseEntered(event -> footer.setOpacity(footerHoverOpacity));
        footer.setOnMouseExited(event -> footer.setOpacity(footerRestOpacity));
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

    /** Applies the persisted user preferences for footer shortcut and icon display. */
    public static void applyFooterPreferences(Node footer, PreferencesService preferencesService) {
        applyFooterPreferences(footer, preferencesService, (UiTheme) null);
    }

    /** Applies footer shortcut display, icon display, and the theme icon color from {@code uiTheme}. */
    public static void applyFooterPreferences(Node footer, PreferencesService preferencesService, UiTheme uiTheme) {
        Validation.requireNonNull(preferencesService, "Preferences service is required.");
        FooterShortcutDisplay shortcutDisplay = preferencesService.footerShortcutDisplay();
        FooterIconDisplay iconDisplay = preferencesService.footerIconDisplay();
        String iconColor = uiTheme != null ? uiTheme.footerIconColor() : null;
        if (footer instanceof HBox footerBox) {
            footerBox.getChildren().forEach(child -> {
                if (child instanceof Label label && label.getUserData() instanceof FooterOption option) {
                    applyFooterOption(label, option, shortcutDisplay, iconDisplay, iconColor);
                }
            });
        }
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
        return applyFooterOptionOverrides(FOOTER_OPTIONS);
    }

    /** Config-driven per-option overrides: {@code id → { "shortcut": ..., "icon": ... }}.  Lets a
     *  game / mod remap footer keyboard shortcuts and glyphs through {@code config.json}. */
    private static volatile Map<String, Map<String, String>> footerOptionOverrides = Map.of();

    /** Installs footer-option keybinding / glyph overrides (keyed by option id).  Pass null/empty
     *  to clear.  Applied to every {@link #footerOptionsForGameState} / {@link #defaultFooterOptions}. */
    public static void setFooterOptionOverrides(Map<String, Map<String, String>> overrides) {
        footerOptionOverrides = (overrides == null || overrides.isEmpty()) ? Map.of() : Map.copyOf(overrides);
    }

    private static List<FooterOption> applyFooterOptionOverrides(List<FooterOption> options) {
        Map<String, Map<String, String>> overrides = footerOptionOverrides;
        if (overrides.isEmpty()) {
            return options;
        }
        return options.stream().map(option -> {
            Map<String, String> override = overrides.get(option.id());
            if (override == null) {
                return option;
            }
            FooterOption result = option;
            String shortcut = override.get("shortcut");
            if (shortcut != null && !shortcut.isBlank()) {
                result = result.withShortcut(shortcut.trim());
            }
            String icon = override.get("icon");
            if (icon != null && !icon.isBlank()) {
                result = result.withIcon(icon.trim());
            }
            return result;
        }).toList();
    }

    /** Shortcut string used by {@link #installDebugScreenInfoShortcut} and the debug-info dialog footer hint. */
    public static final String DEBUG_SCREEN_INFO_SHORTCUT = "Ctrl+D";

    /**
     * Returns {@code true} when a key event matches a footer-option shortcut string such as
     * {@code "Ctrl+D"}, {@code "Space"}, or {@code "Ctrl+Shift+S"}.
     *
     * <p>Recognized tokens: {@code Ctrl}/{@code Cmd}/{@code Meta} (mapped to JavaFX shortcut modifier),
     * {@code Shift}, {@code Alt}/{@code Option}. Key tokens include {@code Space}, {@code Backspace},
     * {@code Tab}, {@code Enter}/{@code Return}, {@code Escape}/{@code Esc}, and any JavaFX
     * {@link KeyCode} name (case-insensitive). Unknown keys return {@code false}.</p>
     */
    public static boolean matchesShortcut(KeyEvent event, String shortcut) {
        Validation.requireNonNull(event, "Key event is required.");
        Validation.requireNonBlank(shortcut, "Footer option shortcut is required.");
        boolean wantShortcut = false;
        boolean wantShift = false;
        boolean wantAlt = false;
        KeyCode wantKey = null;
        for (String rawToken : shortcut.split("\\+")) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }
            String lower = token.toLowerCase(Locale.ROOT);
            switch (lower) {
                case "ctrl", "control", "cmd", "command", "meta" -> wantShortcut = true;
                case "shift" -> wantShift = true;
                case "alt", "option" -> wantAlt = true;
                default -> wantKey = parseShortcutKeyCode(token);
            }
        }
        if (wantKey == null) {
            return false;
        }
        return event.getCode() == wantKey
                && event.isShortcutDown() == wantShortcut
                && event.isShiftDown() == wantShift
                && event.isAltDown() == wantAlt;
    }

    private static KeyCode parseShortcutKeyCode(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "space" -> KeyCode.SPACE;
            case "backspace", "back_space" -> KeyCode.BACK_SPACE;
            case "tab" -> KeyCode.TAB;
            case "enter", "return" -> KeyCode.ENTER;
            case "escape", "esc" -> KeyCode.ESCAPE;
            case "delete", "del" -> KeyCode.DELETE;
            default -> {
                try {
                    yield KeyCode.valueOf(token.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    yield null;
                }
            }
        };
    }

    /**
     * Installs a single {@link KeyEvent#KEY_PRESSED} filter that dispatches footer-shortcut keys to
     * the supplied {@link Runnable} handlers. Handler keys are the footer-option shortcut strings
     * (e.g. {@code "Ctrl+P"}); the first matching binding consumes the event.
     */
    public static void installFooterShortcuts(Scene scene, Map<String, Runnable> handlersByShortcut) {
        Validation.requireNonNull(scene, "Scene is required.");
        Validation.requireNonNull(handlersByShortcut, "Footer shortcut handlers are required.");
        if (handlersByShortcut.isEmpty()) {
            return;
        }
        Map<String, Runnable> bindings = Map.copyOf(handlersByShortcut);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isConsumed()) {
                return;
            }
            for (Map.Entry<String, Runnable> entry : bindings.entrySet()) {
                if (matchesShortcut(event, entry.getKey())) {
                    entry.getValue().run();
                    event.consume();
                    return;
                }
            }
        });
    }

    /**
     * Recursively finds the live footer-bar {@link HBox} in {@code root}'s subtree by matching
     * {@link #SCREEN_FOOTER_BAR_STYLE_CLASS}.  Returns {@code null} when no footer is present
     * (e.g. a screen without a shell, or the footer hasn't been attached yet).
     *
     * <p>Exposed so hosts can locate the footer at fire time without re-implementing the
     * style-class lookup — useful for "shortcut → synthesise click on current footer label"
     * dispatch where the footer Label instances change across navigations.</p>
     */
    public static HBox findFooterBar(Node root) {
        if (root instanceof HBox hbox && hbox.getStyleClass().contains(SCREEN_FOOTER_BAR_STYLE_CLASS)) {
            return hbox;
        }
        if (root instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                HBox found = findFooterBar(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * "Find the live footer, find the enabled Label whose footer-option shortcut matches the
     * supplied key event, and synthesise a {@link MouseEvent#MOUSE_CLICKED} on it" — the dispatch
     * routine hosts use to make footer-shortcut keystrokes behave as if the player clicked the
     * matching footer button.  Returns {@code true} when an enabled match was found and the
     * synthetic click was fired; {@code false} when no footer, no matching shortcut, or the
     * matching option was disabled.
     *
     * <p>Does <em>not</em> consume {@code event} — the caller decides whether to call
     * {@link KeyEvent#consume()} based on the return value.  Typical pattern:</p>
     *
     * <pre>{@code
     * scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
     *     if (event.isConsumed()) return;
     *     if (ScreenShell.dispatchKeyToFooter(scene, event)) {
     *         event.consume();
     *     }
     * });
     * }</pre>
     *
     * <p>The synthetic click goes through the Label's normal MouseEvent dispatch chain, so any
     * {@code setOnMouseClicked} handler the host wired plus any engine-side bindings (e.g.
     * {@code DialogEntriesView.bindHistoryToggle}) fire as they would for a real click.</p>
     */
    public static boolean dispatchKeyToFooter(Scene scene, KeyEvent event) {
        if (scene == null || event == null) {
            return false;
        }
        HBox footer = findFooterBar(scene.getRoot());
        if (footer == null) {
            return false;
        }
        for (Node child : footer.getChildren()) {
            if (!(child instanceof Label label)
                    || !(label.getUserData() instanceof FooterOption option)) {
                continue;
            }
            if (!isFooterOptionEnabled(label)) {
                continue;
            }
            String shortcut = option.shortcut();
            if (shortcut == null || shortcut.isBlank()) {
                continue;
            }
            if (!matchesShortcut(event, shortcut)) {
                continue;
            }
            label.fireEvent(new MouseEvent(
                    MouseEvent.MOUSE_CLICKED,
                    0, 0, 0, 0,
                    MouseButton.PRIMARY,
                    1,
                    /*shiftDown*/ false, /*controlDown*/ false, /*altDown*/ false, /*metaDown*/ false,
                    /*primaryButtonDown*/ true, /*middleButtonDown*/ false, /*secondaryButtonDown*/ false,
                    /*synthesized*/ true, /*popupTrigger*/ false, /*stillSincePress*/ false,
                    /*pickResult*/ null));
            return true;
        }
        return false;
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
        return applyFooterOptionOverrides(changeFooterEnabled(FOOTER_OPTIONS, "history", historyAvailable));
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

    /** Applies configured screen background defaults behind a screen shell. */
    public static Parent withConfiguredBackground(
            BorderPane screen,
            Path applicationRoot,
            String backgroundColor,
            String backgroundImage,
            String backgroundImageTransparency) {
        Validation.requireNonNull(screen, "Screen shell is required.");
        Color canvasColor = configuredBackgroundColor(backgroundColor);
        String imageSource = configuredBackgroundSource(applicationRoot, backgroundImage);
        if (imageSource == null && canvasColor == null) {
            return screen;
        }
        BorderPane configuredScreen = screen;
        configuredScreen.setBackground(Background.EMPTY);
        StackPane root = new StackPane();
        ConfiguredBackgroundLayer backgroundLayer = new ConfiguredBackgroundLayer(
                imageSource == null ? null : ScreenLayoutRenderer.loadBackgroundImage(imageSource),
                imageSource,
                backgroundImage,
                configuredBackgroundOpacity(backgroundImageTransparency),
                canvasColor == null ? Color.TRANSPARENT : canvasColor);
        root.getChildren().add(backgroundLayer);
        root.getChildren().add(configuredScreen);
        root.setMinSize(0, 0);
        configureBackgroundLayer(backgroundLayer, root);
        configuredScreen.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return root;
    }

    /** Finds the screen shell border pane even when it is wrapped in a background stack. */
    public static BorderPane shellRoot(Parent root) {
        if (root instanceof BorderPane borderPane) {
            return borderPane;
        }
        if (root instanceof StackPane stackPane) {
            for (Node child : stackPane.getChildren()) {
                if (child instanceof BorderPane borderPane) {
                    return borderPane;
                }
            }
        }
        return null;
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

    /**
     * Re-applies a list of updated {@link FooterOption} values to a live footer bar — walks
     * every {@link Label} child, matches its existing option id against {@code updates}, and
     * re-renders the Label with the matching update's enabled / icon / label / tooltip /
     * shortcut state.  Labels whose option id isn't in {@code updates} are left alone.
     *
     * <p>This is the loop hosts used to copy-paste — find the footer bar, iterate Labels,
     * lookup by id, call {@link #applyFooterOption} — collapsed into one engine entry point so
     * the host's footer-refresh code can shrink to "compute new options list, call this."
     * Mirrors {@link #applyFooterPreferences} but takes an explicit options list rather than
     * reading display values off the preferences service.</p>
     *
     * @param footer            footer bar produced by {@link #footerBar(List)} (or any HBox
     *                          whose Label children carry {@link FooterOption} in
     *                          {@code userData}).
     * @param updates           updated options keyed by {@link FooterOption#id()}.
     * @param shortcutDisplay   preference for shortcut visibility (visible / hidden /
     *                          tooltip-only); pass null for {@link #DEFAULT_FOOTER_SHORTCUT_DISPLAY DEFAULT}.
     * @param iconDisplay       preference for icon visibility; pass null for the default.
     */
    public static void applyFooterOptions(HBox footer, List<FooterOption> updates,
            FooterShortcutDisplay shortcutDisplay, FooterIconDisplay iconDisplay) {
        Validation.requireNonNull(footer, "Footer is required.");
        Validation.requireNonNull(updates, "Footer updates list is required.");
        for (Node child : footer.getChildren()) {
            if (child instanceof Label label && label.getUserData() instanceof FooterOption option) {
                FooterOption updated = findById(updates, option.id());
                if (updated != null) {
                    applyFooterOption(label, updated, shortcutDisplay, iconDisplay);
                }
            }
        }
    }

    /**
     * Sets the "active" highlight (yellow pill + bold white text — see
     * {@link #setFooterOptionActive}) on the Label whose option id equals
     * {@code activeOptionId}; clears the highlight on every other Label in the footer.  Pass
     * {@code null} (or a non-matching id) to clear the highlight everywhere.
     *
     * <p>Hosts use this to toggle the auto-skip pill / history-mode pill / any future
     * "this mode is engaged" indicator without standing up their own iteration loop.</p>
     */
    public static void setActiveFooterOption(HBox footer, String activeOptionId) {
        Validation.requireNonNull(footer, "Footer is required.");
        for (Node child : footer.getChildren()) {
            if (child instanceof Label label && label.getUserData() instanceof FooterOption option) {
                setFooterOptionActive(label, activeOptionId != null && activeOptionId.equals(option.id()));
            }
        }
    }

    /**
     * Sets the "active" highlight on the footer Label whose option id equals {@code optionId},
     * <em>without</em> disturbing the active state of any other option (unlike
     * {@link #setActiveFooterOption}, which clears every other option).  Used for independent
     * mode pills such as history-mode that coexist with other highlights.  No-op when the footer
     * or the option isn't found.  {@code footer} may be the footer {@link HBox} itself or any
     * ancestor (the live footer bar is located via {@link #findFooterBar}).
     */
    public static void setFooterOptionActiveById(Node footer, String optionId, boolean active) {
        if (footer == null || optionId == null) {
            return;
        }
        HBox bar = footer instanceof HBox hbox
                && hbox.getStyleClass().contains(SCREEN_FOOTER_BAR_STYLE_CLASS)
                ? hbox : findFooterBar(footer);
        if (bar == null) {
            return;
        }
        for (Node child : bar.getChildren()) {
            if (child instanceof Label label
                    && label.getUserData() instanceof FooterOption option
                    && optionId.equals(option.id())) {
                setFooterOptionActive(label, active);
                return;
            }
        }
    }

    private static FooterOption findById(List<FooterOption> options, String id) {
        if (options == null || id == null) {
            return null;
        }
        for (FooterOption option : options) {
            if (id.equals(option.id())) {
                return option;
            }
        }
        return null;
    }

    public static void applyFooterOption(Label label, FooterOption option, FooterShortcutDisplay shortcutDisplay) {
        applyFooterOption(label, option, shortcutDisplay, DEFAULT_FOOTER_ICON_DISPLAY);
    }

    public static void applyFooterOption(Label label, FooterOption option,
            FooterShortcutDisplay shortcutDisplay, FooterIconDisplay iconDisplay) {
        applyFooterOption(label, option, shortcutDisplay, iconDisplay, null);
    }

    public static void applyFooterOption(Label label, FooterOption option,
            FooterShortcutDisplay shortcutDisplay, FooterIconDisplay iconDisplay, String iconColor) {
        Validation.requireNonNull(label, "Footer label is required.");
        Validation.requireNonNull(option, "Footer option is required.");
        FooterShortcutDisplay checkedShortcut = shortcutDisplay == null
                ? DEFAULT_FOOTER_SHORTCUT_DISPLAY
                : shortcutDisplay;
        FooterIconDisplay checkedIcon = iconDisplay == null
                ? DEFAULT_FOOTER_ICON_DISPLAY
                : iconDisplay;
        String displayText = option.displayText(checkedShortcut, checkedIcon);
        String tooltipText = option.tooltipText(checkedShortcut);
        ImageView graphic = checkedIcon == FooterIconDisplay.TEXT_ONLY ? null : footerGraphic(option, iconColor);
        applyFooterOption(label, option, displayText, tooltipText, graphic);
    }

    private static void applyFooterOption(Label label, FooterOption option, String displayText, String tooltipText) {
        applyFooterOption(label, option, displayText, tooltipText, footerGraphic(option));
    }

    private static void applyFooterOption(Label label, FooterOption option, String displayText, String tooltipText,
            ImageView graphic) {
        Validation.requireNonNull(label, "Footer label is required.");
        Validation.requireNonNull(option, "Footer option is required.");
        label.setUserData(option);
        label.setGraphic(graphic);
        label.setGraphicTextGap(4);
        label.setText(graphic == null ? displayText : footerTextWithoutFallbackIcon(option, displayText));
        applyFooterOptionState(label, option);
        installFooterTooltip(label, tooltipText);
    }

    static ImageView footerGraphic(FooterOption option) {
        return footerGraphic(option, null);
    }

    static ImageView footerGraphic(FooterOption option, String iconColor) {
        if (option.iconResourcePath().isBlank()) {
            return null;
        }
        String cacheKey = iconColor == null || iconColor.isBlank()
                ? option.iconResourcePath()
                : option.iconResourcePath() + ":" + iconColor;
        Image image = FOOTER_ICON_CACHE.computeIfAbsent(cacheKey,
                k -> loadFooterIcon(option.iconResourcePath(), iconColor));
        if (image == null) {
            return null;
        }
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(FOOTER_ICON_SIZE);
        imageView.setFitHeight(FOOTER_ICON_SIZE);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    private static Image loadFooterIcon(String resourcePath, String iconColor) {
        try {
            // Override-aware + extension-flexible: a mod can supply the footer icon as any image
            // type (its own .png/.jpg/.svg) via the asset override root; otherwise the bundled
            // classpath SVG is used.
            URL resource = com.eb.javafx.util.ResourceOverrides.findImage(resourcePath)
                    .orElseGet(() -> resolveResource(resourcePath));
            boolean svg = resource.getPath().toLowerCase(java.util.Locale.ROOT).endsWith(".svg");
            try (InputStream inputStream = resource.openStream()) {
                if (svg) {
                    VectorImage image = VectorImage.fromInputStream(inputStream);
                    // Recolour only applies to the vector source icons.
                    if (iconColor != null && !iconColor.isBlank()) {
                        image = image.replaceFillColor(FOOTER_ICON_SOURCE_COLOR, iconColor);
                    }
                    return image.toRasterImage(FOOTER_ICON_SIZE, FOOTER_ICON_SIZE);
                }
                // Raster override (png/jpg/gif/…): load + scale directly; no recolour.
                Image raster = new Image(inputStream, FOOTER_ICON_SIZE, FOOTER_ICON_SIZE, true, true);
                return raster.isError() ? null : raster;
            }
        } catch (IllegalArgumentException | IOException | IllegalStateException exception) {
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
        double width = Math.max(backgroundSvgRasterMinWidth, image.getWidth());
        double height = Math.max(backgroundSvgRasterMinHeight, image.getHeight());
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
        String requested = Validation.requireNonBlank(resourcePath, "Screen SVG background resource is required.");
        // A per-resource alias repoints this icon/SVG to a replacement path (override-root file
        // or bundled resource) before any lookup.
        String checkedPath = com.eb.javafx.util.ResourceOverrides.effectivePath(requested);
        // Config-driven asset override wins when present (lets a mod replace footer / screen
        // icons & SVGs without a rebuild); otherwise fall back to the bundled classpath copy.
        java.util.Optional<URL> override = com.eb.javafx.util.ResourceOverrides.find(checkedPath);
        if (override.isPresent()) {
            return override.get();
        }
        String absolutePath = checkedPath.startsWith("/") ? checkedPath : "/" + checkedPath;
        URL resource = ScreenShell.class.getResource(absolutePath);
        if (resource == null && !absolutePath.equals(checkedPath)) {
            resource = ScreenShell.class.getResource(checkedPath);
        }
        if (resource == null) {
            String classLoaderPath = checkedPath.startsWith("/") ? checkedPath.substring(1) : checkedPath;
            resource = ScreenShell.class.getClassLoader().getResource(classLoaderPath);
        }
        if (resource == null) {
            throw new IllegalArgumentException("Screen SVG background resource is missing: " + checkedPath);
        }
        return resource;
    }

    private static String configuredBackgroundSource(Path applicationRoot, String backgroundImage) {
        if (backgroundImage == null || backgroundImage.isBlank()) {
            return null;
        }
        try {
            Path candidate = Path.of(backgroundImage);
            if (!candidate.isAbsolute() && applicationRoot != null) {
                Path resolved = applicationRoot.resolve(candidate).normalize();
                if (Files.exists(resolved)) {
                    return resolved.toString();
                }
            }
        } catch (InvalidPathException ignored) {
            // Leave non-path values for URI/classpath resolution.
        }
        return backgroundImage;
    }

    private static Color configuredBackgroundColor(String backgroundColor) {
        if (backgroundColor == null || backgroundColor.isBlank()) {
            return null;
        }
        return parseColor(backgroundColor, "Screen background color is required.");
    }

    private static double configuredBackgroundOpacity(String transparency) {
        if (transparency == null || transparency.isBlank()) {
            return 1.0;
        }
        try {
            double opacity = 1.0 - Double.parseDouble(transparency);
            return opacity < 0.0 || opacity > 1.0 ? 1.0 : opacity;
        } catch (NumberFormatException exception) {
            return 1.0;
        }
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

    /**
     * Marks a footer-option label as <em>active</em> by adding (or removing) the
     * {@link #SCREEN_FOOTER_OPTION_ACTIVE_STYLE_CLASS} style class. Active styling is independent
     * of enabled/disabled — an active label is still clickable; the visual just tells the user
     * the underlying mode is currently engaged (e.g. auto-skip is on).
     *
     * <p>The default stylesheet renders {@code .screen-footer-option-active} as bright white +
     * bold so the active option stands out in the footer pill. Application themes can override
     * by re-defining the same selector.</p>
     */
    public static void setFooterOptionActive(Label label, boolean active) {
        Validation.requireNonNull(label, "Footer label is required.");
        if (active) {
            if (!label.getStyleClass().contains(SCREEN_FOOTER_OPTION_ACTIVE_STYLE_CLASS)) {
                label.getStyleClass().add(SCREEN_FOOTER_OPTION_ACTIVE_STYLE_CLASS);
            }
            if (label.getUserData() instanceof FooterOption option) {
                // White icon (matches the bold-white text from the active CSS rule) on the yellow
                // pill background painted by {@code .screen-footer-option-active}. Distinct from
                // the default gold-on-translucent footer look so the active option pops out.
                ImageView activeIcon = footerGraphic(option, "#ffffff");
                if (activeIcon != null) {
                    label.setGraphic(activeIcon);
                }
            }
        } else {
            label.getStyleClass().remove(SCREEN_FOOTER_OPTION_ACTIVE_STYLE_CLASS);
            if (label.getUserData() instanceof FooterOption option) {
                label.setGraphic(footerGraphic(option));
            }
        }
    }

    /** Returns {@code true} when {@code label} carries the active style class. */
    public static boolean isFooterOptionActive(Label label) {
        Validation.requireNonNull(label, "Footer label is required.");
        return label.getStyleClass().contains(SCREEN_FOOTER_OPTION_ACTIVE_STYLE_CLASS);
    }

    /** Creates a shared tooltip with a shorter delay so hover help appears promptly across reusable UI screens. */
    public static Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(tooltipShowDelay());
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

    private static final class ConfiguredBackgroundLayer extends Region {
        private final ImageView imageView;

        private ConfiguredBackgroundLayer(
                Image image,
                String resolvedImageSource,
                String originalImageReference,
                double opacity,
                Color canvasBackgroundColor) {
            imageView = image == null ? null : new ImageView(image);
            setMinSize(0, 0);
            setMouseTransparent(true);
            setFocusTraversable(false);
            setBackground(canvasBackgroundColor.equals(Color.TRANSPARENT)
                    ? Background.EMPTY
                    : new Background(new BackgroundFill(canvasBackgroundColor, CornerRadii.EMPTY, Insets.EMPTY)));
            if (originalImageReference != null && !originalImageReference.isBlank()) {
                setUserData(originalImageReference);
                if (originalImageReference.toLowerCase().endsWith(".svg")
                        || (resolvedImageSource != null && resolvedImageSource.toLowerCase().endsWith(".svg"))) {
                    getStyleClass().add(SCREEN_BACKGROUND_SVG_STYLE_CLASS);
                }
            }
            if (imageView != null) {
                imageView.setMouseTransparent(true);
                imageView.setFocusTraversable(false);
                imageView.setPreserveRatio(false);
                imageView.setSmooth(true);
                imageView.setOpacity(opacity);
                getChildren().add(imageView);
            }
        }

        @Override
        protected void layoutChildren() {
            if (imageView != null) {
                imageView.setFitWidth(getWidth());
                imageView.setFitHeight(getHeight());
                imageView.resizeRelocate(0, 0, getWidth(), getHeight());
            }
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

        public String displayText(FooterShortcutDisplay shortcutDisplay, FooterIconDisplay iconDisplay) {
            if (iconDisplay == FooterIconDisplay.ICONS_ONLY) {
                return icon;
            }
            if (iconDisplay == FooterIconDisplay.TEXT_ONLY) {
                FooterShortcutDisplay checkedDisplay = shortcutDisplay == null
                        ? FooterShortcutDisplay.TOOLTIP_ONLY
                        : shortcutDisplay;
                if (checkedDisplay == FooterShortcutDisplay.DISPLAY) {
                    return label + " (" + shortcut + ")";
                }
                return label;
            }
            return displayText(shortcutDisplay);
        }

        public String tooltipText(FooterShortcutDisplay shortcutDisplay) {
            FooterShortcutDisplay checkedDisplay = shortcutDisplay == null
                    ? DEFAULT_FOOTER_SHORTCUT_DISPLAY
                    : shortcutDisplay;
            if (checkedDisplay == FooterShortcutDisplay.HIDE) {
                return tooltip;
            }
            String shortcutText = SystemCodeTables.defaultMessage("footer.keyboard-shortcut", Map.of("shortcut", shortcut));
            if (tooltip == null || tooltip.isBlank()) {
                return shortcutText;
            }
            return tooltip.endsWith(".") ? tooltip + " " + shortcutText : tooltip + ". " + shortcutText;
        }

        public String accessibleText() {
            return SystemCodeTables.defaultMessage("footer.accessible-text", Map.of(
                    "label", label,
                    "shortcut", shortcut));
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
