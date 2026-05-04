package com.eb.javafx.input;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.Set;

/** Device input trigger such as a key, mouse button, or controller button plus modifiers. */
public record InputTrigger(InputDevice device, String code, Set<String> modifiers) {
    public InputTrigger {
        device = Validation.requireNonNull(device, "Input device is required.");
        code = Validation.requireNonBlank(code, "Input code is required.");
        modifiers = modifiers == null || modifiers.isEmpty() ? Set.of() : Set.copyOf(modifiers);
    }
}
