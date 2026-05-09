package com.eb.javafx.ui.test;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.testscreen.ManualTest;
import com.eb.javafx.testscreen.TestScreenApplication;
import com.eb.javafx.ui.ScreenDesignJson;
import com.eb.javafx.ui.ScreenDesignLayoutAdapter;
import com.eb.javafx.ui.ScreenLayoutModel;
import com.eb.javafx.ui.ScreenLayoutRenderer;
import com.eb.javafx.ui.ScreenLayoutSection;
import com.eb.javafx.ui.ScreenLayoutType;
import com.eb.javafx.ui.ScreenShell;
import com.eb.javafx.ui.UiTheme;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class JsonBlockBackgroundImageTestScreenTest {
    private static final long JAVAFX_OPERATION_TIMEOUT_SECONDS = 5;
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();
    private static final String DESIGN_RESOURCE = "block-background-image-screen-design.json";
    private static final String SCREEN_BACKGROUND_RESOURCE_KEY = "screenBackgroundImage";
    private static final String SCREEN_BACKGROUND_TRANSPARENCY_KEY = "screenBackgroundImageTransparency";
    private static final String SCREEN_BACKGROUND_RESOURCE = "/com/eb/javafx/images/svg/circle-background.svg";
    private static final String BLOCK_BACKGROUND_RESOURCE = "/com/eb/javafx/images/svg/circle2-background.svg";
    private static final String SCREEN_BACKGROUND_COLOR = "#08111f";
    private static final String BLOCK_TEXT_COLOR = "#ffffff";
    private static final double BACKGROUND_OPACITY = 0.5;
    private static final String TRANSPARENT_BACKGROUND_STYLE = "-fx-background-color: transparent;";
    private static final String PLACEMENT_TEST_BACKGROUND = "/com/eb/javafx/ui/test/background-placement-test.svg";

    @Test
    void modelUsesJsonScreenBackgroundAndTwoBlocksWithBlockBackgroundImages() {
        ScreenLayoutModel model = loadLayoutModel();

        assertEquals(ScreenLayoutType.FORM, model.type());
        assertEquals("JSON Block Background Image Test", model.title());
        assertEquals(SCREEN_BACKGROUND_RESOURCE, model.metadata().get(SCREEN_BACKGROUND_RESOURCE_KEY));
        assertEquals(Double.toString(BACKGROUND_OPACITY), model.metadata().get(SCREEN_BACKGROUND_TRANSPARENCY_KEY));
        assertEquals(SCREEN_BACKGROUND_COLOR, model.metadata().get("backgroundColor"));
        assertEquals(1, model.contentSections().size());
        ScreenLayoutSection outerSection = model.contentSections().get(0);
        assertEquals(2, outerSection.lines().size());
        assertEquals(ScreenLayoutType.TWO_COLUMN, outerSection.layoutType());
        assertEquals(2, outerSection.childSections().size());
        for (ScreenLayoutSection block : outerSection.childSections()) {
            assertEquals(6, block.lines().size());
            assertEquals(BLOCK_BACKGROUND_RESOURCE, block.metadata().get("backgroundImage"));
            assertEquals(Double.toString(BACKGROUND_OPACITY), block.metadata().get("backgroundImageTransparency"));
            assertEquals("stretch to fit", block.metadata().get("backgroundImagePlacement"));
            assertEquals("3", block.metadata().get("borderThickness"));
            assertEquals("rounded", block.metadata().get("borderCorner"));
            assertEquals(BLOCK_TEXT_COLOR, block.lineMetadata().get(0).get("color"));
            assertEquals(BLOCK_TEXT_COLOR, block.lineMetadata().get(3).get("color"));
            assertEquals("blockButtonPreview", block.lineMetadata().get(4).get("eventName"));
            assertEquals(BLOCK_TEXT_COLOR, block.lineMetadata().get(4).get("color"));
        }
    }

    @Test
    void createRootLayersJsonScreenBackgroundBehindRenderedLayoutAndBlockBackgrounds() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX root inspection requires a display.");
        runOnJavaFxThread(() -> {
            StackPane root = createRoot();
            root.resize(640, 360);
            root.layout();

            assertEquals(2, root.getChildren().size());
            Region background = assertInstanceOf(Region.class, root.getChildren().get(0));
            BorderPane content = assertInstanceOf(BorderPane.class, root.getChildren().get(1));
            ImageView screenBackgroundImage = assertInstanceOf(ImageView.class, background.getChildrenUnmodifiable().get(0));

            assertTrue(background.getStyleClass().contains(ScreenShell.SCREEN_BACKGROUND_SVG_STYLE_CLASS));
            assertEquals(SCREEN_BACKGROUND_RESOURCE, background.getUserData());
            assertEquals(BACKGROUND_OPACITY, screenBackgroundImage.getOpacity(), 0.0001);
            assertTrue(content.getStyle().contains("transparent"));
            assertTrue(assertInstanceOf(Region.class, content.getCenter()).getStyle().contains("transparent"));

            List<StackPane> layeredSections = new ArrayList<>();
            collectNodes(content, StackPane.class, layeredSections);
            long blockBackgroundLayers = layeredSections.stream()
                    .filter(JsonBlockBackgroundImageTestScreenTest::isBlockBackgroundLayer)
                    .count();
            List<Button> buttons = new ArrayList<>();
            collectNodes(content, Button.class, buttons);

            assertTrue(blockBackgroundLayers >= 2, "Expected at least two layered block background sections.");
            assertTrue(buttons.size() >= 4, "Expected block action buttons to render inside the blocks.");
            assertTrue(layeredSections.stream().filter(JsonBlockBackgroundImageTestScreenTest::isBlockBackgroundLayer)
                            .allMatch(JsonBlockBackgroundImageTestScreenTest::hasRoundedBackgroundClip),
                    "Expected block background images to be clipped to the rounded border shape.");
            assertTrue(layeredSections.stream().filter(JsonBlockBackgroundImageTestScreenTest::isBlockBackgroundLayer)
                            .allMatch(JsonBlockBackgroundImageTestScreenTest::clipMatchesRegionSize),
                    "Expected block background clips to match the initial rendered size.");
            assertTrue(layeredSections.stream().filter(JsonBlockBackgroundImageTestScreenTest::isBlockBackgroundLayer)
                            .allMatch(JsonBlockBackgroundImageTestScreenTest::hasVisibleBorderWrapper),
                    "Expected the border wrapper to remain unclipped around the background image.");
            assertTrue(layeredSections.stream().filter(JsonBlockBackgroundImageTestScreenTest::isBlockBackgroundLayer)
                            .allMatch(JsonBlockBackgroundImageTestScreenTest::usesRoundedInsteadOfPillClip),
                    "Expected the sample blocks to use rounded corners instead of pill corners.");
            root.resize(960, 540);
            root.layout();
            assertTrue(layeredSections.stream().filter(JsonBlockBackgroundImageTestScreenTest::isBlockBackgroundLayer)
                            .allMatch(JsonBlockBackgroundImageTestScreenTest::clipMatchesRegionSize),
                    "Expected block background clips to update after the block resizes.");
            VBox body = assertInstanceOf(VBox.class, content.getCenter());
            assertTrue(body.getChildren().size() >= 1);
        });
    }

    @Test
    void rendererSupportsFixedAndStretchBlockBackgroundImagePlacementsFromJson() throws Exception {
        assumeTrue(!GraphicsEnvironment.isHeadless(), "JavaFX root inspection requires a display.");
        runOnJavaFxThread(() -> {
            assertBackgroundPlacement("fixed top left");
            assertBackgroundPlacement("fixed center");
            assertBackgroundPlacement("fixed bottom right");
            assertBackgroundPlacement("stretch to fit");
        });
    }

    @Test
    @ManualTest
    void runJsonBackedBlockBackgroundImageScreenFromTestApp() throws Exception {
        assumeTrue(Boolean.getBoolean(TestScreenApplication.TEST_SCREEN_ACTIVE_PROPERTY),
                "Manual JavaFX screen test runs only from TestScreenApplication.");

        runOnJavaFxThread(() -> {
            PreferencesService preferencesService = new PreferencesService();
            preferencesService.load();

            UiTheme uiTheme = new UiTheme();
            uiTheme.initialize(preferencesService);

            Stage stage = new Stage();
            stage.setTitle("JsonBlockBackgroundImageTestScreen manual test");
            stage.setScene(createScene(preferencesService, uiTheme));
            stage.show();
            assertTrue(stage.isShowing() && stage.getScene() != null,
                    "JsonBlockBackgroundImageTestScreen window was not shown.");
        });
    }

    private static Scene createScene(PreferencesService preferencesService, UiTheme uiTheme) {
        Scene scene = new Scene(createRoot(), TestUiScreenSize.sceneWidth(preferencesService), TestUiScreenSize.sceneHeight(preferencesService));
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }

    private static StackPane createRoot() {
        ScreenLayoutModel model = loadLayoutModel();
        StackPane root = new StackPane();
        Region background = createBackgroundLayer(root, model);
        BorderPane content = ScreenLayoutRenderer.createRoot(model);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.setStyle(TRANSPARENT_BACKGROUND_STYLE);
        if (content.getCenter() instanceof Region contentPanel) {
            contentPanel.setStyle(TRANSPARENT_BACKGROUND_STYLE);
        }
        root.getChildren().addAll(background, content);
        return root;
    }

    private static ScreenLayoutModel loadLayoutModel() {
        return ScreenDesignLayoutAdapter.toLayoutModel(ScreenDesignJson.fromJson(loadDesignJson(), DESIGN_RESOURCE));
    }

    private static String loadDesignJson() {
        try (InputStream stream = JsonBlockBackgroundImageTestScreenTest.class.getResourceAsStream(DESIGN_RESOURCE)) {
            assertTrue(stream != null, () -> "Missing test resource " + DESIGN_RESOURCE);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + DESIGN_RESOURCE, exception);
        }
    }

    private static Region createBackgroundLayer(StackPane root, ScreenLayoutModel model) {
        String backgroundResource = model.metadata().get(SCREEN_BACKGROUND_RESOURCE_KEY);
        double opacity = Double.parseDouble(model.metadata().getOrDefault(SCREEN_BACKGROUND_TRANSPARENCY_KEY, "1.0"));
        Region background = ScreenShell.backgroundSvg(
                backgroundResource,
                opacity,
                Color.web(model.metadata().getOrDefault("backgroundColor", SCREEN_BACKGROUND_COLOR)));
        background.setUserData(backgroundResource);
        background.prefWidthProperty().bind(root.widthProperty());
        background.prefHeightProperty().bind(root.heightProperty());
        background.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return background;
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
                && Math.abs(imageView.getOpacity() - BACKGROUND_OPACITY) < 0.0001;
    }

    private static boolean hasRoundedBackgroundClip(StackPane stackPane) {
        if (!(stackPane.getClip() instanceof Rectangle clip)) {
            return false;
        }
        return clip.getArcWidth() > 0
                && clip.getArcHeight() > 0;
    }

    private static boolean clipMatchesRegionSize(StackPane stackPane) {
        if (!(stackPane.getClip() instanceof Rectangle clip)) {
            return false;
        }
        return Math.abs(clip.getWidth() - stackPane.getWidth()) < 0.0001
                && Math.abs(clip.getHeight() - stackPane.getHeight()) < 0.0001
                && clip.getArcWidth() > 0
                && clip.getArcHeight() > 0;
    }

    private static boolean hasVisibleBorderWrapper(StackPane stackPane) {
        if (!(stackPane.getParent() instanceof StackPane wrapper)) {
            return false;
        }
        return wrapper.getClip() == null
                && wrapper.getPadding().getTop() > 0
                && wrapper.getStyle().contains("-fx-border-width");
    }

    private static boolean usesRoundedInsteadOfPillClip(StackPane stackPane) {
        if (!(stackPane.getClip() instanceof Rectangle clip)) {
            return false;
        }
        return clip.getArcWidth() < stackPane.getWidth()
                && clip.getArcHeight() < stackPane.getHeight();
    }

    private static void assertBackgroundPlacement(String placement) {
        BorderPane root = ScreenLayoutRenderer.createRoot(ScreenDesignLayoutAdapter.toLayoutModel(ScreenDesignJson.fromJson(
                placementDesignJson(placement),
                "placement-" + placement + ".json")));
        root.resize(320, 220);
        root.layout();

        StackPane blockLayer = firstBlockBackgroundLayer(root);
        ImageView imageView = backgroundImageView(blockLayer);
        double imageWidth = imageView.getImage().getWidth();
        double imageHeight = imageView.getImage().getHeight();
        if ("stretch to fit".equals(placement)) {
            assertFalse(imageView.isPreserveRatio());
            assertEquals(blockLayer.getWidth(), imageView.getFitWidth(), 0.0001);
            assertEquals(blockLayer.getHeight(), imageView.getFitHeight(), 0.0001);
            assertEquals(0.0, imageView.getLayoutX(), 0.0001);
            assertEquals(0.0, imageView.getLayoutY(), 0.0001);
            return;
        }
        assertTrue(imageView.isPreserveRatio());
        assertEquals(imageWidth, imageView.getFitWidth(), 0.0001);
        assertEquals(imageHeight, imageView.getFitHeight(), 0.0001);
        double expectedX = switch (placement) {
            case "fixed center" -> (blockLayer.getWidth() - imageWidth) / 2.0;
            case "fixed bottom right" -> blockLayer.getWidth() - imageWidth;
            default -> 0.0;
        };
        double expectedY = switch (placement) {
            case "fixed center" -> (blockLayer.getHeight() - imageHeight) / 2.0;
            case "fixed bottom right" -> blockLayer.getHeight() - imageHeight;
            default -> 0.0;
        };
        assertEquals(expectedX, imageView.getLayoutX(), 0.0001);
        assertEquals(expectedY, imageView.getLayoutY(), 0.0001);
    }

    private static String placementDesignJson(String placement) {
        return """
                {
                  "id": "test.block-placement",
                  "title": "Placement Test",
                  "layoutType": "FORM",
                  "metadata": {},
                  "blocks": [
                    {
                      "id": "placement-block",
                      "title": "Placement block",
                      "layoutType": null,
                      "parentBlockId": null,
                      "conditions": [],
                      "styleClass": "placement-block",
                      "metadata": {
                        "backgroundImage": "%s",
                        "backgroundImagePlacement": "%s",
                        "borderStyle": "solid",
                        "borderCorner": "rounded",
                        "borderThickness": "2",
                        "borderColor": "#ffffff"
                      }
                    }
                  ],
                  "items": [
                    {
                      "id": "placement.row",
                      "blockId": "placement-block",
                      "type": "TEXT",
                      "label": null,
                      "text": "Placement preview",
                      "value": null,
                      "defaultValue": null,
                      "sequence": 10,
                      "editable": false,
                      "styleClass": null,
                      "metadata": {}
                    }
                  ]
                }
                """.formatted(PLACEMENT_TEST_BACKGROUND, placement);
    }

    private static StackPane firstBlockBackgroundLayer(Node root) {
        List<StackPane> layeredSections = new ArrayList<>();
        collectNodes(root, StackPane.class, layeredSections);
        return layeredSections.stream()
                .filter(JsonBlockBackgroundImageTestScreenTest::isBlockBackgroundLayer)
                .findFirst()
                .orElseThrow();
    }

    private static ImageView backgroundImageView(StackPane stackPane) {
        Region backgroundRegion = assertInstanceOf(Region.class, stackPane.getChildren().get(0));
        return assertInstanceOf(ImageView.class, backgroundRegion.getChildrenUnmodifiable().get(0));
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
