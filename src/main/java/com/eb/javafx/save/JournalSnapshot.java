package com.eb.javafx.save;

import com.eb.javafx.journal.JournalState;
import com.eb.javafx.util.Validation;

import java.util.LinkedHashSet;
import java.util.Set;

/** Immutable save snapshot of reusable journal or quest read/unlocked state. */
public record JournalSnapshot(Set<String> unlockedEntryIds, Set<String> readEntryIds) {
    public JournalSnapshot {
        unlockedEntryIds = unlockedEntryIds == null || unlockedEntryIds.isEmpty()
                ? Set.of()
                : Set.copyOf(new LinkedHashSet<>(unlockedEntryIds));
        readEntryIds = readEntryIds == null || readEntryIds.isEmpty()
                ? Set.of()
                : Set.copyOf(new LinkedHashSet<>(readEntryIds));
        unlockedEntryIds.forEach(id -> Validation.requireNonBlank(id, "Unlocked journal entry id is required."));
        readEntryIds.forEach(id -> Validation.requireNonBlank(id, "Read journal entry id is required."));
    }

    public static JournalSnapshot empty() {
        return new JournalSnapshot(Set.of(), Set.of());
    }

    public static JournalSnapshot fromState(JournalState state) {
        JournalState checkedState = Validation.requireNonNull(state, "Journal state is required.");
        return new JournalSnapshot(checkedState.unlockedEntryIds(), checkedState.readEntryIds());
    }

    public JournalState toState() {
        JournalState state = new JournalState();
        unlockedEntryIds.forEach(state::unlock);
        readEntryIds.forEach(state::markRead);
        return state;
    }
}
