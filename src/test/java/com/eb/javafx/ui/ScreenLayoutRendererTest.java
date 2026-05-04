package com.eb.javafx.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class ScreenLayoutRendererTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();
    private static final AtomicBoolean JAVAFX_AVAILABLE = new AtomicBoolean(true);

    @Test
    void rendersTwoColumnLayoutWithSemanticStyleClasses() throws Exception {
        ScreenLayoutModel model = new ScreenLayoutModel(
                ScreenLayoutType.TWO_COLUMN,
                "Equipment",
                "Compare loadouts",
                List.of(
                        new ScreenLayoutSection("left", "Current", List.of("Sword"), "custom-left"),
                        new ScreenLayoutSection("right", "Preview", List.of("Axe"))),
                List.of(new ScreenActionViewModel("Equip", "equip", true)),
                List.of(new ScreenActionViewModel("Cancel", "cancel", false)),
                List.of(),
                "Footer");

        runOnJavaFxThread(() -> {
            BorderPane root = ScreenLayoutRenderer.createRoot(model);
            VBox panel = (VBox) root.getCenter();
            VBox content = (VBox) panel.getChildren().get(0);
            HBox columns = (HBox) content.getChildren().get(1);
            VBox firstColumn = (VBox) columns.getChildren().get(0);
            VBox secondaryActions = (VBox) content.getChildren().get(3);
            Button disabledSecondary = (Button) secondaryActions.getChildren().get(0);

            assertTrue(root.getStyleClass().contains(ScreenShell.SCREEN_ROOT_STYLE_CLASS));
            assertTrue(panel.getStyleClass().contains(ScreenShell.SCREEN_PANEL_STYLE_CLASS));
            assertTrue(content.getStyleClass().contains(ScreenShell.LAYOUT_CONTENT_STYLE_CLASS));
            assertTrue(columns.getStyleClass().contains(ScreenShell.LAYOUT_TWO_COLUMN_STYLE_CLASS));
            assertTrue(firstColumn.getStyleClass().contains(ScreenShell.LAYOUT_COLUMN_STYLE_CLASS));
            assertTrue(firstColumn.getStyleClass().contains("custom-left"));
            assertTrue(disabledSecondary.isDisabled());
        });
    }

    @Test
    void rendersSidebarEntriesSeparatelyFromMainContent() throws Exception {
        ScreenLayoutModel model = new ScreenLayoutModel(
                ScreenLayoutType.SIDEBAR_CONTENT,
                "Codex",
                null,
                List.of(new ScreenLayoutSection("entry", "Entry", List.of("Details"))),
                List.of(),
                List.of(),
                List.of(new ScreenActionViewModel("Overview", "overview", true)),
                null);

        runOnJavaFxThread(() -> {
            BorderPane root = ScreenLayoutRenderer.createRoot(model);
            VBox panel = (VBox) root.getCenter();
            VBox content = (VBox) panel.getChildren().get(0);
            HBox sidebarLayout = (HBox) content.getChildren().get(0);
            VBox sidebar = (VBox) sidebarLayout.getChildren().get(0);
            Button entry = (Button) sidebar.getChildren().get(0);

            assertTrue(sidebarLayout.getStyleClass().contains(ScreenShell.LAYOUT_SIDEBAR_CONTENT_STYLE_CLASS));
            assertTrue(sidebar.getStyleClass().contains(ScreenShell.LAYOUT_SIDEBAR_STYLE_CLASS));
            assertTrue(entry.getStyleClass().contains(ScreenShell.LAYOUT_SIDEBAR_ENTRY_STYLE_CLASS));
            assertEquals("Overview", entry.getText());
        });
    }

    private static void runOnJavaFxThread(Runnable action) throws Exception {
        assumeTrue(startJavaFxToolkit());
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
