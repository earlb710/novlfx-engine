package com.eb.javafx.ui;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void styledPanelAppliesSharedAndSceneSpecificHooks() {
        VBox panel = ScreenShell.styledPanel(ScreenShell.SCENE_DIALOGUE_PANEL_STYLE_CLASS, new BorderPane());

        assertTrue(panel.getStyleClass().contains(ScreenShell.SCREEN_PANEL_STYLE_CLASS));
        assertTrue(panel.getStyleClass().contains(ScreenShell.SCENE_DIALOGUE_PANEL_STYLE_CLASS));
        assertEquals(ScreenShell.BODY_SPACING, panel.getSpacing());
        assertEquals(ScreenShell.PANEL_INSETS, panel.getPadding());
    }

    @Test
    void footerOptionTextsExposeRequestedIconShortcuts() {
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
    void footerOptionsCanBeCustomizedWithoutChangingDefaults() {
        List<ScreenShell.FooterOption> customized = ScreenShell.changeFooterTooltip(
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
                "Immediately write a quick save.");

        ScreenShell.FooterOption quickSave = customized.stream()
                .filter(option -> option.id().equals("quick-save"))
                .findFirst()
                .orElseThrow();

        assertEquals("💾 Quicksave (Ctrl+Shift+S)", quickSave.displayText());
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
        assertEquals(0.35, footer.getOpacity());
    }

    @Test
    void footerHelpersValidateRequiredArguments() {
        HBox footer = new HBox();

        assertThrows(IllegalArgumentException.class, () ->
                ScreenShell.setFooterVisible((javafx.scene.Node) null, true));
        assertThrows(IllegalArgumentException.class, () ->
                ScreenShell.setFooterTransparency(footer, -0.1));
        assertThrows(IllegalArgumentException.class, () ->
                ScreenShell.changeFooterIcon(ScreenShell.defaultFooterOptions(), "quick-save", ""));
        assertThrows(IllegalArgumentException.class, () ->
                new ScreenShell.FooterOption("", "?", "Help", "F1", "Help tooltip"));
    }
}
