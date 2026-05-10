package com.eb.javafx.util;

import java.util.Map;

/** Replaces simple {@code $name} and {@code ${name}} text placeholders with supplied values. */
public final class TextPlaceholders {
    private TextPlaceholders() {
    }

    public static String resolve(String text, Map<String, String> bindings) {
        String resolved = Validation.requireNonBlank(text, "Text is required.");
        for (Map.Entry<String, String> binding : Validation.requireNonNull(bindings, "Text bindings are required.").entrySet()) {
            String value = binding.getValue() == null ? "" : binding.getValue();
            resolved = resolved.replace("${" + binding.getKey() + "}", value)
                    .replace("$" + binding.getKey(), value);
        }
        return resolved;
    }
}
