package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import com.eb.javafx.ui.ButtonVisuals;
import com.eb.javafx.ui.UiTheme;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class ButtonPillSvgTestScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void exposesExpectedButtonPillScreenCopy() {
        assertEquals("Use this screen to confirm the shared button-pill-normal.svg shape is visibly applied.",
                ButtonPillSvgTestScreen.DESCRIPTION_TEXT);
        assertEquals("Buttons below cover short, normal, fixed-width, long-label, and multiline SVG artwork.",
                ButtonPillSvgTestScreen.DETAIL_TEXT);
        assertEquals("Short", ButtonPillSvgTestScreen.PRIMARY_LABEL);
        assertEquals("Long dynamic text button", ButtonPillSvgTestScreen.SECONDARY_LABEL);
        assertEquals("Fixed width", ButtonPillSvgTestScreen.FIXED_LABEL);
        assertEquals("Multiline\nbutton text", ButtonPillSvgTestScreen.MULTILINE_LABEL);
        assertEquals("Back", ButtonPillSvgTestScreen.BACK_LABEL);
        assertTrue(ButtonVisuals.buttonShapePath().startsWith("M "));
    }

    @Test
    void buttonPillArtworkUsesPackagedSvgResource() {
        assertTrue(ButtonVisuals.buttonArtworkResourceUrl()
                .endsWith(ButtonVisuals.BUTTON_NORMAL_ARTWORK_RESOURCE.substring(1)));
    }

    @Test
    void buttonPillArtworkSelectsShortNormalAndLongSvgResources() {
        String normalLabel = "Normal";
        double normalWidth = 160;

        assertTrue(ButtonVisuals.usesShortArtwork(ButtonPillSvgTestScreen.PRIMARY_LABEL, -1, -1));
        assertFalse(ButtonVisuals.usesShortArtwork(normalLabel, normalWidth, ButtonVisuals.BUTTON_ARTWORK_HEIGHT));
        assertTrue(ButtonVisuals.usesLongArtwork(ButtonPillSvgTestScreen.SECONDARY_LABEL, -1, -1));
        assertTrue(ButtonVisuals.usesLongArtwork(ButtonPillSvgTestScreen.MULTILINE_LABEL, -1, -1));
        assertTrue(ButtonVisuals.usesLongArtwork(ButtonPillSvgTestScreen.FIXED_LABEL, 260, 64));
        assertFalse(ButtonVisuals.usesLongArtwork(normalLabel, normalWidth, ButtonVisuals.BUTTON_ARTWORK_HEIGHT));
        assertTrue(ButtonVisuals.buttonArtworkResourceUrl(ButtonPillSvgTestScreen.PRIMARY_LABEL, -1, -1)
                .endsWith(ButtonVisuals.BUTTON_SHORT_ARTWORK_RESOURCE.substring(1)));
        assertTrue(ButtonVisuals.buttonArtworkResourceUrl(normalLabel, normalWidth, ButtonVisuals.BUTTON_ARTWORK_HEIGHT)
                .endsWith(ButtonVisuals.BUTTON_NORMAL_ARTWORK_RESOURCE.substring(1)));
        assertTrue(ButtonVisuals.buttonArtworkResourceUrl(ButtonPillSvgTestScreen.SECONDARY_LABEL, -1, -1)
                .endsWith(ButtonVisuals.BUTTON_LONG_ARTWORK_RESOURCE.substring(1)));
        assertFalse(ButtonVisuals.usesLongArtwork(ButtonPillSvgTestScreen.PRIMARY_LABEL, -1, -1));
    }

    @Test
    void buttonPillArtworkUsesRasterizedImageViewAtTextSizedDimensions() {
        StackPane graphic = assertInstanceOf(StackPane.class, ButtonVisuals.createArtworkGraphic("Play"));
        double width = graphic.prefWidth(-1);
        double height = graphic.prefHeight(width);
        graphic.resize(width, height);
        graphic.layout();

        Node artwork = graphic.getChildren().get(0);
        ImageView imageView = assertInstanceOf(ImageView.class, artwork);

        assertEquals(width, imageView.getFitWidth());
        assertEquals(height, imageView.getFitHeight());
        assertEquals(width, imageView.getImage().getWidth());
        assertEquals(height, imageView.getImage().getHeight());
    }

    @Test
    void buttonPillArtworkTextSizedDimensionsGrowForLongAndMultilineLabels() {
        StackPane shortGraphic = assertInstanceOf(StackPane.class,
                ButtonVisuals.createArtworkGraphic(ButtonPillSvgTestScreen.PRIMARY_LABEL));
        StackPane longGraphic = assertInstanceOf(StackPane.class,
                ButtonVisuals.createArtworkGraphic(ButtonPillSvgTestScreen.SECONDARY_LABEL));
        StackPane multilineGraphic = assertInstanceOf(StackPane.class,
                ButtonVisuals.createArtworkGraphic(ButtonPillSvgTestScreen.MULTILINE_LABEL));

        double shortWidth = shortGraphic.prefWidth(-1);
        double longWidth = longGraphic.prefWidth(-1);
        double multilineHeight = multilineGraphic.prefHeight(multilineGraphic.prefWidth(-1));

        assertTrue(shortWidth >= 48);
        assertTrue(longWidth > shortWidth, "Long dynamic button should be wider than short button.");
        assertTrue(multilineHeight > ButtonVisuals.BUTTON_ARTWORK_HEIGHT,
                "Multiline button should be taller than a single-line button.");
    }

    @Test
    void buttonPillArtworkUsesBlueGradientForLongButtonArtwork() {
        StackPane graphic = assertInstanceOf(StackPane.class,
                ButtonVisuals.createArtworkGraphic(ButtonPillSvgTestScreen.SECONDARY_LABEL));
        double width = graphic.prefWidth(-1);
        double height = graphic.prefHeight(width);
        graphic.resize(width, height);
        graphic.layout();

        ImageView imageView = assertInstanceOf(ImageView.class, graphic.getChildren().get(0));
        Image image = imageView.getImage();
        int centerY = Math.max(0, (int) Math.round(image.getHeight() / 2) - 1);
        Color middleCenter = image.getPixelReader().getColor((int) image.getWidth() / 2, centerY);

        assertTrue(middleCenter.getBlue() > middleCenter.getRed()
                        && middleCenter.getBlue() > middleCenter.getGreen(),
                "Button artwork should use a blue gradient.");
    }

    @Test
    void buttonPillArtworkSwapsGradientDirectionWhenPressed() {
        Image normalImage = rasterizedImage(ButtonPillSvgTestScreen.SECONDARY_LABEL, false);
        Image pressedImage = rasterizedImage(ButtonPillSvgTestScreen.SECONDARY_LABEL, true);
        int centerX = Math.max(0, (int) Math.round(normalImage.getWidth() / 2) - 1);
        int topY = 4;
        int bottomY = Math.max(topY + 1, (int) normalImage.getHeight() - 5);

        double normalDelta = brightness(normalImage.getPixelReader().getColor(centerX, topY))
                - brightness(normalImage.getPixelReader().getColor(centerX, bottomY));
        double pressedDelta = brightness(pressedImage.getPixelReader().getColor(centerX, topY))
                - brightness(pressedImage.getPixelReader().getColor(centerX, bottomY));

        assertTrue(Math.abs(normalDelta) > 0.05, "Normal button artwork should have a visible vertical gradient.");
        assertTrue(Math.abs(pressedDelta) > 0.05, "Pressed button artwork should have a visible vertical gradient.");
        assertTrue(normalDelta * pressedDelta < 0, "Pressed artwork should reverse the normal gradient direction.");
    }

    @Test
    void buttonPillArtworkRasterizesDirectlyAtFixedDimensions() {
        StackPane graphic = assertInstanceOf(StackPane.class, ButtonVisuals.createArtworkGraphic("Multi\nLine", 220, 72));
        graphic.resize(220, 72);
        graphic.layout();

        Node artwork = graphic.getChildren().get(0);
        ImageView imageView = assertInstanceOf(ImageView.class, artwork);

        assertEquals(220, imageView.getFitWidth());
        assertEquals(72, imageView.getFitHeight());
        assertEquals(220, imageView.getImage().getWidth());
        assertEquals(72, imageView.getImage().getHeight());
    }

    private static Image rasterizedImage(String text, boolean pressed) {
        StackPane graphic = assertInstanceOf(StackPane.class,
                ButtonVisuals.createArtworkGraphic(text, -1, -1, pressed));
        double width = graphic.prefWidth(-1);
        double height = graphic.prefHeight(width);
        graphic.resize(width, height);
        graphic.layout();
        ImageView imageView = assertInstanceOf(ImageView.class, graphic.getChildren().get(0));
        return imageView.getImage();
    }

    private static double brightness(Color color) {
        return color.getRed() + color.getGreen() + color.getBlue();
    }

    @Test
    @ManualTest
    void runButtonPillSvgTestScreenFromTestApp() throws Exception {
        assumeTrue(Boolean.getBoolean(TestScreenApplication.TEST_SCREEN_ACTIVE_PROPERTY),
                "Manual JavaFX screen test runs only from TestScreenApplication.");

        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            stage.setTitle("ButtonPillSvgTestScreen manual test");
            stage.setScene(ButtonPillSvgTestScreen.createScene(
                    "Button Pill SVG Test",
                    preferencesService,
                    uiTheme,
                    stage::close));
            stage.show();
            assertTrue(stage.isShowing() && stage.getScene() != null, "ButtonPillSvgTestScreen window was not shown.");
        });
    }

    private static <T> T runOnJavaFxThread(ThrowingSupplier<T> action) throws Exception {
        startJavaFxToolkit();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(action.get());
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
        return result.get();
    }

    private static void runOnJavaFxThread(ThrowingRunnable action) throws Exception {
        runOnJavaFxThread(() -> {
            action.run();
            return null;
        });
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

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
