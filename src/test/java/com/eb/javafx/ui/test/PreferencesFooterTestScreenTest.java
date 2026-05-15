package com.eb.javafx.ui.test;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.EnginePlaceholderContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import com.eb.javafx.ui.ScreenShell;
import com.eb.javafx.ui.UiTheme;
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

final class PreferencesFooterTestScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void footerOptionsEnableBackAndPreferencesOnly() {
        assertEquals(8, PreferencesFooterTestScreen.footerOptions().size());

        for (ScreenShell.FooterOption option : PreferencesFooterTestScreen.footerOptions()) {
            if (option.id().equals("back") || option.id().equals("preferences")) {
                assertTrue(option.enabled(), option.id() + " should be enabled.");
            } else {
                assertFalse(option.enabled(), option.id() + " should be disabled.");
            }
        }
    }

    @Test
    void shortcutRequiresPlatformModifierAndPKey() {
        assertTrue(PreferencesFooterTestScreen.isPreferencesShortcut(KeyCode.P, true));
        assertFalse(PreferencesFooterTestScreen.isPreferencesShortcut(KeyCode.P, false));
        assertFalse(PreferencesFooterTestScreen.isPreferencesShortcut(KeyCode.B, true));
    }

    @Test
    void footerOptionTriggersPreferencesAndBackActions() {
        AtomicBoolean preferencesOpened = new AtomicBoolean();
        AtomicBoolean backOpened = new AtomicBoolean();
        ScreenShell.FooterOption preferences = option("preferences");
        ScreenShell.FooterOption back = option("back");
        ScreenShell.FooterOption save = option("save");

        assertTrue(PreferencesFooterTestScreen.triggerFooterOption(preferences, () -> preferencesOpened.set(true), () -> backOpened.set(true)));
        assertTrue(preferencesOpened.get());
        assertFalse(backOpened.get());

        assertTrue(PreferencesFooterTestScreen.triggerFooterOption(back, () -> preferencesOpened.set(false), () -> backOpened.set(true)));
        assertTrue(backOpened.get());

        assertFalse(PreferencesFooterTestScreen.triggerFooterOption(save, () -> preferencesOpened.set(false), () -> backOpened.set(false)));
    }

    @Test
    void footerPreferencesLabelNavigatesToPreferencesRouteFromTestScreen() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX route navigation test requires a display.");
        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            SceneRouter router = createManualRouter(stage, preferencesService, uiTheme);
            Scene initialScene = router.open(SceneRouter.PREFERENCES_FOOTER_TEST_ROUTE);
            stage.setScene(initialScene);

            BorderPane initialRoot = (BorderPane) initialScene.getRoot();
            HBox footer = (HBox) initialRoot.getBottom();
            Label preferencesLabel = footer.getChildren().stream()
                    .filter(Label.class::isInstance)
                    .map(Label.class::cast)
                    .filter(label -> label.getUserData() instanceof ScreenShell.FooterOption option
                            && option.id().equals("preferences"))
                    .findFirst()
                    .orElseThrow();

            javafx.scene.Parent initialSceneRoot = initialScene.getRoot();
            preferencesLabel.getOnMouseClicked().handle(null);

            assertTrue(stage.getScene() != null && stage.getScene().getRoot() != initialSceneRoot,
                    "Preferences footer action should replace the scene root.");
            BorderPane preferencesRoot = (BorderPane) stage.getScene().getRoot();
            assertEquals("Preferences", ((Label) preferencesRoot.getTop()).getText());
            stage.close();
        });
    }

    @Test
    @ManualTest
    void runPreferencesFooterTestScreenFromTestApp() throws Exception {
        assumeTrue(Boolean.getBoolean(TestScreenApplication.TEST_SCREEN_ACTIVE_PROPERTY),
                "Manual JavaFX screen test runs only from TestScreenApplication.");

        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            stage.setTitle("PreferencesFooterTestScreen manual test");
            SceneRouter router = createManualRouter(stage, preferencesService, uiTheme);
            stage.setScene(router.open(SceneRouter.PREFERENCES_FOOTER_TEST_ROUTE));
            stage.show();
            assertTrue(stage.isShowing() && stage.getScene() != null,
                    "PreferencesFooterTestScreen window was not shown.");
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

    private static ScreenShell.FooterOption option(String id) {
        return PreferencesFooterTestScreen.footerOptions().stream()
                .filter(option -> option.id().equals(id))
                .findFirst()
                .orElseThrow();
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
                // TestScreenApplication may already have started JavaFX before this manual test runs.
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
