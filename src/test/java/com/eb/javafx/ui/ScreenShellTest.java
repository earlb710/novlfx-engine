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
    void titledScreenIncludesBottomCenteredFooterShortcuts() {
        BorderPane screen = ScreenShell.titled("Title", new VBox());

        assertTrue(screen.getStyleClass().contains(ScreenShell.SCREEN_ROOT_STYLE_CLASS));
        assertTrue(screen.getBottom() instanceof HBox);
        HBox footer = (HBox) screen.getBottom();
        assertTrue(footer.getStyleClass().contains(ScreenShell.SCREEN_FOOTER_BAR_STYLE_CLASS));
        assertEquals(8, footer.getChildren().size());
        assertEquals("‹ Back (Backspace)", ((javafx.scene.control.Label) footer.getChildren().get(0)).getText());
        assertEquals("◷ History (Ctrl+H)", ((javafx.scene.control.Label) footer.getChildren().get(1)).getText());
        assertEquals("⇥ Skip mode (Tab)", ((javafx.scene.control.Label) footer.getChildren().get(2)).getText());
        assertEquals("⇩ Load (Ctrl+L)", ((javafx.scene.control.Label) footer.getChildren().get(3)).getText());
        assertEquals("▣ Save (Ctrl+S)", ((javafx.scene.control.Label) footer.getChildren().get(4)).getText());
        assertEquals("⚡ Quick save (Ctrl+Q)", ((javafx.scene.control.Label) footer.getChildren().get(5)).getText());
        assertEquals("⚙ Preferences (Ctrl+P)", ((javafx.scene.control.Label) footer.getChildren().get(6)).getText());
        assertEquals("› Forward (Space)", ((javafx.scene.control.Label) footer.getChildren().get(7)).getText());
    }
}
