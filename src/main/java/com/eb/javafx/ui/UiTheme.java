package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.prefs.PreferencesService.ThemeFamily;
import com.eb.javafx.prefs.PreferencesService.ThemeVariant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JavaFX theme tokens and generated stylesheet lookup for reusable screens.
 */
public final class UiTheme {
    private static final String STYLESHEET_TEMPLATE = """
            .root {
                -fx-background-color: %s;
                -fx-font-family: "%s";
                -fx-accent: %s;
                -fx-default-button: %s;
                -fx-focus-color: %s;
                -fx-faint-focus-color: transparent;
                -fx-selection-bar: %s;
                -fx-selection-bar-text: %s;
                -fx-text-background-color: %s;
                -fx-control-inner-background: %s;
                -fx-control-inner-background-alt: %s;
                -fx-cell-hover-color: %s;
                -fx-mark-color: %s;
            }

            .screen-root {
                -fx-background-color: linear-gradient(to bottom, %s, %s);
            }

            .screen-title {
                -fx-font-size: 34px;
                -fx-font-weight: bold;
                -fx-text-fill: %s;
            }

            .screen-subtitle {
                -fx-text-fill: %s;
            }

            .screen-panel {
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-radius: 6px;
                -fx-background-radius: 6px;
            }

            .screen-footer-bar {
                -fx-alignment: center;
                -fx-background-color: %s;
                -fx-border-color: transparent;
                -fx-border-width: 0;
                -fx-border-radius: 999px;
                -fx-background-radius: 999px;
                -fx-padding: 1px;
            }

            .screen-footer-option {
                -fx-text-fill: %s;
                -fx-font-size: 9px;
            }

            .screen-footer-option-disabled {
                -fx-opacity: 0.45;
            }

            .conversation-history-rows {
                -fx-hgap: 10px;
                -fx-vgap: 6px;
            }

            .conversation-history-speaker {
                -fx-alignment: top-right;
            }

            .conversation-history-message {
                -fx-alignment: top-left;
            }

            .screen-footer-compact {
                -fx-padding: 1px;
            }

            .scene-status-panel,
            .scene-dialogue-panel,
            .scene-choices-panel,
            .scene-effects-panel {
                -fx-border-color: %s;
            }

            .scene-choice-button {
                -fx-alignment: center-left;
            }

            .scene-choice-state,
            .scene-effect-preview {
                -fx-text-fill: %s;
            }

            .screen-text-highlight,
            .layout-text-highlight {
                -fx-text-fill: %s;
            }

            .screen-text-highlight:hover,
            .layout-text-highlight:hover {
                -fx-text-fill: %s;
            }

            .screen-value,
            .layout-value {
                -fx-text-fill: %s;
            }

            .layout-content,
            .layout-titled-panel,
            .layout-main-content,
            .layout-hud-overlay,
            .layout-dialogue,
            .layout-menu,
            .layout-form {
                -fx-spacing: 12px;
            }

            .layout-subtitle,
            .layout-footer,
            .layout-section-row {
                -fx-text-fill: %s;
            }

            .layout-section-title {
                -fx-font-weight: bold;
                -fx-font-size: 20px;
                -fx-text-fill: %s;
            }

            .layout-two-column,
            .layout-sidebar-content {
                -fx-spacing: 12px;
            }

            .layout-column,
            .layout-sidebar,
            .layout-card,
            .layout-section {
                -fx-spacing: 8px;
            }

            .layout-sidebar,
            .layout-card,
            .layout-hud-overlay,
            .layout-dialogue {
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-border-radius: 6px;
                -fx-background-radius: 6px;
                -fx-padding: 12px;
            }

            .layout-action-row {
                -fx-spacing: 8px;
            }

            .layout-primary-action {
                -fx-border-color: %s;
            }

            .layout-secondary-action,
            .layout-sidebar-entry {
                -fx-border-color: %s;
            }

            .label {
                -fx-text-fill: %s;
                -fx-font-size: 18px;
            }

            .screen-text {
                -fx-text-fill: %s;
            }

            .button {
                -fx-font-size: 20px;
                -fx-font-weight: bold;
                -fx-padding: 8px 18px;
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-border-color: %s;
                -fx-border-width: 1px;
                -fx-background-insets: 0;
                -fx-border-insets: 0;
            }

            .button:hover {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-border-color: %s;
            }

            .button:pressed {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-border-color: %s;
            }

            .combo-box-base,
            .combo-box-base:editable > .text-field,
            .combo-box > .list-cell {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-border-color: %s;
                -fx-control-inner-background: %s;
                -fx-prompt-text-fill: %s;
            }

            .combo-box-base:hover,
            .combo-box-base:showing {
                -fx-background-color: %s;
                -fx-text-fill: %s;
                -fx-border-color: %s;
            }

            .combo-box-base .arrow {
                -fx-background-color: %s;
            }

            .combo-box-popup .list-view {
                -fx-background-color: %s;
                -fx-border-color: %s;
                -fx-control-inner-background: %s;
            }

            .combo-box-popup .list-cell {
                -fx-background-color: %s;
                -fx-text-fill: %s;
            }

            .combo-box-popup .list-cell:hover {
                -fx-background-color: %s;
                -fx-text-fill: %s;
            }

            .combo-box-popup .list-cell:selected,
            .combo-box-popup .list-cell:hover:selected {
                -fx-background-color: %s;
                -fx-text-fill: %s;
            }

            .svg-button-artwork-text {
                -fx-fill: %s;
                -fx-font-size: 20px;
                -fx-font-weight: bold;
            }

            .svg-button:hover .svg-button-artwork-text,
            .svg-button:pressed .svg-button-artwork-text {
                -fx-fill: %s;
            }
            """;

