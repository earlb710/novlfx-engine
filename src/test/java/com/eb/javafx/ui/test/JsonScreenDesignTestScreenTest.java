package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import com.eb.javafx.ui.ScreenLayoutModel;
import com.eb.javafx.ui.ScreenLayoutType;
import com.eb.javafx.ui.UiTheme;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

final class JsonScreenDesignTestScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @TempDir
    Path tempDir;

    @Test
    void defaultDesignPathLoadsBundledReloadableScreenDefinition() {
        Path designPath = JsonScreenDesignTestScreen.defaultDesignPath();

        ScreenLayoutModel model = JsonScreenDesignTestScreen.loadLayoutModel(designPath);

        assertTrue(Files.isRegularFile(designPath));
        assertEquals("Reloadable JSON Screen", model.title());
        assertEquals(ScreenLayoutType.FORM, model.type());
    }

    @Test
    void loadLayoutModelReflectsJsonEditsOnNextLoad() throws Exception {
        Path designPath = tempDir.resolve("reloadable.json");
        Files.writeString(designPath, designJson("First title"));

        ScreenLayoutModel first = JsonScreenDesignTestScreen.loadLayoutModel(designPath);

        Files.writeString(designPath, designJson("Second title"));
        ScreenLayoutModel second = JsonScreenDesignTestScreen.loadLayoutModel(designPath);

        assertEquals("First title", first.title());
        assertEquals("Second title", second.title());
    }

    @Test
    void repositoryRootDiscoveryFindsScreenDesignExamples() {
        Path root = JsonScreenDesignTestScreen.findRepositoryRootFrom(JsonScreenDesignTestScreen.defaultDesignPath())
                .orElseThrow();

        assertTrue(Files.isDirectory(root.resolve(Path.of("examples", "resources", "json", "screens"))));
    }

    @Test
    void managementWorkingDirectoryCanOverrideDefaultDesignPath() throws Exception {
        Path workingDirectory = Files.createDirectories(tempDir.resolve("screens"));
        Path designPath = workingDirectory.resolve("reloadable-test-screen.json");
        Files.writeString(designPath, designJson("Managed title"));

        assertEquals(designPath, JsonScreenDesignTestScreen.defaultDesignPath(workingDirectory));
    }

    @Test
    @ManualTest
    void runReloadableJsonScreenFromTestApp() throws Exception {
        assumeTrue(Boolean.getBoolean(TestScreenApplication.TEST_SCREEN_ACTIVE_PROPERTY),
                "Manual JavaFX screen test runs only from TestScreenApplication.");

        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            stage.setTitle("Reloadable JSON screen manual test");
            stage.setScene(JsonScreenDesignTestScreen.createScene(
                    JsonScreenDesignTestScreen.defaultDesignPath(),
                    preferencesService,
                    uiTheme));
            stage.show();
            assertTrue(stage.isShowing() && stage.getScene() != null, "Reloadable JSON screen was not shown.");
        });
    }

    private static String designJson(String title) {
        return """
                {
                  "id": "test.reloadable",
                  "title": "%s",
                  "layoutType": "FORM",
                  "metadata": {},
                  "blocks": [{"id": "body", "title": "Body"}],
                  "items": [{"id": "body.text", "blockId": "body", "type": "TEXT", "text": "Ready"}]
                }
                """.formatted(title);
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
