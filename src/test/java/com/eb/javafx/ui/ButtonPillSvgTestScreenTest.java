package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class ButtonPillSvgTestScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void createSceneShowsButtonsUsingSharedButtonPillShape() throws Exception {
        PreferencesService preferencesService = new PreferencesService();
        preferencesService.load();
        UiTheme uiTheme = new UiTheme();
        uiTheme.initialize(preferencesService);

        Scene scene = runOnJavaFxThread(() -> ButtonPillSvgTestScreen.createScene(
                "Button Pill SVG Test",
                preferencesService,
                uiTheme,
                () -> {
                }));

        BorderPane root = assertInstanceOf(BorderPane.class, scene.getRoot());
        VBox content = assertInstanceOf(VBox.class, root.getCenter());
        HBox actions = assertInstanceOf(HBox.class, content.getChildren().get(2));
        List<Button> buttons = actions.getChildren().stream()
                .map(node -> assertInstanceOf(Button.class, node))
                .toList();

        assertEquals(3, buttons.size());
        for (Button button : buttons) {
            SVGPath shape = assertInstanceOf(SVGPath.class, button.getShape());
            assertEquals(ButtonVisuals.buttonShapePath(), shape.getContent());
            assertTrue(button.getStyleClass().contains(ButtonVisuals.BUTTON_STYLE_CLASS));
        }
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