    private String fontFamily;
    private String accentColor;
    private String textColor;
    private String panelBackground;
    private String hoverBackground;
    private double fontScale;
    private boolean highContrast;
    private boolean reducedMotion;
    private String stylesheet;
    private String stylesheetContent;
    private RoleColors roleColors;
    private DisplayDefaults themedDisplayDefaults;

    /**
     * Loads the active theme from preferences and writes a generated stylesheet for the current palette.
     *
     * @param preferencesService loaded preferences supplying font, theme, and accessibility tokens
     */
    public void initialize(PreferencesService preferencesService) {
        fontFamily = preferencesService.fontFamily();
        fontScale = preferencesService.fontScale();
        highContrast = preferencesService.highContrast();
        reducedMotion = preferencesService.reducedMotion();

        ThemePalette palette = highContrast
                ? ThemePalette.highContrast()
                : ThemePalette.forSelection(preferencesService.themeFamily(), preferencesService.themeVariant());
        accentColor = palette.accentColor();
        textColor = palette.labelText();
        panelBackground = palette.screenPanelBackground();
        hoverBackground = palette.buttonHoverBackground();
        stylesheetContent = palette.toStylesheet(fontFamily);
        stylesheet = writeStylesheet(stylesheetContent);
        ButtonVisuals.configureArtworkGradient(
                palette.buttonGradientStart(),
                palette.buttonGradientMid(),
                palette.buttonGradientEnd());
        roleColors = palette.roleColors();
        themedDisplayDefaults = DisplayDefaults.defaults().withRoleColors(roleColors);
        DisplayDefaults.installActive(themedDisplayDefaults);
    }

    /**
     * Returns the theme-resolved role colors for the active palette — used to drive
     * per-item-type color/background for all nine {@link ScreenDesignItemType} values.
     */
    public RoleColors roleColors() {
        return roleColors;
    }

    /**
     * Returns the bundled display defaults with the active palette's role colors applied.
     */
    public DisplayDefaults themedDisplayDefaults() {
        return themedDisplayDefaults;
    }

    public String fontFamily() {
        return fontFamily;
    }

