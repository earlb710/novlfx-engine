package com.eb.javafx.ui;

import com.eb.javafx.util.VectorImage;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private static final double BUTTON_ARTWORK_MIN_WIDTH = 48;
    private static final double BUTTON_ARTWORK_HORIZONTAL_PADDING = 36;
    private static final double BUTTON_ARTWORK_VERTICAL_PADDING = 16;
    private static final double BUTTON_ARTWORK_FONT_SIZE = 20;
    private static final int MAX_RASTER_CACHE_SIZE = 128;
    private static final String ARTWORK_FALLBACK_GRADIENT = """
            <linearGradient id="%s" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="0" y2="48">
              <stop offset="0" style="stop-color:#ffffff;stop-opacity:1"/>
              <stop offset="0.5" style="stop-color:#000000;stop-opacity:1"/>
              <stop offset="1" style="stop-color:#ffffff;stop-opacity:1"/>
            </linearGradient>
            """;
    private static final Pattern PATH_DATA_PATTERN = Pattern.compile("<path\\b[^>]*\\bd\\s*=\\s*(['\"])(.*?)\\1", Pattern.DOTALL);
    private static final Pattern SAFE_PATH_DATA_PATTERN = Pattern.compile("[MmZzLlHhVvCcSsQqTtAaEe0-9+\\-.,\\s]+");
    private static final Pattern MESH_GRADIENT_PATTERN = Pattern.compile(
            "<meshgradient\\b[^>]*\\bid\\s*=\\s*(['\"])(.*?)\\1[^>]*>.*?</meshgradient>",
            Pattern.DOTALL);
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script\\b[^>]*>.*?</script>", Pattern.DOTALL);
    private static final System.Logger LOGGER = System.getLogger(ButtonVisuals.class.getName());
    private static final String SHAPE_PATH = loadShapePath();
    private static final String ARTWORK_RESOURCE_URL = loadArtworkResourceUrl();
    private static final String ARTWORK_SVG = loadArtworkSvg();
    private static final Map<RasterSize, Image> RASTER_CACHE = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<RasterSize, Image> eldest) {
            return size() > MAX_RASTER_CACHE_SIZE;
        }
    };

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
        double fixedWidth = positiveSizeOrUnset(button.getPrefWidth());
        double fixedHeight = positiveSizeOrUnset(button.getPrefHeight());
        Node artwork = createArtworkGraphic(button.getText(), fixedWidth, fixedHeight);
        if (artwork != null) {
            button.setGraphic(artwork);
            button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            double targetWidth = fixedWidth > 0 ? fixedWidth : artwork.prefWidth(-1);
            double targetHeight = fixedHeight > 0 ? fixedHeight : artwork.prefHeight(targetWidth);
            button.setPrefSize(targetWidth, targetHeight);
            button.setMinSize(targetWidth, targetHeight);
            button.setMaxSize(targetWidth, targetHeight);
            if (fixedWidth > 0) {
                button.setPrefWidth(fixedWidth);
            }
            if (fixedHeight > 0) {
                button.setPrefHeight(fixedHeight);
            }
            button.setStyle(appendStyle(button.getStyle(),
                    "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;"));
        }
        return button;
    }

    public static String buttonShapePath() {
        return SHAPE_PATH;
    }

    public static String buttonArtworkResourceUrl() {
        return ARTWORK_RESOURCE_URL;
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
        return createArtworkGraphic(text, Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
    }

    public static Node createArtworkGraphic(String text, double width, double height) {
        if (ARTWORK_SVG.isBlank()) {
            return null;
        }
        return new RasterizedArtwork(text, width, height);
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

    private static String loadArtworkResourceUrl() {
        URL resource = ButtonVisuals.class.getResource(BUTTON_SHAPE_RESOURCE);
        if (resource == null) {
            LOGGER.log(System.Logger.Level.WARNING, "Button artwork resource is missing: {0}", BUTTON_SHAPE_RESOURCE);
            return "";
        }
        return resource.toExternalForm();
    }

    private static String loadArtworkSvg() {
        String svg = loadSvgResource();
        if (svg.isEmpty()) {
            return "";
        }
        return prepareArtworkSvgForBatik(svg);
    }

    private static String prepareArtworkSvgForBatik(String svg) {
        Matcher matcher = MESH_GRADIENT_PATTERN.matcher(svg);
        String prepared = svg;
        if (matcher.find()) {
            prepared = matcher.replaceFirst(Matcher.quoteReplacement(ARTWORK_FALLBACK_GRADIENT.formatted(matcher.group(2))));
        }
        return SCRIPT_PATTERN.matcher(prepared).replaceAll("");
    }

    private static Image rasterizeArtwork(int width, int height) {
        if (ARTWORK_SVG.isBlank() || width <= 0 || height <= 0) {
            return null;
        }
        RasterSize size = new RasterSize(width, height);
        synchronized (RASTER_CACHE) {
            Image cached = RASTER_CACHE.get(size);
            if (cached != null) {
                return cached;
            }
            try {
                Image image = VectorImage.rasterize(ARTWORK_SVG, width, height);
                RASTER_CACHE.put(size, image);
                return image;
            } catch (RuntimeException exception) {
                LOGGER.log(System.Logger.Level.WARNING, "Button artwork resource failed to rasterize at "
                        + width + "x" + height + ": " + BUTTON_SHAPE_RESOURCE, exception);
                return null;
            }
        }
    }

    private static double positiveSizeOrUnset(double size) {
        return Double.isFinite(size) && size > 0 ? size : Region.USE_COMPUTED_SIZE;
    }

    private record RasterSize(int width, int height) {
    }

    private static final class RasterizedArtwork extends StackPane {
        private final ImageView artwork = new ImageView();
        private final Text label = new Text();
        private final double fixedWidth;
        private final double fixedHeight;
        private int rasterWidth;
        private int rasterHeight;

        private RasterizedArtwork(String text, double width, double height) {
            this.fixedWidth = positiveSizeOrUnset(width);
            this.fixedHeight = positiveSizeOrUnset(height);

            artwork.setPreserveRatio(false);
            artwork.setSmooth(true);
            artwork.setMouseTransparent(true);

            label.setText(text == null ? "" : text);
            label.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, BUTTON_ARTWORK_FONT_SIZE));
            label.getStyleClass().add(BUTTON_ARTWORK_TEXT_STYLE_CLASS);

            getChildren().addAll(artwork, label);
            getStyleClass().add(BUTTON_ARTWORK_STYLE_CLASS);
            setAlignment(Pos.CENTER);
            refreshPreferredSize();
        }

        @Override
        protected double computePrefWidth(double height) {
            if (fixedWidth > 0) {
                return fixedWidth;
            }
            return computeTextWidth();
        }

        @Override
        protected double computePrefHeight(double width) {
            if (fixedHeight > 0) {
                return fixedHeight;
            }
            return computeTextHeight();
        }

        @Override
        protected void layoutChildren() {
            double width = getWidth() > 0 ? getWidth() : prefWidth(-1);
            double height = getHeight() > 0 ? getHeight() : prefHeight(width);
            label.setWrappingWidth(Math.max(0, width - BUTTON_ARTWORK_HORIZONTAL_PADDING));
            updateArtwork(width, height);
            super.layoutChildren();
        }

        @Override
        public void applyCss() {
            super.applyCss();
            refreshPreferredSize();
        }

        private void refreshPreferredSize() {
            if (fixedWidth > 0) {
                label.setWrappingWidth(Math.max(0, fixedWidth - BUTTON_ARTWORK_HORIZONTAL_PADDING));
            } else {
                label.setWrappingWidth(0);
            }
            double width = fixedWidth > 0 ? fixedWidth : computeTextWidth();
            double height = fixedHeight > 0 ? fixedHeight : computeTextHeight();
            setPrefSize(width, height);
            setMinSize(width, height);
            setMaxSize(width, height);
        }

        private double computeTextWidth() {
            return Math.max(BUTTON_ARTWORK_MIN_WIDTH,
                    Math.ceil(label.getLayoutBounds().getWidth() + BUTTON_ARTWORK_HORIZONTAL_PADDING));
        }

        private double computeTextHeight() {
            return Math.max(BUTTON_ARTWORK_HEIGHT,
                    Math.ceil(label.getLayoutBounds().getHeight() + BUTTON_ARTWORK_VERTICAL_PADDING));
        }

        private void updateArtwork(double width, double height) {
            int targetWidth = Math.max(1, (int) Math.ceil(width));
            int targetHeight = Math.max(1, (int) Math.ceil(height));
            if (targetWidth == rasterWidth && targetHeight == rasterHeight) {
                return;
            }
            Image image = rasterizeArtwork(targetWidth, targetHeight);
            if (image != null) {
                artwork.setImage(image);
                artwork.setFitWidth(targetWidth);
                artwork.setFitHeight(targetHeight);
                rasterWidth = targetWidth;
                rasterHeight = targetHeight;
            }
        }
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
