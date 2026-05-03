package com.eb.javafx.ui;

import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import com.eb.javafx.util.UtilJavaFx;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;
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

final class ManBaseMeshTestScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void loadModelLoadsObjFile() throws Exception {
        Path objFile = Files.createTempFile("man-base-mesh-", ".obj");
        Files.writeString(objFile, """
                v 0 0 0
                v 1 0 0
                v 1 1 0
                v 0 1 0
                f 1//1 2//2 3//3 4//4
                """);
        objFile.toFile().deleteOnExit();

        Node model = ManBaseMeshTestScreen.loadModel(objFile.toUri().toURL());

        assertTrue(model instanceof Group);
        assertTrue(((Group) model).getChildren().size() > 0);
    }

    @Test
    void loadModelLoadsMayaAsciiFile() throws Exception {
        Path mayaFile = Files.createTempFile("man-base-mesh-", ".ma");
        Files.writeString(mayaFile, """
                //Maya ASCII 2020 scene
                requires maya "2020";
                createNode transform -n "triangle";
                createNode mesh -n "triangleShape" -p "triangle";
                setAttr ".vt[0:2]" -type "float3" 0 0 0 1 0 0 0 1 0;
                setAttr ".ed[0:2]" -type "int3" 0 1 1 1 2 1 2 0 1;
                setAttr ".uvst[0].uvsp[0:2]" -type "float2" 0 0 1 0 0 1;
                setAttr ".cuvs" -type "string" "map1";
                setAttr ".uvst[0].uvsn" -type "string" "map1";
                setAttr ".fc[0]" -type "polyFaces" f 3 0 1 2 mu 0 3 0 1 2;
                """);
        mayaFile.toFile().deleteOnExit();

        Node model = ManBaseMeshTestScreen.loadModel(mayaFile.toUri().toURL());

        assertTrue(model instanceof Group);
        assertTrue(((Group) model).getChildren().size() > 0);
    }

    @Test
    void manBaseMeshResourceLoadsAsNodeHierarchy() throws Exception {
        Node model = ManBaseMeshTestScreen.loadModelResource();

        assertTrue(model instanceof Group);
        assertTrue(((Group) model).getChildren().size() > 0);
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
