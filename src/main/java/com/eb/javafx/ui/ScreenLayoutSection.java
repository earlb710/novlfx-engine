package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/**
 * UI-neutral content section for reusable screen layouts.
 *
 * <p>The section describes display intent as ids, optional headings, text rows, and an optional semantic style
 * hook. It does not expose JavaFX controls so applications can build and test screen layout data before rendering.</p>
 */
public record ScreenLayoutSection(
        String id,
        String title,
        List<String> lines,
        String styleClass,
        List<String> lineIds,
        List<Map<String, String>> lineMetadata) {
    public ScreenLayoutSection {
        Validation.requireNonBlank(id, "Screen layout section id is required.");
        lines = List.copyOf(Validation.requireNonNull(lines, "Screen layout section lines are required."));
        lineIds = List.copyOf(Validation.requireNonNull(lineIds, "Screen layout section line ids are required."));
        lineMetadata = Validation.requireNonNull(lineMetadata, "Screen layout section line metadata is required.")
                .stream()
                .map(metadata -> Map.copyOf(Validation.requireNonNull(metadata, "Screen layout section line metadata entry is required.")))
                .toList();
        lines.forEach(line -> Validation.requireNonBlank(line, "Screen layout section line text is required."));
        lineIds.forEach(lineId -> Validation.requireNonBlank(lineId, "Screen layout section line id is required."));
        if (!lineIds.isEmpty() && lineIds.size() != lines.size()) {
            throw new IllegalArgumentException("Screen layout section line ids must match line count.");
        }
        if (!lineMetadata.isEmpty() && lineMetadata.size() != lines.size()) {
            throw new IllegalArgumentException("Screen layout section line metadata must match line count.");
        }
    }

    public ScreenLayoutSection(String id, String title, List<String> lines, String styleClass, List<String> lineIds) {
        this(id, title, lines, styleClass, lineIds, List.of());
    }

    public ScreenLayoutSection(String id, String title, List<String> lines, String styleClass) {
        this(id, title, lines, styleClass, List.of());
    }

    public ScreenLayoutSection(String id, String title, List<String> lines) {
        this(id, title, lines, null);
    }
}
