package com.eb.javafx.testsupport;

import javafx.application.Platform;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts the JavaFX toolkit <strong>once, at the very start of the test session</strong> — before
 * any test class is loaded.
 *
 * <p>Without this, the suite is order-dependent and cascades: a test with no toolkit setup (e.g.
 * {@code BootstrapServiceTest}) runs first, touches a JavaFX {@code Control} while the toolkit is
 * down, and that control's static initializer fails <em>permanently</em> for the whole JVM — every
 * later test using it then dies with {@code NoClassDefFoundError: Could not initialize class
 * javafx.scene.control.*}. Starting the toolkit before any test class loads removes the root cause,
 * so per-class {@code Platform.startup()} guards become harmless no-ops (they already catch the
 * "already started" {@link IllegalStateException}).</p>
 *
 * <p>Registered via {@code META-INF/services/org.junit.platform.launcher.LauncherSessionListener};
 * the JUnit Platform launcher auto-loads it. In a genuinely headless environment with no display
 * this is a no-op — it never makes things worse than the pre-existing per-test startup.</p>
 */
public final class JavaFxTestToolkit implements LauncherSessionListener {

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        boolean toolkitUp = false;
        try {
            CountDownLatch ready = new CountDownLatch(1);
            Platform.startup(ready::countDown);
            ready.await(10, TimeUnit.SECONDS);
            toolkitUp = true;
        } catch (IllegalStateException alreadyStarted) {
            toolkitUp = true;   // a test already started it — fine
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException headlessOrMissingDisplay) {
            // No display / toolkit unavailable — leave it; FX-dependent tests behave as before.
        }
        if (toolkitUp) {
            // Keep the toolkit alive across tests even if a test closes its last window.
            Platform.setImplicitExit(false);
        }
    }
}
