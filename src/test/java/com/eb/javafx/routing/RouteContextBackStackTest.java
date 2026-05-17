package com.eb.javafx.routing;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.EnginePlaceholderContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.ui.UiTheme;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Locks in {@link RouteContext}'s back-stack contract: {@link RouteContext#pushAndNavigateTo}
 * saves the current scene root, {@link RouteContext#navigateBack} restores it (same node
 * identity so any in-flight state — scroll position, scene-flow stepHistory in closures, etc. —
 * is preserved across the round-trip). This is the basis for the Preferences close button
 * returning to "where the player was" instead of always bouncing them to the main menu.
 */
final class RouteContextBackStackTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void pushAndNavigateRecordsCurrentRootThenNavigateBackRestoresIt() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX route test requires a display.");
        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();
            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            // Seed the stage with a marker scene so we can prove the SAME root is restored after
            // the round-trip (not a fresh scene constructed by re-opening the same route).
            StackPane originalRoot = new StackPane(new Label("ORIGINAL"));
            stage.setScene(new Scene(originalRoot, 400, 300));

            RouteContext context = newContext(stage, preferencesService, uiTheme);

            assertFalse(context.canNavigateBack(), "Fresh context starts with an empty back-stack.");

            context.pushAndNavigateTo(SceneRouter.PREFERENCES_ROUTE);

            assertTrue(context.canNavigateBack(), "Push must enqueue a back-stack entry.");
            Parent prefsRoot = stage.getScene().getRoot();
            assertNotSame(originalRoot, prefsRoot,
                    "After push+navigate, the preferences root must replace the original.");

            assertTrue(context.navigateBack(), "navigateBack should succeed when something was pushed.");
            assertSame(originalRoot, stage.getScene().getRoot(),
                    "navigateBack must restore the EXACT original root (same instance), preserving state.");
            assertFalse(context.canNavigateBack(), "Back-stack should be empty after the only entry is popped.");

            stage.close();
        });
    }

    @Test
    void navigateBackReturnsFalseAndNoOpsWhenBackStackIsEmpty() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX route test requires a display.");
        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();
            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            StackPane originalRoot = new StackPane(new Label("ORIGINAL"));
            stage.setScene(new Scene(originalRoot, 400, 300));

            RouteContext context = newContext(stage, preferencesService, uiTheme);

            assertFalse(context.navigateBack(),
                    "navigateBack must report false (so callers can fall back) when there's nothing pushed.");
            assertSame(originalRoot, stage.getScene().getRoot(),
                    "An empty-stack navigateBack must not mutate the current scene.");
            stage.close();
        });
    }

    @Test
    void plainNavigateToDoesNotPushSoTheBackStackStaysEmpty() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX route test requires a display.");
        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();
            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            stage.setScene(new Scene(new StackPane(), 400, 300));

            RouteContext context = newContext(stage, preferencesService, uiTheme);
            // Drives the "fall back to main menu" branch in PreferencesSummaryScreen's close
            // handler — older or non-overlay navigations should NOT pile entries onto the stack.
            context.navigateTo(SceneRouter.PREFERENCES_ROUTE);

            assertFalse(context.canNavigateBack(),
                    "Plain navigateTo must not push the back-stack — only pushAndNavigateTo does.");
            stage.close();
        });
    }

    private static RouteContext newContext(Stage stage, PreferencesService prefs, UiTheme theme) {
        ContentRegistry contentRegistry = new ContentRegistry();
        contentRegistry.registerBaseContent();
        new EnginePlaceholderContentModule().register(contentRegistry, null);
        ImageDisplayRegistry imageDisplayRegistry = new ImageDisplayRegistry();
        imageDisplayRegistry.registerBaseDisplayContent();
        SaveLoadService saveLoadService = new SaveLoadService();
        saveLoadService.initialize();
        SceneRouter router = new SceneRouter();
        router.registerDefaultRoutes(stage, prefs, contentRegistry, imageDisplayRegistry, saveLoadService, theme);
        return new RouteContext(stage, prefs, contentRegistry, imageDisplayRegistry, saveLoadService, theme, router);
    }

    private static void runOnJavaFxThread(Runnable action) throws Exception {
        startJavaFxToolkit();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
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
        if (!JAVAFX_STARTED.compareAndSet(false, true)) {
            return;
        }
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                started.countDown();
            });
        } catch (IllegalStateException already) {
            Platform.setImplicitExit(false);
            started.countDown();
        }
        assertTrue(started.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start.");
    }
}
