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
        if (tokens.size() < 4 || tokens.size() > 7) {
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
                case "interpolation" -> interpolation = parseInterpolation(value, sourceName, lineNumber);
                default -> throw error(sourceName, lineNumber, "unsupported step property: " + property);
            }
        }
        builder.addStep(lineNumber, durationMillis, pauseBeforeMillis, interpolation);
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
                    interpolation)));
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
