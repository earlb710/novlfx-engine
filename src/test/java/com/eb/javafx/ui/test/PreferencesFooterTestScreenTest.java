package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import com.eb.javafx.ui.ScreenShell;
import com.eb.javafx.ui.UiTheme;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

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
            stage.setScene(PreferencesFooterTestScreen.createScene(
                    "Preferences Footer Test",
                    preferencesService,
                    uiTheme,
                    () -> stage.setTitle("Preferences opened from footer"),
                    stage::close));
            stage.show();
            assertTrue(stage.isShowing() && stage.getScene() != null,
                    "PreferencesFooterTestScreen window was not shown.");
        });
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

    private static void startJavaFxToolkit() {
        if (JAVAFX_STARTED.compareAndSet(false, true)) {
            Platform.startup(() -> {
            });
        }
    }
}
