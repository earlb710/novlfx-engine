package com.eb.javafx.ui;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.EnginePlaceholderContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class PreferencesSummaryScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void footerOptionsOnlyEnablePreferencesWhilePreferencesIsOpen() {
        assertEquals(8, PreferencesSummaryScreen.footerOptions().size());

        for (ScreenShell.FooterOption option : PreferencesSummaryScreen.footerOptions()) {
            if (option.id().equals("preferences")) {
                assertTrue(option.enabled(), "preferences should stay enabled.");
                assertEquals("Close preferences.", option.tooltip());
            } else {
                assertFalse(option.enabled(), option.id() + " should be disabled.");
            }
        }
    }

    @Test
    void shortcutRequiresPlatformModifierAndPKey() {
        assertTrue(PreferencesSummaryScreen.isCloseShortcut(KeyCode.P, true));
        assertFalse(PreferencesSummaryScreen.isCloseShortcut(KeyCode.P, false));
        assertFalse(PreferencesSummaryScreen.isCloseShortcut(KeyCode.B, true));
    }

    @Test
    void preferencesFooterClosesBackToMainMenu() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX route navigation test requires a display.");
        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            SceneRouter router = createManualRouter(stage, preferencesService, uiTheme);
            Scene initialScene = router.open(SceneRouter.PREFERENCES_ROUTE);
            stage.setScene(initialScene);

            BorderPane initialRoot = (BorderPane) initialScene.getRoot();
            HBox footer = (HBox) initialRoot.getBottom();
            for (var child : footer.getChildren()) {
                if (child instanceof Label label && label.getUserData() instanceof ScreenShell.FooterOption option) {
                    if (option.id().equals("preferences")) {
                        assertFalse(label.isDisabled(), "Preferences footer icon should stay enabled.");
                    } else {
                        assertTrue(label.isDisabled(), option.id() + " footer icon should be disabled.");
                    }
                }
            }

            Label preferencesLabel = footer.getChildren().stream()
                    .filter(Label.class::isInstance)
                    .map(Label.class::cast)
                    .filter(label -> label.getUserData() instanceof ScreenShell.FooterOption option
                            && option.id().equals("preferences"))
                    .findFirst()
                    .orElseThrow();

            preferencesLabel.getOnMouseClicked().handle(null);

            assertTrue(stage.getScene() != null && stage.getScene() != initialScene,
                    "Preferences footer action should replace the scene.");
            BorderPane mainMenuRoot = (BorderPane) stage.getScene().getRoot();
            assertEquals("Main Menu", ((Label) mainMenuRoot.getTop()).getText());
            stage.close();
        });
    }

    private static SceneRouter createManualRouter(Stage stage, PreferencesService preferencesService, UiTheme uiTheme) {
        ContentRegistry contentRegistry = new ContentRegistry();
        contentRegistry.registerBaseContent();
        new EnginePlaceholderContentModule().register(contentRegistry, null);

        ImageDisplayRegistry imageDisplayRegistry = new ImageDisplayRegistry();
        imageDisplayRegistry.registerBaseDisplayContent();

        SaveLoadService saveLoadService = new SaveLoadService();
        saveLoadService.initialize();

        SceneRouter router = new SceneRouter();
        router.registerDefaultRoutes(stage, preferencesService, contentRegistry, imageDisplayRegistry, saveLoadService, uiTheme);
        return router;
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
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
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
