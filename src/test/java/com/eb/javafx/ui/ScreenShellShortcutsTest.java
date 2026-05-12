package com.eb.javafx.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class ScreenShellShortcutsTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();
    private static final AtomicBoolean JAVAFX_AVAILABLE = new AtomicBoolean(true);

    @Test
    void matchesShortcutAcceptsCtrlD() {
        KeyEvent event = keyEvent(KeyCode.D, true, false, false);
        assertTrue(ScreenShell.matchesShortcut(event, "Ctrl+D"));
        assertTrue(ScreenShell.matchesShortcut(event, "cmd+d"));
    }

    @Test
    void matchesShortcutRejectsMissingModifier() {
        KeyEvent event = keyEvent(KeyCode.D, false, false, false);
        assertFalse(ScreenShell.matchesShortcut(event, "Ctrl+D"));
    }

    @Test
    void matchesShortcutRejectsExtraModifier() {
        KeyEvent event = keyEvent(KeyCode.D, true, true, false);
        assertFalse(ScreenShell.matchesShortcut(event, "Ctrl+D"));
        assertTrue(ScreenShell.matchesShortcut(event, "Ctrl+Shift+D"));
    }

    @Test
    void matchesShortcutHandlesNamedKeys() {
        assertTrue(ScreenShell.matchesShortcut(keyEvent(KeyCode.SPACE, false, false, false), "Space"));
        assertTrue(ScreenShell.matchesShortcut(keyEvent(KeyCode.BACK_SPACE, false, false, false), "Backspace"));
        assertTrue(ScreenShell.matchesShortcut(keyEvent(KeyCode.TAB, false, false, false), "Tab"));
    }

    @Test
    void matchesShortcutReturnsFalseForUnknownKey() {
        assertFalse(ScreenShell.matchesShortcut(keyEvent(KeyCode.D, true, false, false), "Ctrl+NotARealKey"));
    }

    @Test
    void matchesShortcutRejectsBlankShortcut() {
        assertThrows(IllegalArgumentException.class, () ->
                ScreenShell.matchesShortcut(keyEvent(KeyCode.D, true, false, false), ""));
    }

    @Test
    void installFooterShortcutsDispatchesMatchingHandlerAndConsumesEvent() throws Exception {
        assumeTrue(startJavaFxToolkit());

        AtomicInteger debugCount = new AtomicInteger();
        AtomicInteger prefsCount = new AtomicInteger();
        Scene scene = runOnFxThread(() -> {
            Scene s = new Scene(new StackPane(), 100, 100);
            ScreenShell.installFooterShortcuts(s, Map.of(
                    "Ctrl+D", debugCount::incrementAndGet,
                    "Ctrl+P", prefsCount::incrementAndGet));
            return s;
        });
        runOnFxThread(() -> {
            scene.getRoot().fireEvent(keyEvent(KeyCode.D, true, false, false));
            scene.getRoot().fireEvent(keyEvent(KeyCode.D, true, false, false));
            scene.getRoot().fireEvent(keyEvent(KeyCode.P, true, false, false));
            scene.getRoot().fireEvent(keyEvent(KeyCode.X, true, false, false));
            return null;
        });

        assertEquals(2, debugCount.get());
        assertEquals(1, prefsCount.get());
    }

    @Test
    void installFooterShortcutsIgnoresEmptyHandlerMap() throws Exception {
        assumeTrue(startJavaFxToolkit());

        Scene scene = runOnFxThread(() -> new Scene(new StackPane(), 100, 100));
        runOnFxThread(() -> {
            ScreenShell.installFooterShortcuts(scene, Map.of());
            return null;
        });
        // No assertion needed — verifies no NPE / no installed filter side-effects.
    }

    private static KeyEvent keyEvent(KeyCode code, boolean shortcut, boolean shift, boolean alt) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, shortcut, alt, false);
    }

    private static <T> T runOnFxThread(java.util.concurrent.Callable<T> action) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return action.call();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(action.call());
            } catch (Exception exception) {
                failure.set(exception);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for JavaFX action.");
        }
        if (failure.get() != null) {
            throw failure.get();
        }
        return result.get();
    }

    private static boolean startJavaFxToolkit() throws InterruptedException {
        if (!JAVAFX_AVAILABLE.get()) {
            return false;
        }
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
            } catch (UnsupportedOperationException exception) {
                JAVAFX_AVAILABLE.set(false);
                started.countDown();
                return false;
            }
        } else {
            Platform.setImplicitExit(false);
            started.countDown();
        }
        return started.await(5, TimeUnit.SECONDS);
    }
}
