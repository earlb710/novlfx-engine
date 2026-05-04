package com.eb.javafx.debug;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.Map;

/** Reusable debug view model containing labeled diagnostic rows. */
public record DebugSnapshot(String title, Map<String, String> rows) {
    public DebugSnapshot {
        title = Validation.requireNonBlank(title, "Debug snapshot title is required.");
        rows = ImmutableCollections.copyMap(rows);
    }
}
