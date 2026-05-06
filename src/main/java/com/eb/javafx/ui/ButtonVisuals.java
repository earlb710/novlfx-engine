package com.eb.javafx.ui;

import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared button visual defaults for routed and test screens.
 */
final class ButtonVisuals {
    static final String BUTTON_SHAPE_RESOURCE = "/com/eb/javafx/images/svg/button-pill.svg";
    private static final Pattern PATH_DATA_PATTERN = Pattern.compile("<path\\b[^>]*\\bd\\s*=\\s*(['\"])(.*?)\\1", Pattern.DOTALL);
    private static final String SHAPE_PATH = loadShapePath();

    private ButtonVisuals() {
    }

    static Button apply(Button button) {
        if (!SHAPE_PATH.isBlank()) {
            SVGPath shape = new SVGPath();
            shape.setContent(SHAPE_PATH);
            button.setShape(shape);
            button.setScaleShape(true);
            button.setCenterShape(false);
        }
        button.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        button.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        return button;
    }

    static String buttonShapePath() {
        return SHAPE_PATH;
    }

    private static String loadShapePath() {
        try (InputStream stream = ButtonVisuals.class.getResourceAsStream(BUTTON_SHAPE_RESOURCE)) {
            if (stream == null) {
                return "";
            }
            String svg = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = PATH_DATA_PATTERN.matcher(svg);
            return matcher.find() ? matcher.group(2) : "";
        } catch (IOException exception) {
            return "";
        }
    }
}
