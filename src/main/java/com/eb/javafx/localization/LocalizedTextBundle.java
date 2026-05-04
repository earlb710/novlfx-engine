package com.eb.javafx.localization;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Immutable language-specific text bundle keyed by stable authored text IDs. */
public record LocalizedTextBundle(String languageId, Map<String, String> texts) {
    public LocalizedTextBundle {
        languageId = Validation.requireNonBlank(languageId, "Language id is required.");
        texts = ImmutableCollections.copyMap(texts);
        texts.forEach((id, text) -> {
            Validation.requireNonBlank(id, "Localized text id is required.");
            Validation.requireNonNull(text, "Localized text value is required.");
        });
    }

    public Optional<String> text(String textId) {
        return Optional.ofNullable(texts.get(textId));
    }

    public String textOr(String textId, String fallback) {
        return text(textId).orElse(fallback);
    }

    public Set<String> missingTextIds(Collection<String> requiredTextIds) {
        Set<String> missing = new LinkedHashSet<>();
        if (requiredTextIds == null) {
            return Set.of();
        }
        for (String id : requiredTextIds) {
            if (!texts.containsKey(id)) {
                missing.add(id);
            }
        }
        return Set.copyOf(missing);
    }

    public LocalizedTextBundle withText(String textId, String text) {
        java.util.Map<String, String> updated = new java.util.LinkedHashMap<>(texts);
        updated.put(
                Validation.requireNonBlank(textId, "Localized text id is required."),
                Validation.requireNonNull(text, "Localized text value is required."));
        return new LocalizedTextBundle(languageId, updated);
    }
}
