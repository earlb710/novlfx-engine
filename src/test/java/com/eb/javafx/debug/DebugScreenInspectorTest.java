package com.eb.javafx.debug;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class DebugScreenInspectorTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();
    private static final AtomicBoolean JAVAFX_AVAILABLE = new AtomicBoolean(true);

    @Test
    void readInfoOnFreshSceneReturnsBlankFields() throws Exception {
        assumeTrue(startJavaFxToolkit());

        Scene scene = runOnFxThread(() -> new Scene(new StackPane(), 100, 100));

        DebugScreenInfo info = DebugScreenInspector.readInfo(scene);
        assertEquals("", info.routeId());
        assertEquals("", info.screenClass());
        assertEquals("", info.jsonFilePath());
    }

    @Test
    void attachStoresRouteIdEvenWhenDebugDisabled() throws Exception {
        assumeTrue(startJavaFxToolkit());

        Scene scene = runOnFxThread(() -> new Scene(new StackPane(), 100, 100));
        runOnFxThread(() -> {
            DebugScreenInspector.attach(scene, "preferences", false, null);
            return null;
        });

        DebugScreenInfo info = DebugScreenInspector.readInfo(scene);
        assertEquals("preferences", info.routeId());
    }

    @Test
    void attachRecordsRouteIdWhenDebugEnabled() throws Exception {
        assumeTrue(startJavaFxToolkit());

        Scene scene = runOnFxThread(() -> new Scene(new StackPane(), 100, 100));
        runOnFxThread(() -> {
            DebugScreenInspector.attach(scene, "main-menu", true, null);
            return null;
        });

        assertEquals("main-menu", DebugScreenInspector.readInfo(scene).routeId());
    }

    @Test
    void setScreenClassAndJsonFilePathAreVisibleInReadInfo() throws Exception {
        assumeTrue(startJavaFxToolkit());

        Scene scene = runOnFxThread(() -> new Scene(new StackPane(), 100, 100));
        runOnFxThread(() -> {
            DebugScreenInspector.attach(scene, "save-load", false, null);
            DebugScreenInspector.setScreenClass(scene, DebugScreenInspectorTest.class);
            DebugScreenInspector.setJsonFilePath(scene, Path.of("screens", "save_load.json"));
            return null;
        });

        DebugScreenInfo info = DebugScreenInspector.readInfo(scene);
        assertEquals("save-load", info.routeId());
        assertEquals(DebugScreenInspectorTest.class.getName(), info.screenClass());
        assertEquals(Path.of("screens", "save_load.json").toString(), info.jsonFilePath());
    }

    @Test
    void attachIsSafeOnNullScene() {
        DebugScreenInspector.attach(null, "any", true, null);
    }

    @Test
    void buildDialogSceneRendersFieldsWithStableLabels() throws Exception {
        assumeTrue(startJavaFxToolkit());

        Scene dialogScene = runOnFxThread(() -> DebugScreenInspector.buildDialogScene(
                new javafx.stage.Stage(),
                new DebugScreenInfo("main-menu", "com.eb.Foo", "screens/main.json"),
                null));

        assertNotNull(dialogScene);
        String sceneText = dialogScene.getRoot().toString();
        // The exact toString isn't asserted; we verify the labels by walking children.
        assertTrue(containsText(dialogScene, DebugScreenInspector.ROUTE_ID_LABEL + ":"));
        assertTrue(containsText(dialogScene, DebugScreenInspector.SCREEN_CLASS_LABEL + ":"));
        assertTrue(containsText(dialogScene, DebugScreenInspector.JSON_FILE_LABEL + ":"));
        assertTrue(containsTextFieldValue(dialogScene, "main-menu"));
        assertTrue(containsTextFieldValue(dialogScene, "com.eb.Foo"));
        assertTrue(containsTextFieldValue(dialogScene, "screens/main.json"));
    }

    @Test
    void buildDialogSceneShowsUnknownPlaceholderForBlankFields() throws Exception {
        assumeTrue(startJavaFxToolkit());

        Scene dialogScene = runOnFxThread(() -> DebugScreenInspector.buildDialogScene(
                new javafx.stage.Stage(),
                DebugScreenInfo.empty(),
                null));

        assertTrue(countTextFieldValue(dialogScene, DebugScreenInspector.UNKNOWN_VALUE) >= 3);
    }

    private static boolean containsText(Scene scene, String text) {
        return walk(scene.getRoot()).stream().anyMatch(node ->
                node instanceof javafx.scene.control.Label label && text.equals(label.getText()));
    }

    private static boolean containsTextFieldValue(Scene scene, String text) {
        return countTextFieldValue(scene, text) > 0;
    }

    private static int countTextFieldValue(Scene scene, String text) {
        return (int) walk(scene.getRoot()).stream()
                .filter(node -> node instanceof javafx.scene.control.TextField field && text.equals(field.getText()))
                .count();
    }

    private static java.util.List<javafx.scene.Node> walk(javafx.scene.Node node) {
        java.util.List<javafx.scene.Node> result = new java.util.ArrayList<>();
        result.add(node);
        if (node instanceof javafx.scene.Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                result.addAll(walk(child));
            }
        }
        return result;
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
