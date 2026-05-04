package com.eb.javafx.journal;

import com.eb.javafx.util.Validation;

/** Read/unread state for one generic journal entry. */
public record JournalEntryStatus(String entryId, boolean unlocked, boolean read) {
    public JournalEntryStatus {
        entryId = Validation.requireNonBlank(entryId, "Journal entry id is required.");
    }
}
