package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import com.eb.javafx.ui.ScreenLayoutModel;
import com.eb.javafx.ui.ScreenLayoutRenderer;
import com.eb.javafx.ui.UiTheme;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class AllItemsTestScreenTest {
    private static final Path ALL_ITEMS_DESIGN_PATH =
            JsonScreenDesignTestScreen.findRepositoryRootFrom(JsonScreenDesignTestScreen.defaultDesignPath())
                    .orElse(Path.of(""))
                    .resolve(Path.of("examples", "resources", "json", "screens", "all-items-test-screen.json"));
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void allItemsDesignFileExistsAndLoads() {
        assertTrue(Files.isRegularFile(ALL_ITEMS_DESIGN_PATH),
                "all-items-test-screen.json not found at " + ALL_ITEMS_DESIGN_PATH);

        ScreenLayoutModel model = JsonScreenDesignTestScreen.loadLayoutModel(ALL_ITEMS_DESIGN_PATH);

        assertEquals("All Items Test Screen", model.title());
        assertTrue(model.contentSections().size() > 0, "Expected at least one content section.");
    }

    @Test
    @ManualTest
    void runAllItemsTestScreenFromTestApp() throws Exception {
        assumeTrue(Boolean.getBoolean(TestScreenApplication.TEST_SCREEN_ACTIVE_PROPERTY),
                "Manual JavaFX screen test runs only from TestScreenApplication.");

        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            ScreenLayoutModel model = JsonScreenDesignTestScreen.loadLayoutModel(ALL_ITEMS_DESIGN_PATH);
            Parent previewRoot = ScreenLayoutRenderer.createScrollablePreviewRoot(model, ALL_ITEMS_DESIGN_PATH.getParent());

            Scene scene = new Scene(previewRoot,
                    TestUiScreenSize.sceneWidth(preferencesService),
                    TestUiScreenSize.sceneHeight(preferencesService));
            scene.getStylesheets().add(uiTheme.stylesheet());

            Stage stage = new Stage();
            stage.setTitle("All items test screen manual test");
            stage.setScene(scene);
            stage.show();
            assertTrue(stage.isShowing() && stage.getScene() != null, "All items test screen was not shown.");
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
