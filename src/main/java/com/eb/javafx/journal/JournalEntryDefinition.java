package com.eb.javafx.journal;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;

/** Generic journal, quest, task, or log entry definition. */
public record JournalEntryDefinition(String id, String title, String categoryId, int sortOrder, List<String> tags) {
    public JournalEntryDefinition {
        id = Validation.requireNonBlank(id, "Journal entry id is required.");
        title = Validation.requireNonBlank(title, "Journal entry title is required.");
        categoryId = categoryId == null ? "" : categoryId;
        tags = ImmutableCollections.copyList(tags);
    }
}
