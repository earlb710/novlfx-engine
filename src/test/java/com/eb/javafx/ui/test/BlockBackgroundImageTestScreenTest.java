package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import com.eb.javafx.ui.ScreenLayoutModel;
import com.eb.javafx.ui.ScreenLayoutSection;
import com.eb.javafx.ui.ScreenLayoutType;
import com.eb.javafx.ui.ScreenShell;
import com.eb.javafx.ui.UiTheme;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class BlockBackgroundImageTestScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void modelUsesScreenBackgroundAndTwoBlocksWithBlockBackgroundImages() {
        ScreenLayoutModel model = BlockBackgroundImageTestScreen.createModel("Block Background Image Test");

        assertEquals(ScreenLayoutType.FORM, model.type());
        assertEquals("Block Background Image Test", model.title());
        assertEquals(1, model.contentSections().size());
        ScreenLayoutSection outerSection = model.contentSections().get(0);
        assertEquals(2, outerSection.lines().size());
        assertEquals(ScreenLayoutType.TWO_COLUMN, outerSection.layoutType());
        assertEquals(2, outerSection.childSections().size());
        for (ScreenLayoutSection block : outerSection.childSections()) {
            assertEquals(BlockBackgroundImageTestScreen.BLOCK_BACKGROUND_RESOURCE, block.metadata().get("backgroundImage"));
            assertEquals(BlockBackgroundImageTestScreen.BACKGROUND_TRANSPARENCY, block.metadata().get("backgroundImageTransparency"));
        }
    }

    @Test
    void createRootLayersScreenBackgroundBehindRenderedLayoutAndBlockBackgrounds() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX root inspection requires a display.");
        runOnJavaFxThread(() -> {
            StackPane root = BlockBackgroundImageTestScreen.createRoot("Block Background Image Test");

            assertEquals(2, root.getChildren().size());
            Region background = assertInstanceOf(Region.class, root.getChildren().get(0));
            BorderPane content = assertInstanceOf(BorderPane.class, root.getChildren().get(1));
            ImageView screenBackgroundImage = assertInstanceOf(ImageView.class, background.getChildrenUnmodifiable().get(0));

            assertTrue(background.getStyleClass().contains(ScreenShell.SCREEN_BACKGROUND_SVG_STYLE_CLASS));
            assertEquals(BlockBackgroundImageTestScreen.BACKGROUND_OPACITY, screenBackgroundImage.getOpacity(), 0.0001);

            List<StackPane> layeredSections = new ArrayList<>();
            collectNodes(content, StackPane.class, layeredSections);
            long blockBackgroundLayers = layeredSections.stream()
                    .filter(stackPane -> stackPane.getChildren().size() == 2
                            && stackPane.getChildren().get(0) instanceof Region region
                            && region.getChildrenUnmodifiable().size() == 1
                            && region.getChildrenUnmodifiable().get(0) instanceof ImageView imageView
                            && Math.abs(imageView.getOpacity() - BlockBackgroundImageTestScreen.BACKGROUND_OPACITY) < 0.0001)
                    .count();

            assertTrue(blockBackgroundLayers >= 2, "Expected at least two layered block background sections.");
            VBox body = assertInstanceOf(VBox.class, content.getCenter());
            assertTrue(body.getChildren().size() >= 1);
        });
    }

    @Test
    @ManualTest
    void runBlockBackgroundImageTestScreenFromTestApp() throws Exception {
        assumeTrue(Boolean.getBoolean(TestScreenApplication.TEST_SCREEN_ACTIVE_PROPERTY),
                "Manual JavaFX screen test runs only from TestScreenApplication.");

        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            stage.setTitle("BlockBackgroundImageTestScreen manual test");
            stage.setScene(BlockBackgroundImageTestScreen.createScene(
                    "Block Background Image Test",
                    preferencesService,
                    uiTheme));
            stage.show();
            assertTrue(stage.isShowing() && stage.getScene() != null,
                    "BlockBackgroundImageTestScreen window was not shown.");
        });
    }

    private static <T extends Node> void collectNodes(Node node, Class<T> type, List<T> nodes) {
        if (type.isInstance(node)) {
            nodes.add(type.cast(node));
        }
        if (node instanceof Region region) {
            for (Node child : region.getChildrenUnmodifiable()) {
                collectNodes(child, type, nodes);
            }
        }
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
