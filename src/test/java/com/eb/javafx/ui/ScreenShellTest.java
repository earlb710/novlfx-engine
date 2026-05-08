package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.localization.LocalizationService;
import com.eb.javafx.localization.LocalizedTextBundle;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.prefs.PreferencesService.FooterShortcutDisplay;
import com.eb.javafx.state.GameState;
import com.eb.javafx.util.VectorImage;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.image.ImageView;
import javafx.scene.shape.SVGPath;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ScreenShellTest {
    @Test
    void backgroundImageStretchesToScreenBounds() {
        Image image = new WritableImage(16, 9);

        BackgroundImage backgroundImage = ScreenShell.backgroundImage(image, ScreenBackgroundFit.STRETCH);
        BackgroundSize size = backgroundImage.getSize();

        assertSame(image, backgroundImage.getImage());
        assertEquals(BackgroundRepeat.NO_REPEAT, backgroundImage.getRepeatX());
        assertEquals(BackgroundRepeat.NO_REPEAT, backgroundImage.getRepeatY());
        assertEquals(BackgroundPosition.CENTER, backgroundImage.getPosition());
        assertEquals(100, size.getWidth());
        assertEquals(100, size.getHeight());
        assertTrue(size.isWidthAsPercentage());
        assertTrue(size.isHeightAsPercentage());
        assertFalse(size.isContain());
        assertFalse(size.isCover());
    }

    @Test
    void backgroundImageCropsFromCenterWhenCoveringScreen() {
        Image image = new WritableImage(16, 9);

        BackgroundImage backgroundImage = ScreenShell.backgroundImage(image, ScreenBackgroundFit.CROP_CENTER);
        BackgroundSize size = backgroundImage.getSize();

        assertSame(image, backgroundImage.getImage());
        assertEquals(BackgroundRepeat.NO_REPEAT, backgroundImage.getRepeatX());
        assertEquals(BackgroundRepeat.NO_REPEAT, backgroundImage.getRepeatY());
        assertEquals(BackgroundPosition.CENTER, backgroundImage.getPosition());
        assertFalse(size.isWidthAsPercentage());
        assertFalse(size.isHeightAsPercentage());
        assertFalse(size.isContain());
        assertTrue(size.isCover());
    }

    @Test
    void setBackgroundImageAppliesConfiguredBackground() {
        BorderPane screen = new BorderPane();
        Image image = new WritableImage(16, 9);

        ScreenShell.setBackgroundImage(screen, image, ScreenBackgroundFit.CROP_CENTER);

        assertEquals(1, screen.getBackground().getImages().size());
        assertSame(image, screen.getBackground().getImages().get(0).getImage());
        assertTrue(screen.getBackground().getImages().get(0).getSize().isCover());
    }

    @Test
    void backgroundHelpersValidateRequiredArguments() {
        Image image = new WritableImage(16, 9);

        assertThrows(IllegalArgumentException.class, () ->
                ScreenShell.backgroundImage(null, ScreenBackgroundFit.STRETCH));
        assertThrows(IllegalArgumentException.class, () ->
                ScreenShell.backgroundImage(image, null));
        assertThrows(IllegalArgumentException.class, () ->
                ScreenShell.setBackgroundImage(null, image, ScreenBackgroundFit.STRETCH));
    }

    @Test
    void backgroundSvgWrapsWholeScreenBehindContentWithoutBorderOrMouseInput() {
        BorderPane screen = new BorderPane();

        StackPane root = ScreenShell.withBackgroundSvg(screen, ButtonVisuals.BUTTON_LONG_ARTWORK_RESOURCE);
        Region background = (Region) root.getChildren().get(0);

        assertSame(screen, root.getChildren().get(1));
        assertTrue(background.getStyleClass().contains(ScreenShell.SCREEN_BACKGROUND_SVG_STYLE_CLASS));
        assertTrue(background.isMouseTransparent());
        assertEquals(Border.EMPTY, background.getBorder());
        assertEquals(0, background.getPadding().getTop());
        assertTrue(background.prefWidthProperty().isBound());
        assertTrue(background.prefHeightProperty().isBound());

        root.resize(640, 360);

        assertEquals(640, background.getPrefWidth());
        assertEquals(360, background.getPrefHeight());
    }

    @Test
    void backgroundSvgHelpersValidateRequiredResource() {
        assertThrows(IllegalArgumentException.class, () -> ScreenShell.backgroundSvg(null));
        assertThrows(IllegalArgumentException.class, () -> ScreenShell.backgroundSvg(" "));
        assertThrows(IllegalArgumentException.class, () -> ScreenShell.backgroundSvg("/missing-background.svg"));
        assertThrows(IllegalArgumentException.class, () -> ScreenShell.withBackgroundSvg(null,
                ButtonVisuals.BUTTON_LONG_ARTWORK_RESOURCE));
    }

    @Test
    void styledPanelAppliesSharedAndSceneSpecificHooks() {
        VBox panel = ScreenShell.styledPanel(ScreenShell.SCENE_DIALOGUE_PANEL_STYLE_CLASS, new BorderPane());

        assertTrue(panel.getStyleClass().contains(ScreenShell.SCREEN_PANEL_STYLE_CLASS));
        assertTrue(panel.getStyleClass().contains(ScreenShell.SCENE_DIALOGUE_PANEL_STYLE_CLASS));
        assertEquals(ScreenShell.BODY_SPACING, panel.getSpacing());
        assertEquals(ScreenShell.PANEL_INSETS, panel.getPadding());
    }

    @Test
    void footerCanBePinnedAsBottomRegion() {
        BorderPane screen = new BorderPane();
        HBox footer = new HBox();
        footer.setManaged(false);
        screen.setBottom(footer);

        ScreenShell.pinFooterToBottom(screen);

        assertTrue(footer.isManaged());
        assertEquals(Pos.BOTTOM_CENTER, BorderPane.getAlignment(footer));
        assertEquals(ScreenShell.FOOTER_INSETS, BorderPane.getMargin(footer));
    }

    @Test
    void defaultStylesMakeFooterCompactTransparentAndBorderless() throws Exception {
        String css = Files.readString(Path.of("src/main/resources/com/eb/javafx/ui/default.css"));

        assertTrue(css.contains("-fx-background-color: rgba(10, 20, 38, 0.50);"));
        assertTrue(css.contains("-fx-border-width: 0;"));
        assertTrue(css.contains("-fx-padding: 1px;"));
        assertTrue(css.contains("-fx-font-size: 9px;"));
    }

    @Test
    void footerBarDefaultsToDimOpacityAndRestoresOnHover() {
        HBox footer = new HBox();

        ScreenShell.configureDefaultFooterPresentation(footer);

        assertEquals(14.0, footer.getSpacing());
        assertEquals(0.5, footer.getOpacity());

        footer.getOnMouseEntered().handle(null);

        assertEquals(1.0, footer.getOpacity());

        footer.getOnMouseExited().handle(null);

        assertEquals(0.5, footer.getOpacity());
    }

    @Test
    void defaultStylesMakeButtonsBoldAndLabelSized() throws Exception {
        String css = Files.readString(Path.of("src/main/resources/com/eb/javafx/ui/default.css"));

        assertTrue(css.contains("-fx-font-size: 20px;"));
        assertTrue(css.contains("-fx-font-weight: bold;"));
        assertTrue(css.contains("-fx-padding: 8px 18px;"));
        assertTrue(css.contains("-fx-text-fill: #bfd3ec;"));
        assertTrue(css.contains(".button:hover"));
        assertTrue(css.contains(".svg-button:hover .svg-button-artwork-text"));
        assertTrue(css.contains("-fx-fill: #ffffff;"));
    }

    @Test
    void buttonVisualsLoadShapeFromLongButtonPillSvg() throws Exception {
        String svg = Files.readString(Path.of("src/main/resources/com/eb/javafx/images/svg/button-pill-long.svg"));

        assertTrue(svg.contains("button-shape-long"));
        assertTrue(svg.contains("width=\"400\""));
        assertTrue(svg.contains("height=\"150\""));
        assertTrue(ButtonVisuals.buttonShapePath().startsWith("M "));
        SVGPath shape = ButtonVisuals.createShape();
        assertNotNull(shape);
        assertEquals(ButtonVisuals.buttonShapePath(), shape.getContent());
    }

    @Test
    void footerOptionTextsExposeRequestedIconShortcuts() {
        ScreenShell.FooterOption back = ScreenShell.defaultFooterOptions().get(0);

        assertEquals("‹ Back", back.displayText(FooterShortcutDisplay.TOOLTIP_ONLY));
        assertEquals("‹ Back", back.displayText(FooterShortcutDisplay.HIDE));
        assertEquals("Return to the previous screen. Keyboard shortcut: Backspace.",
                back.tooltipText(FooterShortcutDisplay.TOOLTIP_ONLY));
        assertEquals("Return to the previous screen.", back.tooltipText(FooterShortcutDisplay.HIDE));
        assertEquals(List.of(
                "‹ Back (Backspace)",
                "◷ History (Ctrl+H)",
                "⇥ Skip mode (Tab)",
                "⇩ Load (Ctrl+L)",
                "▣ Save (Ctrl+S)",
                "⚡ Quick save (Ctrl+Q)",
                "⚙ Preferences (Ctrl+P)",
                "› Forward (Space)"), ScreenShell.defaultFooterOptions().stream()
                .map(ScreenShell.FooterOption::displayText)
                .toList());
        assertEquals(List.of(
                "Back - Keyboard shortcut: Backspace",
                "History - Keyboard shortcut: Ctrl+H",
                "Skip mode - Keyboard shortcut: Tab",
                "Load - Keyboard shortcut: Ctrl+L",
                "Save - Keyboard shortcut: Ctrl+S",
                "Quick save - Keyboard shortcut: Ctrl+Q",
                "Preferences - Keyboard shortcut: Ctrl+P",
                "Forward - Keyboard shortcut: Space"), ScreenShell.defaultFooterOptions().stream()
                .map(ScreenShell.FooterOption::accessibleText)
                .toList());
        assertEquals("screen-footer-bar", ScreenShell.SCREEN_FOOTER_BAR_STYLE_CLASS);
        assertEquals("screen-footer-option", ScreenShell.SCREEN_FOOTER_OPTION_STYLE_CLASS);
    }

    @Test
    void footerOptionsExposeDefaultIconResources() throws Exception {
        assertEquals(List.of(
                "com/eb/javafx/images/icons/footer-back.svg",
                "com/eb/javafx/images/icons/footer-history.svg",
                "com/eb/javafx/images/icons/footer-skip-mode.svg",
                "com/eb/javafx/images/icons/footer-load.svg",
                "com/eb/javafx/images/icons/footer-save.svg",
                "com/eb/javafx/images/icons/footer-quick-save.svg",
                "com/eb/javafx/images/icons/footer-preferences.svg",
                "com/eb/javafx/images/icons/footer-forward.svg"), ScreenShell.defaultFooterOptions().stream()
                .map(ScreenShell.FooterOption::iconResourcePath)
                .toList());

        for (ScreenShell.FooterOption option : ScreenShell.defaultFooterOptions()) {
            Path resourcePath = Path.of("src/main/resources").resolve(option.iconResourcePath());

            assertTrue(Files.isRegularFile(resourcePath), option.id() + " icon resource should exist.");
            assertTrue(VectorImage.isSvgPath(resourcePath), option.id() + " icon resource should be valid SVG.");
            assertTrue(Files.readString(resourcePath).contains("#ffcc00"), option.id() + " icon should use the yellow footer color.");
        }
        assertTrue(VectorImage.isSvgPath(Path.of(
                "src/main/resources/com/eb/javafx/images/icons/icons-10x10.svg")));
    }

    @Test
    void footerOptionsRenderSlightlyLargerSvgIcons() {
        ImageView graphic = ScreenShell.footerGraphic(ScreenShell.defaultFooterOptions().get(0));

        assertNotNull(graphic);
        assertEquals(14.0, graphic.getFitWidth());
        assertEquals(14.0, graphic.getFitHeight());
        assertTrue(graphic.isPreserveRatio());
    }

    @Test
    void footerTextOmitsFallbackGlyphWhenSvgGraphicIsUsed() {
        ScreenShell.FooterOption back = ScreenShell.defaultFooterOptions().get(0);

        assertEquals("Back", ScreenShell.footerTextWithoutFallbackIcon(back, "‹ Back"));
        assertEquals("", ScreenShell.footerTextWithoutFallbackIcon(back, "‹"));
        assertEquals("Back (Backspace)", ScreenShell.footerTextWithoutFallbackIcon(back, "‹ Back (Backspace)"));
    }

    @Test
    void footerOptionsCanBeCustomizedWithoutChangingDefaults() {
        List<ScreenShell.FooterOption> customized = ScreenShell.changeFooterTooltip(
                ScreenShell.changeFooterIconResourcePath(
                        ScreenShell.changeFooterIcon(
                        ScreenShell.changeFooterLabel(
                                ScreenShell.changeFooterShortcut(
                                        ScreenShell.defaultFooterOptions(),
                                        "quick-save",
                                        "Ctrl+Shift+S"),
                                "quick-save",
                                 "Quicksave"),
                         "quick-save",
                         "💾"),
                        "quick-save",
                        "com/example/quicksave.svg"),
                "quick-save",
                "Immediately write a quick save.");

        ScreenShell.FooterOption quickSave = customized.stream()
                .filter(option -> option.id().equals("quick-save"))
                .findFirst()
                .orElseThrow();

        assertEquals("💾 Quicksave (Ctrl+Shift+S)", quickSave.displayText());
        assertEquals("com/example/quicksave.svg", quickSave.iconResourcePath());
        assertEquals("Quicksave - Keyboard shortcut: Ctrl+Shift+S", quickSave.accessibleText());
        assertEquals("Immediately write a quick save.", quickSave.tooltip());
        assertEquals("⚡ Quick save (Ctrl+Q)", ScreenShell.defaultFooterOptions().get(5).displayText());
    }

    @Test
    void footerVisibilityAndTransparencyHelpersUpdateFooterNodes() {
        HBox footer = new HBox();
        BorderPane screen = new BorderPane();
        screen.setBottom(footer);

        ScreenShell.setFooterVisible(screen, false);

        assertFalse(footer.isVisible());
        assertFalse(footer.isManaged());

        ScreenShell.setFooterVisible(footer, true);
        ScreenShell.setFooterTransparency(screen, 0.35);

        assertTrue(footer.isVisible());
        assertTrue(footer.isManaged());
        assertEquals(0.65, footer.getOpacity());
    }

    @Test
    void footerVisualHelpersUpdateBackgroundAndBorder() {
        HBox footer = new HBox();

        ScreenShell.setFooterFontSize(footer, 9.0);
        ScreenShell.setFooterTextColor(footer, "#ff0000");
        ScreenShell.setFooterBackgroundColor(footer, "#112233");
        ScreenShell.setFooterBackgroundTransparency(footer, 0.25);
        ScreenShell.setFooterBorderColor(footer, "#445566");
        ScreenShell.setFooterBorderSize(footer, 2.0);
        ScreenShell.setFooterBorderStyle(footer, "dashed");

        Color backgroundColor = (Color) footer.getBackground()
                .getFills()
                .get(0)
                .getFill();
        assertEquals(0.75, backgroundColor.getOpacity());
        assertEquals(Color.web("#112233").getBlue(), backgroundColor.getBlue());
        assertEquals(BorderStrokeStyle.DASHED, footer.getBorder().getStrokes().get(0).getTopStyle());
        assertEquals(2.0, footer.getBorder().getStrokes().get(0).getWidths().getTop());

        ScreenShell.setFooterBorderStyle(footer, "none");

        assertEquals(Border.EMPTY, footer.getBorder());
    }

    @Test
    void footerOptionsCanBeDisabledFromGameState() {
        List<ScreenShell.FooterOption> withoutHistory = ScreenShell.footerOptionsForGameState(new GameState("start"));

        assertFalse(withoutHistory.stream()
                .filter(option -> option.id().equals("history"))
                .findFirst()
                .orElseThrow()
                .enabled());

        GameState gameState = new GameState("start");
        gameState.conversationHistory().beginDialog("intro", new GameDateTime(1, "morning"));

        List<ScreenShell.FooterOption> withHistory = ScreenShell.footerOptionsForGameState(gameState);

        assertTrue(withHistory.stream()
                .filter(option -> option.id().equals("history"))
                .findFirst()
                .orElseThrow()
                .enabled());
    }

    @Test
    void footerCanSwitchToCompactOrIconOnlyLayout() {
        HBox footer = new HBox();
        ScreenShell.FooterOption firstOption = ScreenShell.defaultFooterOptions().get(0);

        ScreenShell.setFooterCompact(footer, true);

        assertTrue(footer.getStyleClass().contains(ScreenShell.SCREEN_FOOTER_COMPACT_STYLE_CLASS));
        assertEquals("‹", firstOption.displayText(false));
        assertEquals(6.0, footer.getSpacing());

        ScreenShell.setFooterCompact(footer, false);
        ScreenShell.setFooterLabelsVisible(footer, false);

        assertFalse(footer.getStyleClass().contains(ScreenShell.SCREEN_FOOTER_COMPACT_STYLE_CLASS));
        assertEquals(14.0, footer.getSpacing());
        assertEquals("‹ Back (Backspace)", firstOption.displayText());
    }

    @Test
    void footerLabelsFollowUserPreference() {
        HBox footer = new HBox();
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        preferencesService.saveFooterShortcutDisplay(FooterShortcutDisplay.HIDE);

        ScreenShell.applyFooterPreferences(footer, preferencesService);

        assertFalse(preferencesService.footerLabelsVisible());
        assertEquals(FooterShortcutDisplay.HIDE, preferencesService.footerShortcutDisplay());
    }

    @Test
    void footerLabelsAndTooltipsCanBeLocalized() {
        LocalizationService localizationService = new LocalizationService();
        localizationService.registerBundle(new LocalizedTextBundle("pirate", Map.of(
                "ui.footer.back.label", "Avast",
                "ui.footer.back.tooltip", "Sail back.")));

        List<ScreenShell.FooterOption> localized = ScreenShell.localizeFooterOptions(
                ScreenShell.defaultFooterOptions(),
                localizationService);
        ScreenShell.FooterOption back = localized.get(0);

        assertEquals("‹ Avast (Backspace)", back.displayText());
        assertEquals("Sail back.", back.tooltip());
        assertEquals("History", localized.get(1).label());
    }

    @Test
    void footerHelpersValidateRequiredArguments() {
        HBox footer = new HBox();

        assertThrows(IllegalArgumentException.class, () ->
                ScreenShell.setFooterVisible((javafx.scene.Node) null, true));
        assertThrows(IllegalArgumentException.class, () ->
                ScreenShell.setFooterTransparency(footer, -0.1));
        assertThrows(IllegalArgumentException.class, () ->
                ScreenShell.setFooterFontSize(footer, 0.0));
        assertThrows(IllegalArgumentException.class, () ->
                ScreenShell.setFooterBorderStyle(footer, "double"));
        assertThrows(IllegalArgumentException.class, () ->
                ScreenShell.changeFooterIcon(ScreenShell.defaultFooterOptions(), "quick-save", ""));
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenShell.FooterOption("", "?", "Help", "F1", "Help tooltip"));
    }
}
