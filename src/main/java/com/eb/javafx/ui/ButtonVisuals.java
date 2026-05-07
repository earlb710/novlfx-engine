package com.eb.javafx.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Button;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
    private static final Pattern STOP_STYLE_PATTERN = Pattern.compile("<stop\\b[^>]*\\bstyle\\s*=\\s*(['\"])(.*?)\\1", Pattern.DOTALL);
    private static final Pattern STOP_COLOR_PATTERN = Pattern.compile("stop-color\\s*:\\s*([^;]+)");
    private static final Pattern STOP_OPACITY_PATTERN = Pattern.compile("stop-opacity\\s*:\\s*([^;]+)");
    private static final Pattern SAFE_PATH_DATA_PATTERN = Pattern.compile("[MmZzLlHhVvCcSsQqTtAaEe0-9+\\-.,\\s]+");
    private static final System.Logger LOGGER = System.getLogger(ButtonVisuals.class.getName());
    private static final String SHAPE_PATH = loadShapePath();
    private static final Paint ARTWORK_FILL = loadArtworkFill();

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
        artwork.setFill(ARTWORK_FILL);

        Text label = new Text(text == null ? "" : text);
        label.getStyleClass().add(BUTTON_ARTWORK_TEXT_STYLE_CLASS);

        StackPane graphic = new StackPane(artwork, label);
        graphic.getStyleClass().add(BUTTON_ARTWORK_STYLE_CLASS);
        graphic.setAlignment(Pos.CENTER);
        return graphic;
    }

    private static String loadShapePath() {
        String svg = loadSvgResource();
        if (svg.isEmpty()) {
            return "";
        }
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
    }

    private static Paint loadArtworkFill() {
        String svg = loadSvgResource();
        if (svg.isEmpty()) {
            return Color.TRANSPARENT;
        }
        List<Color> stops = loadStopColors(svg);
        if (stops.size() < 4) {
            LOGGER.log(System.Logger.Level.WARNING, "Button artwork resource does not include four mesh stop colors: {0}",
                    BUTTON_SHAPE_RESOURCE);
            return Color.TRANSPARENT;
        }
        return createMeshFill(stops.get(0), stops.get(1), stops.get(2), stops.get(3));
    }

    private static String loadSvgResource() {
        try (InputStream stream = ButtonVisuals.class.getResourceAsStream(BUTTON_SHAPE_RESOURCE)) {
            if (stream == null) {
                LOGGER.log(System.Logger.Level.WARNING, "Button shape resource is missing: {0}", BUTTON_SHAPE_RESOURCE);
                return "";
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Button shape resource failed to load: " + BUTTON_SHAPE_RESOURCE, exception);
            return "";
        }
    }

    private static List<Color> loadStopColors(String svg) {
        List<Color> colors = new ArrayList<>(4);
        Matcher stopMatcher = STOP_STYLE_PATTERN.matcher(svg);
        while (stopMatcher.find() && colors.size() < 4) {
            String style = stopMatcher.group(2);
            Matcher colorMatcher = STOP_COLOR_PATTERN.matcher(style);
            if (!colorMatcher.find()) {
                continue;
            }
            Color color = parseColor(colorMatcher.group(1).trim());
            if (color == null) {
                continue;
            }
            Matcher opacityMatcher = STOP_OPACITY_PATTERN.matcher(style);
            if (opacityMatcher.find()) {
                try {
                    double opacity = Double.parseDouble(opacityMatcher.group(1).trim());
                    color = color.deriveColor(0, 1, 1, opacity);
                } catch (NumberFormatException ignored) {
                    LOGGER.log(System.Logger.Level.WARNING, "Button artwork stop opacity is invalid in {0}",
                            BUTTON_SHAPE_RESOURCE);
                }
            }
            colors.add(color);
        }
        return colors;
    }

    private static Color parseColor(String value) {
        try {
            return Color.web(value);
        } catch (IllegalArgumentException exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Button artwork stop color is invalid in {0}", BUTTON_SHAPE_RESOURCE);
            return null;
        }
    }

    private static Paint createMeshFill(Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        int width = (int) BUTTON_ARTWORK_WIDTH;
        int height = (int) BUTTON_ARTWORK_HEIGHT;
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            double verticalProgress = height == 1 ? 0 : (double) y / (height - 1);
            for (int x = 0; x < width; x++) {
                double horizontalProgress = width == 1 ? 0 : (double) x / (width - 1);
                Color top = topLeft.interpolate(topRight, horizontalProgress);
                Color bottom = bottomLeft.interpolate(bottomRight, horizontalProgress);
                writer.setColor(x, y, top.interpolate(bottom, verticalProgress));
            }
        }
        return new ImagePattern(image, 0, 0, BUTTON_ARTWORK_WIDTH, BUTTON_ARTWORK_HEIGHT, false);
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
