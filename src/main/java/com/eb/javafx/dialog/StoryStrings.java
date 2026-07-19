package com.eb.javafx.dialog;

import java.util.regex.Pattern;

/** Shared validation helpers for story-system identifiers and text fields. */
final class StoryStrings {
    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:#-]+");

    private StoryStrings() {
    }

    static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    static String requireKey(String value, String message) {
        String trimmed = requireText(value, message);
        if (!KEY_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(message + " Got: " + value);
        }
        return trimmed;
    }
}
