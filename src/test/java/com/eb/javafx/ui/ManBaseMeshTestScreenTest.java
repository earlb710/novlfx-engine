package com.eb.javafx.ui;

import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import com.eb.javafx.util.UtilJavaFx;
import javafx.application.Platform;
import javafx.scene.shape.TriangleMesh;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class ManBaseMeshTestScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void loadMeshTriangulatesObjFaces() throws Exception {
        TriangleMesh mesh = ManBaseMeshTestScreen.loadMesh(new ByteArrayInputStream("""
                v 0 0 0
                v 1 0 0
                v 1 1 0
                v 0 1 0
                f 1//1 2//2 3//3 4//4
                """.getBytes(StandardCharsets.UTF_8)));

        assertEquals(12, mesh.getPoints().size());
        assertEquals(6, mesh.getFaceElementSize());
        assertEquals(12, mesh.getFaces().size());
    }

    @Test
    void manBaseMeshResourceLoadsAsTriangleMesh() throws Exception {
        TriangleMesh mesh = ManBaseMeshTestScreen.loadMeshResource();

        assertTrue(mesh.getPoints().size() > 0);
        assertTrue(mesh.getFaces().size() > 0);
        assertEquals(6, mesh.getFaceElementSize());
    }

    @Test
    @ManualTest
    void runManBaseMeshTestScreenFromTestApp() throws Exception {
        assumeTrue(Boolean.getBoolean(TestScreenApplication.TEST_SCREEN_ACTIVE_PROPERTY),
                "Manual JavaFX mesh screen test runs only from TestScreenApplication.");

        runOnJavaFxThread(() -> {
            Stage stage = new Stage();
            stage.setTitle("ManBaseMesh.obj manual test");
            stage.setScene(ManBaseMeshTestScreen.createScene(stage::close));
            stage.show();
            assertTrue(stage.isShowing() && stage.getScene() != null, "ManBaseMesh test screen was not shown.");
        });
    }

    private static void runOnJavaFxThread(ThrowingRunnable action) throws Exception {
        startJavaFxToolkit();
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        UtilJavaFx.run(() -> {
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

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
