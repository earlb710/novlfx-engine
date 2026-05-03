package com.eb.javafx.util;

import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Helper methods for engine fonts packaged under {@code /com/eb/javafx/fonts}.
 */
public final class FontResources {
    public static final String RESOURCE_ROOT = "/com/eb/javafx/fonts";

    private static final List<String> FONT_FILE_NAMES = List.of(
            "AAntiCorona-L3Ax3.ttf",
            "AlfaqixAlgorithm-SemiBold.otf",
            "AlfaqixDiode-SemiBold.otf",
            "AlfaqixEllipsoid-SemiBold.otf",
            "Alien.ttf",
            "Autobusbold-1ynL.ttf",
            "Avara.ttf",
            "Boron.otf",
            "Crimson-Bold.ttf",
            "Crimson-Roman.ttf",
            "Dommage.ttf",
            "DymaxionScript.ttf",
            "FantasqueSansMono-Bold.ttf",
            "FantasqueSansMono-BoldItalic.ttf",
            "FantasqueSansMono-Italic.ttf",
            "FantasqueSansMono-Regular.ttf",
            "GlacialIndifference-Regular.otf",
            "HKVenetian-Italic.otf",
            "HKVenetian-Regular.otf",
            "Hydrogen Whiskey.otf",
            "Library 3 am soft.otf",
            "Library 3 am.otf",
            "Nasalization Rg.otf",
            "OpenDyslexic3-Regular.ttf",
            "SpeedPXWatch-Regular.ttf",
            "TruenoBd.otf",
            "TruenoRg.otf",
            "boon-500.otf",
            "ethnocentric rg.ttf",
            "nerdropol lattice.otf",
            "plasdrip.ttf",
            "plasdrpe.ttf",
            "supragc.ttf",
            "supragen.fon",
            "supragl.ttf",
            "trebuc.ttf");

    private static final Set<String> FONT_FILE_NAME_SET = Set.copyOf(FONT_FILE_NAMES);

    private FontResources() {
    }

    /**
     * Returns the packaged font file names.
     */
    public static List<String> fontFileNames() {
        return FONT_FILE_NAMES;
    }

    /**
     * Returns whether the file name is one of the packaged engine fonts.
     *
     * @param fileName font file name, without a directory
     */
    public static boolean isPackagedFont(String fileName) {
        return fileName != null && FONT_FILE_NAME_SET.contains(fileName);
    }

    /**
     * Builds the classpath resource path for a packaged engine font.
     *
     * @param fileName font file name, without a directory
     */
    public static String resourcePath(String fileName) {
        return RESOURCE_ROOT + "/" + requirePackagedFontFileName(fileName);
    }

    /**
     * Finds the packaged font resource URL when present.
     *
     * @param fileName font file name, without a directory
     */
    public static Optional<URL> resourceUrl(String fileName) {
        return Optional.ofNullable(FontResources.class.getResource(resourcePath(fileName)));
    }

    /**
     * Returns the packaged font resource URL or throws when the resource is missing.
     *
     * @param fileName font file name, without a directory
     */
    public static URL requireResourceUrl(String fileName) {
        return resourceUrl(fileName)
                .orElseThrow(() -> new IllegalStateException("Packaged font resource is missing: " + resourcePath(fileName)));
    }

    /**
     * Opens a packaged font resource stream. Callers must close the returned stream.
     *
     * @param fileName font file name, without a directory
     */
    public static InputStream open(String fileName) {
        InputStream inputStream = FontResources.class.getResourceAsStream(resourcePath(fileName));
        if (inputStream == null) {
            throw new IllegalStateException("Packaged font resource is missing: " + resourcePath(fileName));
        }
        return inputStream;
    }

    /**
     * Loads a packaged JavaFX font at the requested size.
     *
     * @param fileName font file name, without a directory
     * @param size font size in points
     */
    public static Font load(String fileName, double size) {
        Validation.requirePositive(size, "Font size must be positive.");
        try (InputStream inputStream = open(fileName)) {
            Font font = Font.loadFont(inputStream, size);
            if (font == null) {
                throw new IllegalStateException("Unable to load packaged font: " + resourcePath(fileName));
            }
            return font;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to close packaged font resource: " + resourcePath(fileName), exception);
        }
    }

    private static String requirePackagedFontFileName(String fileName) {
        String validFileName = Validation.requireNonBlank(fileName, "Font file name is required.");
        if (validFileName.contains("/") || validFileName.contains("\\")) {
            throw new IllegalArgumentException("Font file name must not include a directory.");
        }
        if (!FONT_FILE_NAME_SET.contains(validFileName)) {
            throw new IllegalArgumentException("Unknown packaged font: " + validFileName);
        }
        return validFileName;
    }
}
