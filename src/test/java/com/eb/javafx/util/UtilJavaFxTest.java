package com.eb.javafx.util;

import javafx.application.Platform;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class UtilJavaFxTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();
    private static final AtomicBoolean JAVAFX_AVAILABLE = new AtomicBoolean(true);

    @Test
    void runRejectsNullActions() {
        assertThrows(IllegalArgumentException.class, () -> UtilJavaFx.run(null));
    }

    @Test
    void runSchedulesActionFromNonFxThread() throws Exception {
        assumeTrue(startJavaFxToolkit());
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean ranOnJavaFxThread = new AtomicBoolean();

        UtilJavaFx.run(() -> {
            ranOnJavaFxThread.set(Platform.isFxApplicationThread());
            completed.countDown();
        });

        assertTrue(completed.await(5, TimeUnit.SECONDS), "JavaFX action did not run.");
        assertTrue(ranOnJavaFxThread.get(), "Action should run on the JavaFX application thread.");
    }

    @Test
    void runExecutesImmediatelyOnFxThread() throws Exception {
        assumeTrue(startJavaFxToolkit());
        CountDownLatch completed = new CountDownLatch(1);
        AtomicBoolean ranBeforeReturn = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                UtilJavaFx.run(() -> ranBeforeReturn.set(true));
                assertTrue(ranBeforeReturn.get(), "Action should run before UtilJavaFx.run returns on the FX thread.");
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });

        assertTrue(completed.await(5, TimeUnit.SECONDS), "JavaFX assertion did not complete.");
        assertNull(failure.get(), () -> "JavaFX assertion failed: " + failure.get());
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
