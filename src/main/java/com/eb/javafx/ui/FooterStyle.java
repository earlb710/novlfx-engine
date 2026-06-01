package com.eb.javafx.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Config-driven footer styling — font, text colour, active-option "select" colour, background
 * colour, and transparency — applied as a small generated stylesheet layered over the theme.
 *
 * <p>The engine's generated theme styles the footer (`.screen-footer-bar` / `.screen-footer-option`
 * / `.screen-footer-option-active`, the last with a hardcoded select colour). This class lets a
 * game / mod override those purely through {@code config.json} (the top-level {@code footer}
 * object), with no code or theme edit. Set from {@link #configure} at boot; the resulting
 * stylesheet URI is appended to every themed scene after the theme stylesheet so it wins.</p>
 */
public final class FooterStyle {

    private static volatile String stylesheetUri;

    private FooterStyle() {
    }

    /**
     * Builds (or clears) the footer override stylesheet from config values.  Any blank/null value
     * is omitted, so a partial config only overrides the fields it sets.  Must be called before
     * scenes are themed (i.e. during boot).
     *
     * @param font            footer font family (CSS {@code -fx-font-family})
     * @param color           footer text colour
     * @param selectColor     active-option highlight (pill) colour
     * @param backgroundColor footer bar background colour
     * @param transparency    footer bar opacity, {@code 0.0}–{@code 1.0}
     */
    public static void configure(String font, String color, String selectColor,
                                 String backgroundColor, String transparency) {
        String css = buildCss(font, color, selectColor, backgroundColor, transparency);
        if (css.isBlank()) {
            stylesheetUri = null;
            return;
        }
        try {
            Path file = Files.createTempFile("novlfx-footer-", ".css");
            file.toFile().deleteOnExit();
            Files.writeString(file, css, StandardCharsets.UTF_8);
            stylesheetUri = file.toUri().toString();
        } catch (IOException exception) {
            System.err.println("[FooterStyle] Could not write footer stylesheet: " + exception);
            stylesheetUri = null;
        }
    }

    /** The generated footer stylesheet URI, if any footer override is configured. */
    public static Optional<String> stylesheet() {
        return Optional.ofNullable(stylesheetUri);
    }

    /** Builds the CSS body — visible for tests; never null. */
    static String buildCss(String font, String color, String selectColor,
                           String backgroundColor, String transparency) {
        StringBuilder option = new StringBuilder();
        appendDeclaration(option, "-fx-text-fill", color);
        if (isSafe(font)) {
            option.append("    -fx-font-family: \"").append(font.trim()).append("\";\n");
        }
        StringBuilder bar = new StringBuilder();
        appendDeclaration(bar, "-fx-background-color", backgroundColor);
        appendDeclaration(bar, "-fx-opacity", transparency);

        StringBuilder css = new StringBuilder();
        if (option.length() > 0) {
            css.append(".screen-footer-option {\n").append(option).append("}\n");
        }
        if (isSafe(selectColor)) {
            css.append(".screen-footer-option-active {\n    -fx-background-color: ")
                    .append(selectColor.trim()).append(";\n}\n");
        }
        if (bar.length() > 0) {
            css.append(".screen-footer-bar {\n").append(bar).append("}\n");
        }
        return css.toString();
    }

    private static void appendDeclaration(StringBuilder block, String property, String value) {
        if (isSafe(value)) {
            block.append("    ").append(property).append(": ").append(value.trim()).append(";\n");
        }
    }

    /** Rejects blank values and anything that could break out of the declaration (CSS injection
     *  guard) — config is author-trusted but this keeps a stray value from corrupting the sheet. */
    private static boolean isSafe(String value) {
        return value != null && !value.isBlank()
                && value.indexOf(';') < 0 && value.indexOf('{') < 0
                && value.indexOf('}') < 0 && value.indexOf('<') < 0;
    }
}