    public String accentColor() {
        return accentColor;
    }

    public String textColor() {
        return textColor;
    }

    public String panelBackground() {
        return panelBackground;
    }

    public String hoverBackground() {
        return hoverBackground;
    }

    public double fontScale() {
        return fontScale;
    }

    public boolean highContrast() {
        return highContrast;
    }

    public boolean reducedMotion() {
        return reducedMotion;
    }

    /**
     * Returns the generated stylesheet used by reusable screens.
     *
     * @throws StartupFailureException when the stylesheet cannot be materialized
     */
    public String stylesheet() {
        if (stylesheet == null || stylesheet.isBlank()) {
            throw new StartupFailureException(StartupFailureCategory.MISSING_ASSET, "Missing JavaFX stylesheet.");
        }
        return stylesheet;
    }

    String stylesheetContent() {
        return stylesheetContent;
    }

    private static String writeStylesheet(String css) {
        try {
            Path stylesheetPath = Files.createTempFile("novlfx-theme-", ".css");
            Files.writeString(stylesheetPath, css, StandardCharsets.UTF_8);
            stylesheetPath.toFile().deleteOnExit();
            return stylesheetPath.toUri().toString();
        } catch (IOException exception) {
            throw new StartupFailureException(StartupFailureCategory.MISSING_ASSET, "Unable to create JavaFX stylesheet.");
        }
    }

