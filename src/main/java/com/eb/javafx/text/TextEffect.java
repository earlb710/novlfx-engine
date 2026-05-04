package com.eb.javafx.text;

import com.eb.javafx.util.Validation;

import java.util.Map;

/** Rendering-neutral rich-text effect metadata such as gradient, kinetic, or glitch. */
public record TextEffect(String id, Map<String, String> parameters) {
    public TextEffect {
        id = Validation.requireNonBlank(id, "Text effect id is required.");
        parameters = Map.copyOf(Validation.requireNonNull(parameters, "Text effect parameters are required."));
    }
}
