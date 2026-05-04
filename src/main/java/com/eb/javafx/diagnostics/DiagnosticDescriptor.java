package com.eb.javafx.diagnostics;

import com.eb.javafx.gamesupport.IdentifiedDefinition;
import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/** Descriptor for reusable diagnostic checks exposed to debug UI. */
public record DiagnosticDescriptor(
        String id,
        String title,
        DiagnosticSeverity defaultSeverity,
        List<String> tags,
        Map<String, String> metadata) implements IdentifiedDefinition {
    public DiagnosticDescriptor {
        id = Validation.requireNonBlank(id, "Diagnostic descriptor id is required.");
        title = Validation.requireNonBlank(title, "Diagnostic descriptor title is required.");
        defaultSeverity = Validation.requireNonNull(defaultSeverity, "Diagnostic descriptor severity is required.");
        tags = ImmutableCollections.copyList(tags);
        tags.forEach(tag -> Validation.requireNonBlank(tag, "Diagnostic descriptor tag is required."));
        metadata = ImmutableCollections.copyMap(metadata);
    }
}
