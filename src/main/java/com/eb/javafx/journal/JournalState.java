package com.eb.javafx.journal;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashSet;
import java.util.Set;

/** Mutable generic journal state for unlocked and read entries. */
public final class JournalState {
    private final Set<String> unlocked = new LinkedHashSet<>();
    private final Set<String> read = new LinkedHashSet<>();

    public void unlock(String entryId) {
        unlocked.add(Validation.requireNonBlank(entryId, "Journal entry id is required."));
    }

    public void markRead(String entryId) {
        String checkedEntryId = Validation.requireNonBlank(entryId, "Journal entry id is required.");
        unlock(checkedEntryId);
        read.add(checkedEntryId);
    }

    public JournalEntryStatus status(String entryId) {
        String checkedEntryId = Validation.requireNonBlank(entryId, "Journal entry id is required.");
        return new JournalEntryStatus(checkedEntryId, unlocked.contains(checkedEntryId), read.contains(checkedEntryId));
    }
}
