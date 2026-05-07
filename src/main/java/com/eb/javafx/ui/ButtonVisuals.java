package com.eb.javafx.ui;

import com.eb.javafx.util.VectorImage;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared button visual defaults for routed and test screens.
 */
public final class ButtonVisuals {
    public static final String BUTTON_SHORT_ARTWORK_RESOURCE = "/com/eb/javafx/images/svg/button-pill-short.svg";
    public static final String BUTTON_NORMAL_ARTWORK_RESOURCE = "/com/eb/javafx/images/svg/button-pill-normal.svg";
    public static final String BUTTON_LONG_ARTWORK_RESOURCE = "/com/eb/javafx/images/svg/button-pill-long.svg";
    public static final String BUTTON_SHAPE_RESOURCE = BUTTON_NORMAL_ARTWORK_RESOURCE;
    public static final String BUTTON_STYLE_CLASS = "svg-button";
    public static final String BUTTON_ARTWORK_STYLE_CLASS = "svg-button-artwork";
    public static final String BUTTON_ARTWORK_TEXT_STYLE_CLASS = "svg-button-artwork-text";
    public static final double BUTTON_SHORT_ARTWORK_WIDTH = 100;
    public static final double BUTTON_ARTWORK_WIDTH = 200;
    public static final double BUTTON_ARTWORK_HEIGHT = 48;
    private static final double BUTTON_ARTWORK_MIN_WIDTH = 48;
    private static final double BUTTON_ARTWORK_HORIZONTAL_PADDING = 36;
    private static final double BUTTON_ARTWORK_VERTICAL_PADDING = 16;
    private static final double BUTTON_ARTWORK_FONT_SIZE = 20;
    private static final double BUTTON_ARTWORK_SOURCE_WIDTH = 400;
    private static final double BUTTON_ARTWORK_SOURCE_HEIGHT = 150;
    private static final double BUTTON_ARTWORK_SOURCE_CAP_WIDTH = 75;
    private static final double BUTTON_ARTWORK_SOURCE_MIDDLE_WIDTH = 250;
    private static final int MAX_RASTER_CACHE_SIZE = 128;
    private static final String ARTWORK_FALLBACK_GRADIENT = """
            <linearGradient id="%s" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="0" y2="48">
              <stop offset="0" style="stop-color:#9dccff;stop-opacity:1"/>
              <stop offset="0.5" style="stop-color:#0f4f9f;stop-opacity:1"/>
              <stop offset="1" style="stop-color:#052f6f;stop-opacity:1"/>
            </linearGradient>
            """;
    private static final Pattern PATH_DATA_PATTERN = Pattern.compile("<path\\b[^>]*\\bd\\s*=\\s*(['\"])(.*?)\\1", Pattern.DOTALL);
    private static final Pattern SAFE_PATH_DATA_PATTERN = Pattern.compile("[MmZzLlHhVvCcSsQqTtAaEe0-9+\\-.,\\s]+");
    private static final Pattern MESH_GRADIENT_PATTERN = Pattern.compile(
            "<meshgradient\\b[^>]*\\bid\\s*=\\s*(['\"])(.*?)\\1[^>]*>.*?</meshgradient>",
            Pattern.DOTALL);
    private static final Pattern STOP_STYLE_PATTERN = Pattern.compile("<stop\\b[^>]*\\bstyle\\s*=\\s*(['\"])(.*?)\\1", Pattern.DOTALL);
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script\\b[^>]*>.*?</script>", Pattern.DOTALL);
    private static final Pattern SVG_TAG_PATTERN = Pattern.compile("<svg\\b(?![^>]*\\bpreserveAspectRatio\\s*=)", Pattern.CASE_INSENSITIVE);
    private static final System.Logger LOGGER = System.getLogger(ButtonVisuals.class.getName());
    private static final String SHAPE_PATH = loadShapePath();
    private static final ArtworkResource SHORT_ARTWORK = loadArtworkResource(BUTTON_SHORT_ARTWORK_RESOURCE);
    private static final ArtworkResource NORMAL_ARTWORK = loadArtworkResource(BUTTON_NORMAL_ARTWORK_RESOURCE);
    private static final ArtworkResource LONG_ARTWORK = loadArtworkResource(BUTTON_LONG_ARTWORK_RESOURCE);
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
            if (artwork instanceof RasterizedArtwork rasterizedArtwork) {
                button.pressedProperty().addListener((observable, wasPressed, isPressed) ->
                        rasterizedArtwork.setArtworkPressed(Boolean.TRUE.equals(isPressed)));
            }
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
        return LONG_ARTWORK.url();
    }

    public static String buttonArtworkResourceUrl(String text, double width, double height) {
        return LONG_ARTWORK.url();
    }

    public static boolean usesLongArtwork(String text, double width, double height) {
        return true;
    }

    public static boolean usesShortArtwork(String text, double width, double height) {
        return false;
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
        return createArtworkGraphic(text, width, height, false);
    }

    public static Node createArtworkGraphic(String text, double width, double height, boolean pressed) {
        if (LONG_ARTWORK.svg().isBlank()) {
            return null;
        }
        return new RasterizedArtwork(text, width, height, pressed);
    }

    private static String loadShapePath() {
        String svg = loadSvgResource(BUTTON_SHAPE_RESOURCE);
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

    private static ArtworkResource loadArtworkResource(String resourcePath) {
        URL resource = ButtonVisuals.class.getResource(resourcePath);
        if (resource == null) {
            LOGGER.log(System.Logger.Level.WARNING, "Button artwork resource is missing: {0}", resourcePath);
            return new ArtworkResource(resourcePath, "", "", "");
        }
        String svg = loadSvgResource(resourcePath);
        if (svg.isEmpty()) {
            return new ArtworkResource(resourcePath, resource.toExternalForm(), "", "");
        }
        String preparedSvg = prepareArtworkSvgForBatik(svg);
        return new ArtworkResource(resourcePath, resource.toExternalForm(), preparedSvg, reverseGradientStopStyles(preparedSvg));
    }

    private static String prepareArtworkSvgForBatik(String svg) {
        Matcher matcher = MESH_GRADIENT_PATTERN.matcher(svg);
        String prepared = svg;
        if (matcher.find()) {
            prepared = matcher.replaceFirst(Matcher.quoteReplacement(ARTWORK_FALLBACK_GRADIENT.formatted(matcher.group(2))));
        }
        prepared = SCRIPT_PATTERN.matcher(prepared).replaceAll("");
        return SVG_TAG_PATTERN.matcher(prepared).replaceFirst("<svg preserveAspectRatio=\"none\"");
    }

    private static String reverseGradientStopStyles(String svg) {
        Matcher matcher = STOP_STYLE_PATTERN.matcher(svg);
        List<StyleSpan> spans = new ArrayList<>();
        while (matcher.find()) {
            spans.add(new StyleSpan(matcher.start(2), matcher.end(2), matcher.group(2)));
        }
        if (spans.size() < 2) {
            return svg;
        }
        StringBuilder reversed = new StringBuilder(svg);
        for (int index = spans.size() - 1; index >= 0; index--) {
            StyleSpan span = spans.get(index);
            reversed.replace(span.start(), span.end(), spans.get(spans.size() - 1 - index).style());
        }
        return reversed.toString();
    }

    private static Image rasterizeArtwork(ArtworkResource artworkResource, int width, int height, boolean pressed) {
        String svg = pressed ? artworkResource.pressedSvg() : artworkResource.svg();
        if (svg.isBlank() || width <= 0 || height <= 0) {
            return null;
        }
        RasterSize size = new RasterSize(artworkResource.resourcePath(), pressed, width, height);
        synchronized (RASTER_CACHE) {
            Image cached = RASTER_CACHE.get(size);
            if (cached != null) {
                return cached;
            }
            try {
                Image image = VectorImage.rasterize(svg, width, height);
                RASTER_CACHE.put(size, image);
                return image;
            } catch (RuntimeException exception) {
                LOGGER.log(System.Logger.Level.WARNING, "Button artwork resource failed to rasterize at "
                        + width + "x" + height + ": " + artworkResource.resourcePath(), exception);
                return null;
            }
        }
    }

    private static double positiveSizeOrUnset(double size) {
        return Double.isFinite(size) && size > 0 ? size : Region.USE_COMPUTED_SIZE;
    }

    private record StyleSpan(int start, int end, String style) {
    }

    private record RasterSize(String resourcePath, boolean pressed, int width, int height) {
    }

    private record ArtworkResource(String resourcePath, String url, String svg, String pressedSvg) {
    }

    private static final class RasterizedArtwork extends StackPane {
        private final Pane artwork = new Pane();
        private final ImageView leftCap = new ImageView();
        private final ImageView middle = new ImageView();
        private final ImageView rightCap = new ImageView();
        private final Text label = new Text();
        private final double fixedWidth;
        private final double fixedHeight;
        private boolean pressed;
        private int rasterWidth;
        private int rasterHeight;
        private boolean rasterPressed;

        private RasterizedArtwork(String text, double width, double height, boolean pressed) {
            this.fixedWidth = positiveSizeOrUnset(width);
            this.fixedHeight = positiveSizeOrUnset(height);
            this.pressed = pressed;

            artwork.setMouseTransparent(true);
            initializeSlice(leftCap);
            initializeSlice(middle);
            initializeSlice(rightCap);
            artwork.getChildren().addAll(leftCap, middle, rightCap);

            label.setText(text == null ? "" : text);
            label.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, BUTTON_ARTWORK_FONT_SIZE));
            label.getStyleClass().add(BUTTON_ARTWORK_TEXT_STYLE_CLASS);

            getChildren().addAll(artwork, label);
            getStyleClass().add(BUTTON_ARTWORK_STYLE_CLASS);
            setAlignment(Pos.CENTER);
            refreshPreferredSize();
        }

        private static void initializeSlice(ImageView slice) {
            slice.setPreserveRatio(false);
            slice.setSmooth(true);
            slice.setMouseTransparent(true);
        }

        private void setArtworkPressed(boolean pressed) {
            if (this.pressed == pressed) {
                return;
            }
            this.pressed = pressed;
            requestLayout();
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
            artwork.resizeRelocate(0, 0, width, height);
            updateArtwork(width, height);
            layoutLabel(width, height);
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
            if (targetWidth == rasterWidth && targetHeight == rasterHeight && pressed == rasterPressed) {
                return;
            }
            double sourceScale = targetHeight / BUTTON_ARTWORK_SOURCE_HEIGHT;
            int sourceWidth = Math.max(1, (int) Math.ceil(BUTTON_ARTWORK_SOURCE_WIDTH * sourceScale));
            int sourceCapWidth = Math.max(1, (int) Math.round(BUTTON_ARTWORK_SOURCE_CAP_WIDTH * sourceScale));
            int sourceMiddleWidth = Math.max(1, sourceWidth - (sourceCapWidth * 2));
            Image image = rasterizeArtwork(LONG_ARTWORK, sourceWidth, targetHeight, pressed);
            if (image != null) {
                double capWidth = Math.min(targetWidth / 2.0,
                        targetHeight * BUTTON_ARTWORK_SOURCE_CAP_WIDTH / BUTTON_ARTWORK_SOURCE_HEIGHT);
                double middleWidth = Math.max(0, targetWidth - (capWidth * 2));
                updateSlice(leftCap, image, 0, sourceCapWidth, 0, capWidth, targetHeight);
                updateSlice(middle, image, sourceCapWidth, sourceMiddleWidth, capWidth, middleWidth, targetHeight);
                updateSlice(rightCap, image, sourceWidth - sourceCapWidth, sourceCapWidth,
                        capWidth + middleWidth, capWidth, targetHeight);
                rasterWidth = targetWidth;
                rasterHeight = targetHeight;
                rasterPressed = pressed;
            }
        }

        private static void updateSlice(ImageView slice, Image image, double sourceX, double sourceWidth,
                                        double targetX, double targetWidth, double targetHeight) {
            slice.setImage(image);
            slice.setViewport(new Rectangle2D(sourceX, 0, sourceWidth, image.getHeight()));
            slice.setFitWidth(targetWidth);
            slice.setFitHeight(targetHeight);
            slice.relocate(targetX, 0);
        }

        private void layoutLabel(double width, double height) {
            label.autosize();
            var bounds = label.getLayoutBounds();
            label.setLayoutX((width - bounds.getWidth()) / 2 - bounds.getMinX());
            label.setLayoutY((height - bounds.getHeight()) / 2 - bounds.getMinY());
        }
    }

    private static String loadSvgResource(String resourcePath) {
        try (InputStream stream = ButtonVisuals.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                LOGGER.log(System.Logger.Level.WARNING, "Button shape resource is missing: {0}", resourcePath);
                return "";
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Button shape resource failed to load: " + resourcePath, exception);
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
