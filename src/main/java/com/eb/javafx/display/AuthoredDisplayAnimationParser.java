package com.eb.javafx.display;

import com.eb.javafx.util.Validation;
import javafx.animation.Animation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Deterministic parser for the engine's small ATL-style authored animation DSL.
 *
 * <p>The parser accepts only data commands and never evaluates expressions,
 * callbacks, filesystem paths, or application code.</p>
 */
public final class AuthoredDisplayAnimationParser {
    private AuthoredDisplayAnimationParser() {
    }

    /** Parses one or more {@code animation <id>} blocks from a text resource. */
    public static List<AuthoredDisplayAnimation> parseDocument(String script, String sourceName) {
        Validation.requireNonBlank(script, "Authored animation script is required.");
        Validation.requireNonBlank(sourceName, "Authored animation source name is required.");

        List<AuthoredDisplayAnimation> animations = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        Builder builder = null;
        String[] lines = script.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            int lineNumber = index + 1;
            String line = stripComment(lines[index]).trim();
            if (line.isEmpty()) {
                continue;
            }
            List<String> tokens = tokens(line);
            String command = tokens.get(0).toLowerCase(Locale.ROOT);
            if ("animation".equals(command)) {
                if (builder != null) {
                    animations.add(finish(builder, ids));
                }
                if (tokens.size() != 2) {
                    throw error(sourceName, lineNumber, "animation command requires exactly one id.");
                }
                builder = new Builder(tokens.get(1), sourceName, lineNumber, AnimationSpec.DEFAULT_REPEAT_COUNT, false);
                continue;
            }
            if (builder == null) {
                throw error(sourceName, lineNumber, "command must appear inside an animation block.");
            }
            if ("end".equals(command)) {
                if (tokens.size() != 1) {
                    throw error(sourceName, lineNumber, "end command does not accept values.");
                }
                animations.add(finish(builder, ids));
                builder = null;
                continue;
            }
            applyCommand(builder, tokens, sourceName, lineNumber);
        }
        if (builder != null) {
            animations.add(finish(builder, ids));
        }
        if (animations.isEmpty()) {
            throw new IllegalArgumentException(sourceName + ": no authored animations were found.");
        }
        return List.copyOf(animations);
    }

    /** Parses the body of a single JSON-backed animation definition. */
    public static AuthoredDisplayAnimation parseAnimation(
            String id,
            List<String> scriptLines,
            String sourceName,
            int lineNumber,
            int repeatCount,
            boolean autoReverse) {
        Validation.requireNonBlank(id, "Authored animation id is required.");
        Validation.requireNonEmpty(scriptLines, "Authored animation script lines are required.");
        Builder builder = new Builder(id, sourceName, lineNumber, repeatCount, autoReverse);
        for (int index = 0; index < scriptLines.size(); index++) {
            int commandLine = lineNumber + index;
            String line = stripComment(scriptLines.get(index)).trim();
            if (line.isEmpty()) {
                continue;
            }
            List<String> tokens = tokens(line);
            String command = tokens.get(0).toLowerCase(Locale.ROOT);
            if ("animation".equals(command) || "end".equals(command)) {
                throw error(sourceName, commandLine, "JSON animation scripts contain commands only, not animation/end blocks.");
            }
            applyCommand(builder, tokens, sourceName, commandLine);
        }
        return builder.build();
    }

    public static AuthoredDisplayAnimation parseAnimation(
            String id,
            String script,
            String sourceName,
            int lineNumber,
            int repeatCount,
            boolean autoReverse) {
        Validation.requireNonBlank(script, "Authored animation script is required.");
        return parseAnimation(id, List.of(script.split("\\R", -1)), sourceName, lineNumber, repeatCount, autoReverse);
    }

    private static AuthoredDisplayAnimation finish(Builder builder, Set<String> ids) {
        if (!ids.add(builder.id)) {
            throw error(builder.sourceName, builder.lineNumber, "duplicate authored animation id: " + builder.id);
        }
        return builder.build();
    }

    private static void applyCommand(Builder builder, List<String> tokens, String sourceName, int lineNumber) {
        String command = tokens.get(0).toLowerCase(Locale.ROOT);
        switch (command) {
            case "repeat" -> parseRepeat(builder, tokens, sourceName, lineNumber);
            case "autoreverse" -> parseAutoReverse(builder, tokens, sourceName, lineNumber);
            case "pause" -> parsePause(builder, tokens, sourceName, lineNumber);
            case "fade" -> parseFade(builder, tokens, sourceName, lineNumber);
            case "move" -> parseMove(builder, tokens, sourceName, lineNumber);
            case "scale" -> parseScale(builder, tokens, sourceName, lineNumber);
            case "rotate" -> parseRotate(builder, tokens, sourceName, lineNumber);
            case "clip" -> parseRectangleBounds(builder, tokens, sourceName, lineNumber, "clip");
            case "viewport" -> parseRectangleBounds(builder, tokens, sourceName, lineNumber, "viewport");
            case "blur" -> parseBlur(builder, tokens, sourceName, lineNumber);
            case "dropshadow" -> parseDropShadow(builder, tokens, sourceName, lineNumber);
            case "coloradjust" -> parseColorAdjust(builder, tokens, sourceName, lineNumber);
            case "step" -> parseStep(builder, tokens, sourceName, lineNumber);
            default -> throw error(sourceName, lineNumber, "unknown authored animation command: " + tokens.get(0));
        }
    }

    private static void parseRepeat(Builder builder, List<String> tokens, String sourceName, int lineNumber) {
        requireTokenCount(tokens, 2, sourceName, lineNumber, "repeat command requires one value.");
        String value = tokens.get(1);
        if ("indefinite".equalsIgnoreCase(value)) {
            builder.repeatCount = Animation.INDEFINITE;
            return;
        }
        builder.repeatCount = parsePositiveInt(value, sourceName, lineNumber, "repeat count");
    }

    private static void parseAutoReverse(Builder builder, List<String> tokens, String sourceName, int lineNumber) {
        requireTokenCount(tokens, 2, sourceName, lineNumber, "autoreverse command requires true or false.");
        builder.autoReverse = parseBoolean(tokens.get(1), sourceName, lineNumber, "autoreverse");
    }

    private static void parsePause(Builder builder, List<String> tokens, String sourceName, int lineNumber) {
        requireTokenCount(tokens, 2, sourceName, lineNumber, "pause command requires a duration in milliseconds.");
        long pauseMillis = parseZeroOrPositiveLong(tokens.get(1), sourceName, lineNumber, "pause duration");
        builder.addStep(lineNumber, 0L, pauseMillis, DisplayInterpolation.LINEAR);
    }

    private static void parseFade(Builder builder, List<String> tokens, String sourceName, int lineNumber) {
        if (tokens.size() < 4 || tokens.size() > 5 || !"opacity".equalsIgnoreCase(tokens.get(2))) {
            throw error(sourceName, lineNumber, "fade syntax is: fade <durationMillis> opacity <0..1> [interpolation].");
        }
        long durationMillis = parseZeroOrPositiveLong(tokens.get(1), sourceName, lineNumber, "fade duration");
        builder.opacity = parseDouble(tokens.get(3), sourceName, lineNumber, "opacity");
        DisplayInterpolation interpolation = parseInterpolation(tokens, 4, sourceName, lineNumber);
        builder.addStep(lineNumber, durationMillis, 0L, interpolation);
    }

    private static void parseMove(Builder builder, List<String> tokens, String sourceName, int lineNumber) {
        if (tokens.size() < 6 || tokens.size() > 7) {
            throw error(sourceName, lineNumber, "move syntax is: move <durationMillis> translateX <x> translateY <y> [interpolation].");
        }
        long durationMillis = parseZeroOrPositiveLong(tokens.get(1), sourceName, lineNumber, "move duration");
        int endExclusive = isInterpolation(tokens.get(tokens.size() - 1)) ? tokens.size() - 1 : tokens.size();
        PropertyCursor cursor = new PropertyCursor(tokens, 2, endExclusive);
        boolean sawX = false;
        boolean sawY = false;
        while (cursor.hasNext()) {
            String property = cursor.next();
            String value = cursor.nextValue(sourceName, lineNumber, property);
            if ("translatex".equalsIgnoreCase(property)) {
                builder.translateX = parseDouble(value, sourceName, lineNumber, "translateX");
                sawX = true;
            } else if ("translatey".equalsIgnoreCase(property)) {
                builder.translateY = parseDouble(value, sourceName, lineNumber, "translateY");
                sawY = true;
            } else {
                throw error(sourceName, lineNumber, "unsupported move property: " + property);
            }
        }
        if (!sawX || !sawY) {
            throw error(sourceName, lineNumber, "move command requires translateX and translateY.");
        }
        builder.addStep(lineNumber, durationMillis, 0L, parseInterpolation(tokens, endExclusive, sourceName, lineNumber));
    }

    private static void parseScale(Builder builder, List<String> tokens, String sourceName, int lineNumber) {
        if (tokens.size() < 3 || tokens.size() > 7) {
            throw error(sourceName, lineNumber, "scale syntax is: scale <durationMillis> <value> [interpolation] or scale <durationMillis> scaleX <x> scaleY <y> [interpolation].");
        }
        long durationMillis = parseZeroOrPositiveLong(tokens.get(1), sourceName, lineNumber, "scale duration");
        if (isNumber(tokens.get(2))) {
            if (tokens.size() > 4) {
                throw error(sourceName, lineNumber, "uniform scale accepts only one value and optional interpolation.");
            }
            double scale = parseDouble(tokens.get(2), sourceName, lineNumber, "scale");
            builder.scaleX = scale;
            builder.scaleY = scale;
            builder.addStep(lineNumber, durationMillis, 0L, parseInterpolation(tokens, 3, sourceName, lineNumber));
            return;
        }

        int endExclusive = isInterpolation(tokens.get(tokens.size() - 1)) ? tokens.size() - 1 : tokens.size();
        PropertyCursor cursor = new PropertyCursor(tokens, 2, endExclusive);
        boolean sawX = false;
        boolean sawY = false;
        while (cursor.hasNext()) {
            String property = cursor.next();
            String value = cursor.nextValue(sourceName, lineNumber, property);
            if ("scalex".equalsIgnoreCase(property)) {
                builder.scaleX = parseDouble(value, sourceName, lineNumber, "scaleX");
                sawX = true;
            } else if ("scaley".equalsIgnoreCase(property)) {
                builder.scaleY = parseDouble(value, sourceName, lineNumber, "scaleY");
                sawY = true;
            } else {
                throw error(sourceName, lineNumber, "unsupported scale property: " + property);
            }
        }
        if (!sawX || !sawY) {
            throw error(sourceName, lineNumber, "scale command requires scaleX and scaleY.");
        }
        builder.addStep(lineNumber, durationMillis, 0L, parseInterpolation(tokens, endExclusive, sourceName, lineNumber));
    }

    private static void parseRotate(Builder builder, List<String> tokens, String sourceName, int lineNumber) {
        if (tokens.size() < 3 || tokens.size() > 5) {
            throw error(sourceName, lineNumber, "rotate syntax is: rotate <durationMillis> <degrees> [interpolation] or rotate <durationMillis> rotate <degrees> [interpolation].");
        }
        long durationMillis = parseZeroOrPositiveLong(tokens.get(1), sourceName, lineNumber, "rotate duration");
        int valueIndex = 2;
        if ("rotate".equalsIgnoreCase(tokens.get(2)) || "rotation".equalsIgnoreCase(tokens.get(2))) {
            if (tokens.size() < 4) {
                throw error(sourceName, lineNumber, "rotate command requires a degree value.");
            }
            valueIndex = 3;
        } else if (tokens.size() > 4) {
            throw error(sourceName, lineNumber, "rotate syntax is: rotate <durationMillis> <degrees> [interpolation] or rotate <durationMillis> rotate <degrees> [interpolation].");
        }
        builder.rotate = parseDouble(tokens.get(valueIndex), sourceName, lineNumber, "rotate");
        builder.addStep(lineNumber, durationMillis, 0L, parseInterpolation(tokens, valueIndex + 1, sourceName, lineNumber));
    }

    private static void parseRectangleBounds(Builder builder, List<String> tokens, String sourceName, int lineNumber, String command) {
        if (tokens.size() < 10 || tokens.size() > 11) {
            throw error(sourceName, lineNumber, command + " syntax is: " + command + " <durationMillis> x <x> y <y> width <width> height <height> [interpolation].");
        }
        long durationMillis = parseZeroOrPositiveLong(tokens.get(1), sourceName, lineNumber, command + " duration");
        int endExclusive = isInterpolation(tokens.get(tokens.size() - 1)) ? tokens.size() - 1 : tokens.size();
        DisplayRectangleBounds bounds = parseBounds(new PropertyCursor(tokens, 2, endExclusive), sourceName, lineNumber, command);
        if ("clip".equals(command)) {
            builder.clipBounds = bounds;
        } else {
            builder.viewportBounds = bounds;
        }
        builder.addStep(lineNumber, durationMillis, 0L, parseInterpolation(tokens, endExclusive, sourceName, lineNumber));
    }

    private static DisplayRectangleBounds parseBounds(PropertyCursor cursor, String sourceName, int lineNumber, String command) {
        Double x = null;
        Double y = null;
        Double width = null;
        Double height = null;
        while (cursor.hasNext()) {
            String property = cursor.next();
            String value = cursor.nextValue(sourceName, lineNumber, property);
            switch (property.toLowerCase(Locale.ROOT)) {
                case "x" -> x = parseDouble(value, sourceName, lineNumber, command + " x");
                case "y" -> y = parseDouble(value, sourceName, lineNumber, command + " y");
                case "width" -> width = parseDouble(value, sourceName, lineNumber, command + " width");
                case "height" -> height = parseDouble(value, sourceName, lineNumber, command + " height");
                default -> throw error(sourceName, lineNumber, "unsupported " + command + " property: " + property);
            }
        }
        if (x == null || y == null || width == null || height == null) {
            throw error(sourceName, lineNumber, command + " command requires x, y, width, and height.");
        }
        return new DisplayRectangleBounds(x, y, width, height);
    }

    private static void parseBlur(Builder builder, List<String> tokens, String sourceName, int lineNumber) {
        if (tokens.size() < 3 || tokens.size() > 5) {
            throw error(sourceName, lineNumber, "blur syntax is: blur <durationMillis> <radius> [interpolation] or blur <durationMillis> radius <radius> [interpolation].");
        }
        long durationMillis = parseZeroOrPositiveLong(tokens.get(1), sourceName, lineNumber, "blur duration");
        int valueIndex = 2;
        if ("radius".equalsIgnoreCase(tokens.get(2))) {
            if (tokens.size() < 4) {
                throw error(sourceName, lineNumber, "blur command requires a radius value.");
            }
            valueIndex = 3;
        } else if (tokens.size() > 4) {
            throw error(sourceName, lineNumber, "blur syntax is: blur <durationMillis> <radius> [interpolation] or blur <durationMillis> radius <radius> [interpolation].");
        }
        builder.blurEnabled = true;
        builder.blurRadius = parseDouble(tokens.get(valueIndex), sourceName, lineNumber, "blur radius");
        builder.addStep(lineNumber, durationMillis, 0L, parseInterpolation(tokens, valueIndex + 1, sourceName, lineNumber));
    }

    private static void parseDropShadow(Builder builder, List<String> tokens, String sourceName, int lineNumber) {
        if (tokens.size() < 8 || tokens.size() > 9) {
            throw error(sourceName, lineNumber, "dropShadow syntax is: dropShadow <durationMillis> radius <radius> offsetX <x> offsetY <y> [interpolation].");
        }
        long durationMillis = parseZeroOrPositiveLong(tokens.get(1), sourceName, lineNumber, "dropShadow duration");
        int endExclusive = isInterpolation(tokens.get(tokens.size() - 1)) ? tokens.size() - 1 : tokens.size();
        PropertyCursor cursor = new PropertyCursor(tokens, 2, endExclusive);
        boolean sawRadius = false;
        boolean sawOffsetX = false;
        boolean sawOffsetY = false;
        while (cursor.hasNext()) {
            String property = cursor.next();
            String value = cursor.nextValue(sourceName, lineNumber, property);
            switch (property.toLowerCase(Locale.ROOT)) {
                case "radius" -> {
                    builder.dropShadowRadius = parseDouble(value, sourceName, lineNumber, "dropShadow radius");
                    sawRadius = true;
                }
                case "offsetx" -> {
                    builder.dropShadowOffsetX = parseDouble(value, sourceName, lineNumber, "dropShadow offsetX");
                    sawOffsetX = true;
                }
                case "offsety" -> {
                    builder.dropShadowOffsetY = parseDouble(value, sourceName, lineNumber, "dropShadow offsetY");
                    sawOffsetY = true;
                }
                default -> throw error(sourceName, lineNumber, "unsupported dropShadow property: " + property);
            }
        }
        if (!sawRadius || !sawOffsetX || !sawOffsetY) {
            throw error(sourceName, lineNumber, "dropShadow command requires radius, offsetX, and offsetY.");
        }
        builder.dropShadowEnabled = true;
        builder.addStep(lineNumber, durationMillis, 0L, parseInterpolation(tokens, endExclusive, sourceName, lineNumber));
    }

    private static void parseColorAdjust(Builder builder, List<String> tokens, String sourceName, int lineNumber) {
        if (tokens.size() < 4 || tokens.size() > 11) {
            throw error(sourceName, lineNumber, "colorAdjust syntax is: colorAdjust <durationMillis> [hue <h>] [saturation <s>] [brightness <b>] [contrast <c>] [interpolation].");
        }
        long durationMillis = parseZeroOrPositiveLong(tokens.get(1), sourceName, lineNumber, "colorAdjust duration");
        int endExclusive = isInterpolation(tokens.get(tokens.size() - 1)) ? tokens.size() - 1 : tokens.size();
        PropertyCursor cursor = new PropertyCursor(tokens, 2, endExclusive);
        boolean sawProperty = false;
        while (cursor.hasNext()) {
            String property = cursor.next();
            String value = cursor.nextValue(sourceName, lineNumber, property);
            switch (property.toLowerCase(Locale.ROOT)) {
                case "hue" -> builder.colorAdjustHue = parseDouble(value, sourceName, lineNumber, "colorAdjust hue");
                case "saturation" -> builder.colorAdjustSaturation = parseDouble(value, sourceName, lineNumber, "colorAdjust saturation");
                case "brightness" -> builder.colorAdjustBrightness = parseDouble(value, sourceName, lineNumber, "colorAdjust brightness");
                case "contrast" -> builder.colorAdjustContrast = parseDouble(value, sourceName, lineNumber, "colorAdjust contrast");
                default -> throw error(sourceName, lineNumber, "unsupported colorAdjust property: " + property);
            }
            sawProperty = true;
        }
        if (!sawProperty) {
            throw error(sourceName, lineNumber, "colorAdjust command requires at least one property.");
        }
        builder.colorAdjustEnabled = true;
        builder.addStep(lineNumber, durationMillis, 0L, parseInterpolation(tokens, endExclusive, sourceName, lineNumber));
    }

    private static void parseStep(Builder builder, List<String> tokens, String sourceName, int lineNumber) {
        if (tokens.size() < 2) {
            throw error(sourceName, lineNumber, "step syntax is: step <durationMillis> [property value ...] [interpolation].");
        }
        long durationMillis = parseZeroOrPositiveLong(tokens.get(1), sourceName, lineNumber, "step duration");
        int endExclusive = tokens.size();
        DisplayInterpolation interpolation = DisplayInterpolation.LINEAR;
        if (tokens.size() > 2 && isInterpolation(tokens.get(tokens.size() - 1))) {
            interpolation = parseInterpolation(tokens.get(tokens.size() - 1), sourceName, lineNumber);
            endExclusive--;
        }
        long pauseBeforeMillis = 0L;
        PropertyCursor cursor = new PropertyCursor(tokens, 2, endExclusive);
        while (cursor.hasNext()) {
            String property = cursor.next();
            String value = cursor.nextValue(sourceName, lineNumber, property);
            switch (property.toLowerCase(Locale.ROOT)) {
                case "pausebefore", "pausebeforemillis" ->
                        pauseBeforeMillis = parseZeroOrPositiveLong(value, sourceName, lineNumber, "pauseBeforeMillis");
                case "opacity" -> builder.opacity = parseDouble(value, sourceName, lineNumber, "opacity");
                case "scalex" -> builder.scaleX = parseDouble(value, sourceName, lineNumber, "scaleX");
                case "scaley" -> builder.scaleY = parseDouble(value, sourceName, lineNumber, "scaleY");
                case "translatex" -> builder.translateX = parseDouble(value, sourceName, lineNumber, "translateX");
                case "translatey" -> builder.translateY = parseDouble(value, sourceName, lineNumber, "translateY");
                case "rotate", "rotation" -> builder.rotate = parseDouble(value, sourceName, lineNumber, "rotate");
                case "clipx" -> builder.clipBounds = stepBounds(value, builder.clipBounds, sourceName, lineNumber, "clip", "x");
                case "clipy" -> builder.clipBounds = stepBounds(value, builder.clipBounds, sourceName, lineNumber, "clip", "y");
                case "clipwidth" -> builder.clipBounds = stepBounds(value, builder.clipBounds, sourceName, lineNumber, "clip", "width");
                case "clipheight" -> builder.clipBounds = stepBounds(value, builder.clipBounds, sourceName, lineNumber, "clip", "height");
                case "viewportx" -> builder.viewportBounds = stepBounds(value, builder.viewportBounds, sourceName, lineNumber, "viewport", "x");
                case "viewporty" -> builder.viewportBounds = stepBounds(value, builder.viewportBounds, sourceName, lineNumber, "viewport", "y");
                case "viewportwidth" -> builder.viewportBounds = stepBounds(value, builder.viewportBounds, sourceName, lineNumber, "viewport", "width");
                case "viewportheight" -> builder.viewportBounds = stepBounds(value, builder.viewportBounds, sourceName, lineNumber, "viewport", "height");
                case "blurradius" -> {
                    builder.blurEnabled = true;
                    builder.blurRadius = parseDouble(value, sourceName, lineNumber, "blur radius");
                }
                case "dropshadowradius", "shadowradius" -> {
                    builder.dropShadowEnabled = true;
                    builder.dropShadowRadius = parseDouble(value, sourceName, lineNumber, "dropShadow radius");
                }
                case "dropshadowoffsetx", "shadowoffsetx" -> {
                    builder.dropShadowEnabled = true;
                    builder.dropShadowOffsetX = parseDouble(value, sourceName, lineNumber, "dropShadow offsetX");
                }
                case "dropshadowoffsety", "shadowoffsety" -> {
                    builder.dropShadowEnabled = true;
                    builder.dropShadowOffsetY = parseDouble(value, sourceName, lineNumber, "dropShadow offsetY");
                }
                case "colorhue", "coloradjusthue", "hue" -> {
                    builder.colorAdjustEnabled = true;
                    builder.colorAdjustHue = parseDouble(value, sourceName, lineNumber, "colorAdjust hue");
                }
                case "colorsaturation", "coloradjustsaturation", "saturation" -> {
                    builder.colorAdjustEnabled = true;
                    builder.colorAdjustSaturation = parseDouble(value, sourceName, lineNumber, "colorAdjust saturation");
                }
                case "colorbrightness", "coloradjustbrightness", "brightness" -> {
                    builder.colorAdjustEnabled = true;
                    builder.colorAdjustBrightness = parseDouble(value, sourceName, lineNumber, "colorAdjust brightness");
                }
                case "colorcontrast", "coloradjustcontrast", "contrast" -> {
                    builder.colorAdjustEnabled = true;
                    builder.colorAdjustContrast = parseDouble(value, sourceName, lineNumber, "colorAdjust contrast");
                }
                case "interpolation" -> interpolation = parseInterpolation(value, sourceName, lineNumber);
                default -> throw error(sourceName, lineNumber, "unsupported step property: " + property);
            }
        }
        builder.addStep(lineNumber, durationMillis, pauseBeforeMillis, interpolation);
    }

    private static DisplayRectangleBounds stepBounds(
            String value,
            DisplayRectangleBounds current,
            String sourceName,
            int lineNumber,
            String description,
            String property) {
        double x = current == null ? 0.0 : current.x();
        double y = current == null ? 0.0 : current.y();
        double width = current == null ? 0.0 : current.width();
        double height = current == null ? 0.0 : current.height();
        double parsed = parseDouble(value, sourceName, lineNumber, description + " " + property);
        return switch (property) {
            case "x" -> new DisplayRectangleBounds(parsed, y, width, height);
            case "y" -> new DisplayRectangleBounds(x, parsed, width, height);
            case "width" -> new DisplayRectangleBounds(x, y, parsed, height);
            case "height" -> new DisplayRectangleBounds(x, y, width, parsed);
            default -> throw error(sourceName, lineNumber, "unsupported " + description + " property: " + property);
        };
    }

    private static DisplayInterpolation parseInterpolation(List<String> tokens, int index, String sourceName, int lineNumber) {
        if (index >= tokens.size()) {
            return DisplayInterpolation.LINEAR;
        }
        return parseInterpolation(tokens.get(index), sourceName, lineNumber);
    }

    private static DisplayInterpolation parseInterpolation(String value, String sourceName, int lineNumber) {
        try {
            return DisplayInterpolation.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw error(sourceName, lineNumber, "unsupported interpolation: " + value);
        }
    }

    private static boolean isInterpolation(String value) {
        try {
            DisplayInterpolation.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static void requireTokenCount(List<String> tokens, int expected, String sourceName, int lineNumber, String message) {
        if (tokens.size() != expected) {
            throw error(sourceName, lineNumber, message);
        }
    }

    private static boolean parseBoolean(String value, String sourceName, int lineNumber, String description) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw error(sourceName, lineNumber, description + " must be true or false.");
    }

    private static int parsePositiveInt(String value, String sourceName, int lineNumber, String description) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw error(sourceName, lineNumber, description + " must be positive.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw error(sourceName, lineNumber, description + " must be an integer.");
        }
    }

    private static long parseZeroOrPositiveLong(String value, String sourceName, int lineNumber, String description) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed < 0L) {
                throw error(sourceName, lineNumber, description + " must be zero or positive.");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw error(sourceName, lineNumber, description + " must be an integer.");
        }
    }

    private static double parseDouble(String value, String sourceName, int lineNumber, String description) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw error(sourceName, lineNumber, description + " must be a number.");
        }
    }

    private static boolean isNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static List<String> tokens(String line) {
        return List.of(line.trim().split("\\s+"));
    }

    private static String stripComment(String line) {
        int commentStart = line.indexOf('#');
        if (commentStart < 0) {
            return line;
        }
        return line.substring(0, commentStart);
    }

    private static IllegalArgumentException error(String sourceName, int lineNumber, String message) {
        return new IllegalArgumentException(sourceName + ":" + lineNumber + ": " + message);
    }

    private static final class Builder {
        private final String id;
        private final String sourceName;
        private final int lineNumber;
        private final List<AuthoredDisplayAnimationStep> steps = new ArrayList<>();
        private int repeatCount;
        private boolean autoReverse;
        private double opacity = 1.0;
        private double scaleX = 1.0;
        private double scaleY = 1.0;
        private double translateX = 0.0;
        private double translateY = 0.0;
        private double rotate = 0.0;
        private DisplayRectangleBounds clipBounds;
        private DisplayRectangleBounds viewportBounds;
        private boolean blurEnabled;
        private double blurRadius;
        private boolean dropShadowEnabled;
        private double dropShadowRadius;
        private double dropShadowOffsetX;
        private double dropShadowOffsetY;
        private boolean colorAdjustEnabled;
        private double colorAdjustHue;
        private double colorAdjustSaturation;
        private double colorAdjustBrightness;
        private double colorAdjustContrast;

        private Builder(String id, String sourceName, int lineNumber, int repeatCount, boolean autoReverse) {
            this.id = Validation.requireNonBlank(id, "Authored animation id is required.");
            this.sourceName = Validation.requireNonBlank(sourceName, "Authored animation source name is required.");
            this.lineNumber = Validation.requirePositive(lineNumber, "Authored animation line number must be positive.");
            this.repeatCount = repeatCount;
            this.autoReverse = autoReverse;
        }

        private void addStep(int stepLineNumber, long durationMillis, long pauseBeforeMillis, DisplayInterpolation interpolation) {
            steps.add(new AuthoredDisplayAnimationStep(stepLineNumber, new DisplayAnimationStep(
                    durationMillis,
                    pauseBeforeMillis,
                    opacity,
                    scaleX,
                    scaleY,
                    translateX,
                    translateY,
                    rotate,
                    clipBounds,
                    viewportBounds,
                    effectTargets(),
                    interpolation)));
        }

        private DisplayEffectTargets effectTargets() {
            if (!blurEnabled && !dropShadowEnabled && !colorAdjustEnabled) {
                return DisplayEffectTargets.NONE;
            }
            return new DisplayEffectTargets(
                    blurEnabled,
                    blurRadius,
                    dropShadowEnabled,
                    dropShadowRadius,
                    dropShadowOffsetX,
                    dropShadowOffsetY,
                    colorAdjustEnabled,
                    colorAdjustHue,
                    colorAdjustSaturation,
                    colorAdjustBrightness,
                    colorAdjustContrast);
        }

        private AuthoredDisplayAnimation build() {
            return new AuthoredDisplayAnimation(id, sourceName, lineNumber, steps, repeatCount, autoReverse);
        }
    }

    private static final class PropertyCursor {
        private final List<String> tokens;
        private final int endExclusive;
        private int index;

        private PropertyCursor(List<String> tokens, int index, int endExclusive) {
            this.tokens = tokens;
            this.index = index;
            this.endExclusive = endExclusive;
        }

        private boolean hasNext() {
            return index < endExclusive;
        }

        private String next() {
            return tokens.get(index++);
        }

        private String nextValue(String sourceName, int lineNumber, String property) {
            if (!hasNext()) {
                throw error(sourceName, lineNumber, "missing value for property: " + property);
            }
            return next();
        }
    }

    private static final class AnimationSpec {
        private static final int DEFAULT_REPEAT_COUNT = 1;
    }
}
