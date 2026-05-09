package com.eb.javafx.ui.test;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class BlockBackgroundImageTestScreenTest {
    private static final long JAVAFX_OPERATION_TIMEOUT_SECONDS = 5;
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @Test
    void modelUsesScreenBackgroundAndTwoBlocksWithBlockBackgroundImages() {
        ScreenLayoutModel model = BlockBackgroundImageTestScreen.createModel("Block Background Image Test");

        assertEquals(ScreenLayoutType.FORM, model.type());
        assertEquals("Block Background Image Test", model.title());
        assertEquals(BlockBackgroundImageTestScreen.SCREEN_BACKGROUND_RESOURCE,
                model.metadata().get(BlockBackgroundImageTestScreen.SCREEN_BACKGROUND_RESOURCE_KEY));
        assertEquals(1, model.contentSections().size());
        ScreenLayoutSection outerSection = model.contentSections().get(0);
        assertEquals(2, outerSection.lines().size());
        assertEquals(ScreenLayoutType.TWO_COLUMN, outerSection.layoutType());
        assertEquals(2, outerSection.childSections().size());
        for (ScreenLayoutSection block : outerSection.childSections()) {
            assertEquals(6, block.lines().size());
            assertEquals(BlockBackgroundImageTestScreen.BLOCK_BACKGROUND_RESOURCE, block.metadata().get("backgroundImage"));
            assertEquals(BlockBackgroundImageTestScreen.BACKGROUND_TRANSPARENCY, block.metadata().get("backgroundImageTransparency"));
            assertEquals("3", block.metadata().get("borderThickness"));
            assertEquals("pill", block.metadata().get("borderCorner"));
            assertEquals(BlockBackgroundImageTestScreen.BLOCK_TEXT_COLOR, block.lineMetadata().get(0).get("color"));
            assertEquals(BlockBackgroundImageTestScreen.BLOCK_TEXT_COLOR, block.lineMetadata().get(3).get("color"));
            assertEquals("blockButtonPreview", block.lineMetadata().get(4).get("eventName"));
            assertEquals(BlockBackgroundImageTestScreen.BLOCK_TEXT_COLOR, block.lineMetadata().get(4).get("color"));
        }
    }

    @Test
    void createRootLayersScreenBackgroundBehindRenderedLayoutAndBlockBackgrounds() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX root inspection requires a display.");
        runOnJavaFxThread(() -> {
            StackPane root = BlockBackgroundImageTestScreen.createRoot("Block Background Image Test");
            root.resize(640, 360);
            root.layout();

            assertEquals(2, root.getChildren().size());
            Region background = assertInstanceOf(Region.class, root.getChildren().get(0));
            BorderPane content = assertInstanceOf(BorderPane.class, root.getChildren().get(1));
            ImageView screenBackgroundImage = assertInstanceOf(ImageView.class, background.getChildrenUnmodifiable().get(0));

            assertTrue(background.getStyleClass().contains(ScreenShell.SCREEN_BACKGROUND_SVG_STYLE_CLASS));
            assertEquals(BlockBackgroundImageTestScreen.SCREEN_BACKGROUND_RESOURCE, background.getUserData());
            assertEquals(BlockBackgroundImageTestScreen.BACKGROUND_OPACITY, screenBackgroundImage.getOpacity(), 0.0001);
            assertTrue(content.getStyle().contains("transparent"));
            assertTrue(assertInstanceOf(Region.class, content.getCenter()).getStyle().contains("transparent"));

            List<StackPane> layeredSections = new ArrayList<>();
            collectNodes(content, StackPane.class, layeredSections);
            long blockBackgroundLayers = layeredSections.stream()
                    .filter(BlockBackgroundImageTestScreenTest::isBlockBackgroundLayer)
                    .count();
            List<Button> buttons = new ArrayList<>();
            collectNodes(content, Button.class, buttons);

            assertTrue(blockBackgroundLayers >= 2, "Expected at least two layered block background sections.");
            assertTrue(buttons.size() >= 4, "Expected block action buttons to render inside the blocks.");
            assertTrue(layeredSections.stream().filter(BlockBackgroundImageTestScreenTest::isBlockBackgroundLayer)
                            .allMatch(BlockBackgroundImageTestScreenTest::hasRoundedBackgroundClip),
                    "Expected block background images to be clipped to the rounded border shape.");
            assertTrue(layeredSections.stream().filter(BlockBackgroundImageTestScreenTest::isBlockBackgroundLayer)
                            .allMatch(BlockBackgroundImageTestScreenTest::pillClipMatchesRegionSize),
                    "Expected block background clips to match the initial rendered size.");
            root.resize(960, 540);
            root.layout();
            assertTrue(layeredSections.stream().filter(BlockBackgroundImageTestScreenTest::isBlockBackgroundLayer)
                            .allMatch(BlockBackgroundImageTestScreenTest::pillClipMatchesRegionSize),
                    "Expected block background clips to update after the block resizes.");
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

    private static boolean isBlockBackgroundLayer(StackPane stackPane) {
        return stackPane.getChildren().size() == 2
                && stackPane.getChildren().get(0) instanceof Region region
                && region.getChildrenUnmodifiable().size() == 1
                && region.getChildrenUnmodifiable().get(0) instanceof ImageView imageView
                && Math.abs(imageView.getOpacity() - BlockBackgroundImageTestScreen.BACKGROUND_OPACITY) < 0.0001;
    }

    private static boolean hasRoundedBackgroundClip(StackPane stackPane) {
        if (!(stackPane.getClip() instanceof Rectangle clip)) {
            return false;
        }
        return clip.getArcWidth() > 0
                && clip.getArcHeight() > 0;
    }

    private static boolean pillClipMatchesRegionSize(StackPane stackPane) {
        if (!(stackPane.getClip() instanceof Rectangle clip)) {
            return false;
        }
        return Math.abs(clip.getWidth() - stackPane.getWidth()) < 0.0001
                && Math.abs(clip.getHeight() - stackPane.getHeight()) < 0.0001
                && Math.abs(clip.getArcWidth() - stackPane.getWidth()) < 0.0001
                && Math.abs(clip.getArcHeight() - stackPane.getHeight()) < 0.0001;
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
        assertTrue(completed.await(JAVAFX_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS), "JavaFX action did not complete.");
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
        assertTrue(started.await(JAVAFX_OPERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS), "JavaFX toolkit did not start.");
    }
}
