package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

/**
 * Theme-driven color overrides applied to {@link DisplayDefaults} role and block metadata.
 *
 * <p>Carries text-fill and background colors for the six render roles plus the
 * container block so that screen rendering tracks the active theme variant rather
 * than the hardcoded values in the bundled defaults JSON.</p>
 */
public record RoleColors(
        String textColor,
        String textBackground,
        String headingColor,
        String headingBackground,
        String subheadingColor,
        String subheadingBackground,
        String fieldColor,
        String fieldBackground,
        String buttonColor,
        String buttonBackground,
        String fieldLabelColor,
        String blockColor,
        String blockBackground,
        String blockBorderColor) {
    public RoleColors {
        Validation.requireNonBlank(textColor, "textColor is required.");
        Validation.requireNonBlank(textBackground, "textBackground is required.");
        Validation.requireNonBlank(headingColor, "headingColor is required.");
        Validation.requireNonBlank(headingBackground, "headingBackground is required.");
        Validation.requireNonBlank(subheadingColor, "subheadingColor is required.");
        Validation.requireNonBlank(subheadingBackground, "subheadingBackground is required.");
        Validation.requireNonBlank(fieldColor, "fieldColor is required.");
        Validation.requireNonBlank(fieldBackground, "fieldBackground is required.");
        Validation.requireNonBlank(buttonColor, "buttonColor is required.");
        Validation.requireNonBlank(buttonBackground, "buttonBackground is required.");
        Validation.requireNonBlank(fieldLabelColor, "fieldLabelColor is required.");
        Validation.requireNonBlank(blockColor, "blockColor is required.");
        Validation.requireNonBlank(blockBackground, "blockBackground is required.");
        Validation.requireNonBlank(blockBorderColor, "blockBorderColor is required.");
    }
}
