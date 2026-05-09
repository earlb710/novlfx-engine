package com.eb.javafx.ui.test;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.ui.ScreenLayoutModel;
import com.eb.javafx.ui.ScreenLayoutRenderer;
import com.eb.javafx.ui.ScreenLayoutSection;
import com.eb.javafx.ui.ScreenLayoutType;
import com.eb.javafx.ui.UiTheme;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Map;

/**
 * Manual route that previews block background SVG images layered over a screen SVG background.
 */
public final class BlockBackgroundImageTestScreen {
    static final String SCREEN_BACKGROUND_RESOURCE = "/com/eb/javafx/images/svg/circle-background.svg";
    static final String BLOCK_BACKGROUND_RESOURCE = "/com/eb/javafx/images/svg/circle2-background.svg";
    static final String SCREEN_BACKGROUND_RESOURCE_KEY = "screenBackgroundImage";
    static final String SCREEN_BACKGROUND_COLOR = "#08111f";
    static final String LEFT_BLOCK_BACKGROUND_COLOR = "#203a67";
    static final String RIGHT_BLOCK_BACKGROUND_COLOR = "#5a2b5f";
    static final String BLOCK_TEXT_COLOR = "#ffffff";
    static final String BACKGROUND_TRANSPARENCY = "0.5";
    static final double BACKGROUND_OPACITY = 0.5;
    private static final String TRANSPARENT_BACKGROUND_STYLE = "-fx-background-color: transparent;";

    private BlockBackgroundImageTestScreen() {
    }

    public static Scene createScene(String title, PreferencesService preferencesService, UiTheme uiTheme) {
        StackPane root = createRoot(title);
        Scene scene = new Scene(root, TestUiScreenSize.sceneWidth(preferencesService), TestUiScreenSize.sceneHeight(preferencesService));
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }

    static StackPane createRoot(String title) {
        StackPane root = new StackPane();
        Region background = createBackgroundLayer(root);
        BorderPane content = ScreenLayoutRenderer.createRoot(createModel(title));
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        content.setStyle(TRANSPARENT_BACKGROUND_STYLE);
        if (content.getCenter() instanceof Region contentPanel) {
            contentPanel.setStyle(TRANSPARENT_BACKGROUND_STYLE);
        }
        root.getChildren().addAll(background, content);
        return root;
    }

    static ScreenLayoutModel createModel(String title) {
        return new ScreenLayoutModel(
                ScreenLayoutType.FORM,
                title,
                "Screen background plus two block background image samples.",
                List.of(new ScreenLayoutSection(
                        "block-background-image-demo",
                        "Block background image preview",
                        List.of(
                                "Outside item: the full screen uses circle-background.svg at 50% transparency.",
                                "Outside item: the two blocks below use circle2-background.svg at 50% transparency."),
                        "block-background-image-demo",
                        Map.of(),
                        List.of("outside.screen", "outside.blocks"),
                        List.of(
                                Map.of("fontStyle", "bold"),
                                Map.of()),
                        ScreenLayoutType.TWO_COLUMN,
                        List.of(
                                blockSection(
                                        "left-block",
                                        "Left block",
                                        LEFT_BLOCK_BACKGROUND_COLOR,
                                        List.of(
                                                "Inside item: left block background image is circle2-background.svg.",
                                                "Inside item: left block image transparency is 50%.",
                                                "Inside item: left block text stays readable with white labels over the artwork.",
                                                "Inside item: left block clipping keeps the SVG image inside the rounded border.",
                                                "Open left action",
                                                "Confirm left action")),
                                blockSection(
                                        "right-block",
                                        "Right block",
                                        RIGHT_BLOCK_BACKGROUND_COLOR,
                                        List.of(
                                                "Inside item: right block background image is circle2-background.svg.",
                                                "Inside item: right block image transparency is 50%.",
                                                "Inside item: right block text stays readable with white labels over the artwork.",
                                                "Inside item: right block clipping keeps the SVG image inside the rounded border.",
                                                "Open right action",
                                                "Confirm right action"))))),
                List.of(),
                List.of(),
                List.of(),
                "Use this screen to verify screen-level and block-level SVG background image layering.",
                Map.of(SCREEN_BACKGROUND_RESOURCE_KEY, SCREEN_BACKGROUND_RESOURCE));
    }

    private static ScreenLayoutSection blockSection(
            String id,
            String title,
            String backgroundColor,
            List<String> lines) {
        return new ScreenLayoutSection(
                id,
                title,
                lines,
                "block-background-image-demo-section",
                Map.of(
                        "backgroundColor", backgroundColor,
                        "transparency", "0.15",
                        "backgroundImage", BLOCK_BACKGROUND_RESOURCE,
                        "backgroundImageTransparency", BACKGROUND_TRANSPARENCY,
                        "backgroundImagePlacement", "stretch to fit",
                        "borderStyle", "solid",
                        "borderCorner", "rounded",
                        "borderThickness", "3",
                        "borderColor", "#d7e7ff"),
                List.of(id + ".line1", id + ".line2", id + ".line3", id + ".line4", id + ".button1", id + ".button2"),
                List.of(
                        textMetadata(true),
                        textMetadata(false),
                        textMetadata(false),
                        textMetadata(false),
                        buttonMetadata(id + ".open"),
                        buttonMetadata(id + ".confirm")));
    }

    private static Map<String, String> textMetadata(boolean bold) {
        return bold
                ? Map.of("fontStyle", "bold", "color", BLOCK_TEXT_COLOR)
                : Map.of("color", BLOCK_TEXT_COLOR);
    }

    private static Map<String, String> buttonMetadata(String actionValue) {
        return Map.of(
                "eventName", "blockButtonPreview",
                "actionValue", actionValue,
                "color", BLOCK_TEXT_COLOR,
                "backgroundColor", "#1d2433",
                "transparency", "0.1",
                "fontStyle", "bold");
    }

    private static Region createBackgroundLayer(StackPane root) {
        Region background = com.eb.javafx.ui.ScreenShell.backgroundSvg(
                SCREEN_BACKGROUND_RESOURCE,
                BACKGROUND_OPACITY,
                Color.web(SCREEN_BACKGROUND_COLOR));
        background.setUserData(SCREEN_BACKGROUND_RESOURCE);
        background.prefWidthProperty().bind(root.widthProperty());
        background.prefHeightProperty().bind(root.heightProperty());
        background.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return background;
    }
}
