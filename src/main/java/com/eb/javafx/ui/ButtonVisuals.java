package com.eb.javafx.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared button visual defaults for routed and test screens.
 */
public final class ButtonVisuals {
    public static final String BUTTON_SHAPE_RESOURCE = "/com/eb/javafx/images/svg/button-pill.svg";
    public static final String BUTTON_STYLE_CLASS = "svg-button";
    public static final String BUTTON_ARTWORK_STYLE_CLASS = "svg-button-artwork";
    public static final String BUTTON_ARTWORK_TEXT_STYLE_CLASS = "svg-button-artwork-text";
    public static final double BUTTON_ARTWORK_WIDTH = 180;
    public static final double BUTTON_ARTWORK_HEIGHT = 48;
    private static final Pattern PATH_DATA_PATTERN = Pattern.compile("<path\\b[^>]*\\bd\\s*=\\s*(['\"])(.*?)\\1", Pattern.DOTALL);
    private static final Pattern SAFE_PATH_DATA_PATTERN = Pattern.compile("[MmZzLlHhVvCcSsQqTtAaEe0-9+\\-.,\\s]+");
    private static final System.Logger LOGGER = System.getLogger(ButtonVisuals.class.getName());
    private static final String SHAPE_PATH = loadShapePath();

    private ButtonVisuals() {
    }

    public static Button apply(Button button) {
        SVGPath shape = createShape();
        if (shape != null) {
            button.setShape(shape);
            button.setScaleShape(true);
            button.setCenterShape(false);
        }
        if (!button.getStyleClass().contains(BUTTON_STYLE_CLASS)) {
            button.getStyleClass().add(BUTTON_STYLE_CLASS);
        }
        button.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        button.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return button;
    }

    public static Button applySvgArtwork(Button button) {
        apply(button);
        Node artwork = createArtworkGraphic(button.getText());
        if (artwork != null) {
            button.setGraphic(artwork);
            button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            button.setPrefSize(BUTTON_ARTWORK_WIDTH, BUTTON_ARTWORK_HEIGHT);
            button.setMinSize(BUTTON_ARTWORK_WIDTH, BUTTON_ARTWORK_HEIGHT);
            button.setMaxSize(BUTTON_ARTWORK_WIDTH, BUTTON_ARTWORK_HEIGHT);
            button.setStyle(appendStyle(button.getStyle(),
                    "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;"));
        }
        return button;
    }

    public static String buttonShapePath() {
        return SHAPE_PATH;
    }

    public static SVGPath createShape() {
        if (SHAPE_PATH.isBlank()) {
            return null;
        }
        SVGPath shape = new SVGPath();
        shape.setContent(SHAPE_PATH);
        return shape;
    }

    public static Node createArtworkGraphic(String text) {
        SVGPath artwork = createShape();
        if (artwork == null) {
            return null;
        }
        artwork.setFill(new LinearGradient(
                0,
                0,
                1,
                1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0a1426")),
                new Stop(0.55, Color.web("#143869")),
                new Stop(1, Color.web("#0099cc"))));
        artwork.setStroke(Color.web("#66c1e0"));
        artwork.setStrokeWidth(1.0);

        Text label = new Text(text == null ? "" : text);
        label.getStyleClass().add(BUTTON_ARTWORK_TEXT_STYLE_CLASS);

        StackPane graphic = new StackPane(artwork, label);
        graphic.getStyleClass().add(BUTTON_ARTWORK_STYLE_CLASS);
        graphic.setAlignment(Pos.CENTER);
        return graphic;
    }

    private static String loadShapePath() {
        try (InputStream stream = ButtonVisuals.class.getResourceAsStream(BUTTON_SHAPE_RESOURCE)) {
            if (stream == null) {
                LOGGER.log(System.Logger.Level.WARNING, "Button shape resource is missing: {0}", BUTTON_SHAPE_RESOURCE);
                return "";
            }
            String svg = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = PATH_DATA_PATTERN.matcher(svg);
            if (!matcher.find()) {
                LOGGER.log(System.Logger.Level.WARNING, "Button shape resource does not include path data: {0}", BUTTON_SHAPE_RESOURCE);
                return "";
            }
            String pathData = matcher.group(2);
            if (!SAFE_PATH_DATA_PATTERN.matcher(pathData).matches()) {
                LOGGER.log(System.Logger.Level.WARNING, "Button shape resource contains invalid path data: {0}", BUTTON_SHAPE_RESOURCE);
                return "";
            }
            return pathData;
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Button shape resource failed to load: " + BUTTON_SHAPE_RESOURCE, exception);
            return "";
        }
    }

    private static String appendStyle(String currentStyle, String addition) {
        String style = currentStyle == null ? "" : currentStyle.trim();
        if (style.isEmpty()) {
            return addition;
        }
        if (style.endsWith(";")) {
            return style + " " + addition;
        }
        return style + "; " + addition;
    }
}
