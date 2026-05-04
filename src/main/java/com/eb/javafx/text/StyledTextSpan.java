package com.eb.javafx.text;

import com.eb.javafx.util.Validation;

import java.util.List;

/** Rendering-neutral styled text span with parsed effect metadata. */
public record StyledTextSpan(String text, TextStyle style, List<TextEffect> effects) {
    public StyledTextSpan {
        text = Validation.requireNonNull(text, "Styled text span text is required.");
        style = Validation.requireNonNull(style, "Styled text span style is required.");
        effects = List.copyOf(Validation.requireNonNull(effects, "Styled text span effects are required."));
    }
}
