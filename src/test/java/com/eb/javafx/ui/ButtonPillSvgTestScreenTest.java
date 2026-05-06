package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class ButtonPillSvgTestScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void exposesExpectedButtonPillScreenCopy() {
        assertEquals("Use this screen to confirm the shared button-pill.svg shape is visibly applied.",
                ButtonPillSvgTestScreen.DESCRIPTION_TEXT);
        assertEquals("All buttons below should render with the same pill silhouette from the packaged SVG resource.",
                ButtonPillSvgTestScreen.DETAIL_TEXT);
        assertEquals("Primary action", ButtonPillSvgTestScreen.PRIMARY_LABEL);
        assertEquals("Secondary action", ButtonPillSvgTestScreen.SECONDARY_LABEL);
        assertEquals("Back to main menu", ButtonPillSvgTestScreen.BACK_LABEL);
        assertEquals("M 24 0 H 156 Q 180 0 180 24 Q 180 48 156 48 H 24 Q 0 48 0 24 Q 0 0 24 0 Z",
                ButtonVisuals.buttonShapePath());
    }

    @Test
    void buttonPillArtworkIsBuiltFromPackagedSvg() {
        Node graphic = ButtonVisuals.createArtworkGraphic(ButtonPillSvgTestScreen.PRIMARY_LABEL);

        assertTrue(graphic instanceof StackPane);
        StackPane stack = (StackPane) graphic;
        assertTrue(stack.getStyleClass().contains(ButtonVisuals.BUTTON_ARTWORK_STYLE_CLASS));
        assertEquals(2, stack.getChildren().size());

        assertTrue(stack.getChildren().get(0) instanceof SVGPath);
        SVGPath artwork = (SVGPath) stack.getChildren().get(0);
        assertEquals(ButtonVisuals.buttonShapePath(), artwork.getContent());
        assertEquals(1.0, artwork.getStrokeWidth());

        assertTrue(stack.getChildren().get(1) instanceof Text);
        Text label = (Text) stack.getChildren().get(1);
        assertEquals(ButtonPillSvgTestScreen.PRIMARY_LABEL, label.getText());
        assertTrue(label.getStyleClass().contains(ButtonVisuals.BUTTON_ARTWORK_TEXT_STYLE_CLASS));
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