    private record ThemePalette(
            String rootBackground,
            String screenGradientStart,
            String screenGradientEnd,
            String accentColor,
            String screenPanelBackground,
            String screenPanelBorder,
            String footerBackground,
            String footerText,
            String sectionBorder,
            String sectionText,
            String textHighlight,
            String textHighlightHover,
            String valueText,
            String layoutPanelBackground,
            String labelText,
            String buttonBackground,
            String buttonText,
            String buttonBorder,
            String buttonHoverBackground,
            String buttonHoverText,
            String buttonHoverBorder,
            String buttonPressedBackground,
            String buttonPressedText,
            String buttonPressedBorder,
            String svgButtonText,
            String svgButtonHoverText,
            String buttonGradientStart,
            String buttonGradientMid,
            String buttonGradientEnd,
            String fieldRoleColor,
            String fieldRoleBackground) {
        private static ThemePalette forSelection(ThemeFamily family, ThemeVariant variant) {
            return switch (family) {
                case OCEAN -> variant == ThemeVariant.DARK
                        ? new ThemePalette(
                        "#101828", "#101828", "#0a1426", "#66c1e0",
                        "rgba(10, 20, 38, 0.85)", "#0099cc",
                        "rgba(10, 20, 38, 0.50)", "#cbd5e1",
                        "#143869", "#cbd5e1", "#66c1e0", "#ffffff", "#bfd3ec",
                        "rgba(10, 20, 38, 0.65)", "#ffffff",
                        "#0a1426", "#bfd3ec", "#143869",
                        "#143869", "#ffffff", "#0099cc",
                        "#0099cc", "#ffffff", "#66c1e0",
                        "#bfd3ec", "#ffffff",
                        "#9dccff", "#0f4f9f", "#052f6f",
                        "#ffffff", "#0a1426")
                        : new ThemePalette(
                        "#f4fbff", "#f4fbff", "#dcefff", "#3c7ea3",
                        "rgba(229, 243, 251, 0.92)", "#7cb8d6",
                        "rgba(229, 243, 251, 0.72)", "#456273",
                        "#97c7dd", "#456273", "#3c7ea3", "#234052", "#36576a",
                        "rgba(220, 239, 255, 0.84)", "#000000",
                        "#e7f4fb", "#36576a", "#97c7dd",
                        "#cfe8f6", "#234052", "#5ba6c8",
                        "#8cc9e3", "#1f3e50", "#cae8f6",
                        "#36576a", "#1f3e50",
                        "#eef8fd", "#9fd3ea", "#5a9ec0",
                        "#1f3e50", "#fbfeff");
                case FOREST -> variant == ThemeVariant.DARK
                        ? new ThemePalette(
                        "#0f1a13", "#0f1a13", "#0a140f", "#8dd7a8",
                        "rgba(11, 25, 17, 0.88)", "#57b27e",
                        "rgba(11, 25, 17, 0.52)", "#cfe7d7",
                        "#2d6a45", "#cfe7d7", "#8dd7a8", "#ffffff", "#d2eedc",
                        "rgba(16, 34, 22, 0.68)", "#ffffff",
                        "#122117", "#d2eedc", "#2d6a45",
                        "#2d6a45", "#ffffff", "#57b27e",
                        "#57b27e", "#082212", "#9fe0ba",
                        "#d2eedc", "#ffffff",
                        "#b7ebc8", "#40916c", "#1b4332",
                        "#ffffff", "#0a140f")
                        : new ThemePalette(
                        "#f5fcf7", "#f5fcf7", "#e1f3e5", "#4f8b66",
                        "rgba(231, 246, 235, 0.92)", "#90c7a4",
                        "rgba(231, 246, 235, 0.72)", "#446152",
                        "#a2cfb1", "#446152", "#4f8b66", "#284233", "#365343",
                        "rgba(225, 243, 229, 0.84)", "#000000",
                        "#e9f6ed", "#365343", "#9ecfb0",
                        "#d4ecdb", "#284233", "#6aad87",
                        "#9ed3b4", "#1d3427", "#d7f0df",
                        "#365343", "#1d3427",
                        "#eef8f0", "#abd7bb", "#6aa07f",
                        "#1d3427", "#f5fdf8");
                case SUNSET -> variant == ThemeVariant.DARK
                        ? new ThemePalette(
                        "#24131a", "#24131a", "#160d12", "#f3a6a0",
                        "rgba(36, 19, 26, 0.86)", "#e07a6e",
                        "rgba(36, 19, 26, 0.52)", "#f0d3d0",
                        "#8f3f4c", "#f0d3d0", "#f3a6a0", "#ffffff", "#f8d8d3",
                        "rgba(45, 24, 31, 0.68)", "#ffffff",
                        "#2a151d", "#f8d8d3", "#8f3f4c",
                        "#8f3f4c", "#ffffff", "#e07a6e",
                        "#e07a6e", "#2a151d", "#f7c1b8",
                        "#f8d8d3", "#ffffff",
                        "#f7c8c2", "#d16b62", "#7f3b45",
                        "#ffffff", "#160d12")
                        : new ThemePalette(
                        "#fff6f3", "#fff6f3", "#ffe6df", "#b86d61",
                        "rgba(255, 235, 228, 0.92)", "#e6ae9f",
                        "rgba(255, 235, 228, 0.72)", "#72514c",
                        "#d7a195", "#72514c", "#b86d61", "#593e3a", "#6f4d47",
                        "rgba(255, 230, 223, 0.84)", "#000000",
                        "#fff0ea", "#6f4d47", "#daa296",
                        "#ffdcd0", "#593e3a", "#d99081",
                        "#efb0a2", "#4c322c", "#f9d7cf",
                        "#6f4d47", "#4c322c",
                        "#fff5f1", "#f0b8ab", "#d88978",
                        "#4c322c", "#fff8f4");
                case VIOLET -> variant == ThemeVariant.DARK
                        ? new ThemePalette(
                        "#191427", "#191427", "#110d1b", "#c7b5ff",
                        "rgba(25, 20, 39, 0.86)", "#9b7be6",
                        "rgba(25, 20, 39, 0.52)", "#e1d9fb",
                        "#5b4b8a", "#e1d9fb", "#c7b5ff", "#ffffff", "#ddd3ff",
                        "rgba(32, 26, 49, 0.68)", "#ffffff",
                        "#1c1730", "#ddd3ff", "#5b4b8a",
                        "#5b4b8a", "#ffffff", "#9b7be6",
                        "#9b7be6", "#1c1730", "#d7c9ff",
                        "#ddd3ff", "#ffffff",
                        "#e3d8ff", "#8f6ad8", "#4c3b78",
                        "#ffffff", "#110d1b")
                        : new ThemePalette(
                        "#faf6ff", "#faf6ff", "#ede3ff", "#775fc1",
                        "rgba(240, 232, 255, 0.92)", "#b9a2ea",
                        "rgba(240, 232, 255, 0.72)", "#605375",
                        "#c8b8ee", "#605375", "#775fc1", "#43395a", "#5b5076",
                        "rgba(237, 227, 255, 0.84)", "#000000",
                        "#f3ecff", "#5b5076", "#baa7e9",
                        "#e6dafd", "#43395a", "#9f81dd",
                        "#c5b0f2", "#352c49", "#e9e0ff",
                        "#5b5076", "#352c49",
                        "#faf5ff", "#cebdf3", "#a287dd",
                        "#352c49", "#fcf9ff");
            };
        }

        private static ThemePalette highContrast() {
            return new ThemePalette(
                    "#000000", "#000000", "#000000", "#ffff66",
                    "rgba(0, 0, 0, 0.96)", "#ffff66",
                    "rgba(0, 0, 0, 0.92)", "#ffff66",
                    "#ffff66", "#ffff66", "#ffff66", "#ffffff", "#ffff66",
                    "rgba(0, 0, 0, 0.94)", "#ffffff",
                    "#000000", "#ffff66", "#ffff66",
                    "#333300", "#ffffff", "#ffff66",
                    "#ffff66", "#000000", "#ffffff",
                    "#ffff66", "#ffffff",
                    "#fff8a6", "#c7bf2d", "#5c5400",
                    "#ffff66", "#000000");
        }

        private RoleColors roleColors() {
            return new RoleColors(
                    labelText, "transparent",
                    accentColor, "transparent",
                    accentColor, "transparent",
                    fieldRoleColor, fieldRoleBackground,
                    buttonText, buttonBackground,
                    accentColor,
                    labelText, screenPanelBackground, sectionBorder);
        }

        private String toStylesheet(String fontFamily) {
            return STYLESHEET_TEMPLATE.formatted(
                    rootBackground,
                    cssQuoted(fontFamily == null || fontFamily.isBlank() ? "System" : fontFamily),
                    accentColor,
                    accentColor,
                    accentColor,
                    accentColor,
                    buttonHoverText,
                    labelText,
                    screenPanelBackground,
                    layoutPanelBackground,
                    buttonHoverBackground,
                    accentColor,
                    screenGradientStart,
                    screenGradientEnd,
                    accentColor,
                    sectionText,
                    screenPanelBackground,
                    screenPanelBorder,
                    footerBackground,
                    footerText,
                    sectionBorder,
                    sectionText,
                    textHighlight,
                    textHighlightHover,
                    valueText,
                    sectionText,
                    accentColor,
                    layoutPanelBackground,
                    sectionBorder,
                    screenPanelBorder,
                    sectionBorder,
                    labelText,
                    labelText,
                    buttonBackground,
                    buttonText,
                    buttonBorder,
                    buttonHoverBackground,
                    buttonHoverText,
                    buttonHoverBorder,
                    buttonPressedBackground,
                    buttonPressedText,
                    buttonPressedBorder,
                    buttonBackground,
                    buttonText,
                    buttonBorder,
                    screenPanelBackground,
                    sectionText,
                    buttonHoverBackground,
                    buttonHoverText,
                    buttonHoverBorder,
                    sectionText,
                    screenPanelBackground,
                    screenPanelBorder,
                    screenPanelBackground,
                    screenPanelBackground,
                    labelText,
                    buttonHoverBackground,
                    buttonHoverText,
                    buttonPressedBackground,
                    buttonPressedText,
                    svgButtonText,
                    svgButtonHoverText);
        }

        private static String cssQuoted(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
