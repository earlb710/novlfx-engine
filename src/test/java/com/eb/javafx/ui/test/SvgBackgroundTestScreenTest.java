package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import com.eb.javafx.ui.ScreenShell;
import com.eb.javafx.ui.UiTheme;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BackgroundFill;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class SvgBackgroundTestScreenTest {
    private static final int GRADIENT_BACKGROUND_INDEX = 0;
    private static final int CIRCLE_BACKGROUND_INDEX = 1;
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void exposesExpectedSvgBackgroundScreenCopy() {
        assertEquals("Use this screen to confirm the SVG background helper fills the whole scene, preserves gradients, and supports transparency.",
                SvgBackgroundTestScreen.DESCRIPTION_TEXT);
        assertEquals("Choose a packaged SVG background, adjust transparency, and swap the canvas color shown behind transparent SVG regions.",
                SvgBackgroundTestScreen.DETAIL_TEXT);
        assertEquals("Back to main menu", SvgBackgroundTestScreen.BACK_LABEL);
        assertEquals("Background", SvgBackgroundTestScreen.BACKGROUND_LABEL);
        assertEquals("Transparency", SvgBackgroundTestScreen.TRANSPARENCY_LABEL);
        assertEquals("Canvas color", SvgBackgroundTestScreen.CANVAS_COLOR_LABEL);
        assertEquals(0.0, SvgBackgroundTestScreen.DEFAULT_TRANSPARENCY);
        assertEquals(Color.web("#08111f"), SvgBackgroundTestScreen.DEFAULT_CANVAS_COLOR);
        assertEquals(List.of(
                        SvgBackgroundTestScreen.GRADIENT_BACKGROUND_RESOURCE,
                        SvgBackgroundTestScreen.CIRCLE_BACKGROUND_RESOURCE),
                SvgBackgroundTestScreen.BACKGROUND_OPTIONS.stream()
                        .map(SvgBackgroundTestScreen.BackgroundOption::resourcePath)
                        .toList());
    }

    @Test
    void gradientBackgroundSvgResourceIsPackaged() throws Exception {
        String svg = Files.readString(Path.of(
                "src/main/resources/com/eb/javafx/images/svg/background-gradient-rectangle.svg"));

        assertTrue(svg.contains("<linearGradient id=\"screenBackgroundGradient\""));
        assertTrue(svg.contains("<rect width=\"1280\" height=\"720\" fill=\"url(#screenBackgroundGradient)\""));
    }

    @Test
    void circleBackgroundSvgResourceIsPackaged() throws Exception {
        String svg = Files.readString(Path.of(
                "src/main/resources/com/eb/javafx/images/svg/circle-background.svg"));

        assertTrue(svg.contains("viewBox=\"0 0 3000.39 1500.53\""));
        assertTrue(svg.contains("id=\"linear-pattern-0\""));
        assertTrue(svg.contains("id=\"clip-1\""));
    }

    @Test
    void usesSelectedResourceWithSvgBackgroundHelper() {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX chooser test requires a display.");
        assertDoesNotThrow(() -> runOnJavaFxThread(() -> {
            StackPane root = SvgBackgroundTestScreen.createRoot("SVG Background Test", () -> {
            });
            assertEquals(2, root.getChildren().size());
            Region background = assertInstanceOf(Region.class, root.getChildren().get(0));
            BorderPane screen = assertInstanceOf(BorderPane.class, root.getChildren().get(1));
            VBox panel = assertInstanceOf(VBox.class, screen.getCenter());
            @SuppressWarnings("unchecked")
            ComboBox<SvgBackgroundTestScreen.BackgroundOption> backgroundChoices =
                    assertInstanceOf(ComboBox.class, panel.getChildren().get(2));
            Label transparency = assertInstanceOf(Label.class, panel.getChildren().get(3));
            Slider transparencySlider = assertInstanceOf(Slider.class, panel.getChildren().get(4));
            ColorPicker canvasColorPicker = assertInstanceOf(ColorPicker.class, panel.getChildren().get(6));
            Label details = assertInstanceOf(Label.class, panel.getChildren().get(7));
            ImageView backgroundImage = assertInstanceOf(ImageView.class, background.getChildrenUnmodifiable().get(0));

            assertTrue(background.getStyleClass().contains(ScreenShell.SCREEN_BACKGROUND_SVG_STYLE_CLASS));
            assertTrue(background.isMouseTransparent());
            assertTrue(background.prefWidthProperty().isBound());
            assertTrue(background.prefHeightProperty().isBound());
            assertSame(screen, root.getChildren().get(1));
            assertEquals(SvgBackgroundTestScreen.BACKGROUND_OPTIONS.get(GRADIENT_BACKGROUND_INDEX), backgroundChoices.getValue());
            assertEquals("Transparency (0%)", transparency.getText());
            assertEquals(SvgBackgroundTestScreen.DEFAULT_TRANSPARENCY, transparencySlider.getValue());
            assertEquals(SvgBackgroundTestScreen.DEFAULT_CANVAS_COLOR, canvasColorPicker.getValue());
            assertEquals(SvgBackgroundTestScreen.BACKGROUND_OPTIONS.get(GRADIENT_BACKGROUND_INDEX).detailText(), details.getText());
            assertEquals(1.0, backgroundImage.getOpacity());

            transparencySlider.setValue(0.40);
            canvasColorPicker.setValue(Color.BEIGE);

            backgroundChoices.getSelectionModel().select(CIRCLE_BACKGROUND_INDEX);

            Region replacement = assertInstanceOf(Region.class, root.getChildren().get(0));
            ImageView replacementImage = assertInstanceOf(ImageView.class, replacement.getChildrenUnmodifiable().get(0));
            assertNotNull(replacement);
            assertTrue(replacement.prefWidthProperty().isBound());
            assertTrue(replacement.prefHeightProperty().isBound());
            assertEquals("Transparency (40%)", transparency.getText());
            assertEquals(Color.BEIGE, ((BackgroundFill) replacement.getBackground().getFills().get(0)).getFill());
            assertEquals(0.60, replacementImage.getOpacity(), 0.0001);
            assertEquals(SvgBackgroundTestScreen.BACKGROUND_OPTIONS.get(CIRCLE_BACKGROUND_INDEX).detailText(), details.getText());
        }));
    }

    @Test
    @ManualTest
    void runSvgBackgroundTestScreenFromTestApp() throws Exception {
        assumeTrue(Boolean.getBoolean(TestScreenApplication.TEST_SCREEN_ACTIVE_PROPERTY),
                "Manual JavaFX screen test runs only from TestScreenApplication.");

        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            stage.setTitle("SvgBackgroundTestScreen manual test");
            stage.setScene(SvgBackgroundTestScreen.createScene(
                    "SVG Background Test",
                    preferencesService,
                    uiTheme,
                    stage::close));
            stage.show();
            assertTrue(stage.isShowing() && stage.getScene() != null, "SvgBackgroundTestScreen window was not shown.");
        });
    }

    private static void runOnJavaFxThread(Runnable action) throws Exception {
        startJavaFxToolkit();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });
        assertTrue(completed.await(5, TimeUnit.SECONDS), "JavaFX action did not complete.");
        if (failure.get() instanceof Exception exception) {
            throw exception;
        }
        if (failure.get() instanceof Error error) {
            throw error;
        }
        assertNull(failure.get(), () -> "JavaFX action failed: " + failure.get());
    }

    private static void startJavaFxToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        if (JAVAFX_STARTED.compareAndSet(false, true)) {
            try {
                Platform.startup(() -> {
                    Platform.setImplicitExit(false);
                    started.countDown();
                });
            } catch (IllegalStateException exception) {
                Platform.setImplicitExit(false);
                started.countDown();
            }
        } else {
            Platform.setImplicitExit(false);
            started.countDown();
        }
        assertTrue(started.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start.");
    }
}
