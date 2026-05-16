package com.eb.javafx.text;

import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mutable dialog history accumulator for an active play session.
 *
 * <p>The support methods enforce a single open dialog at a time: start a dated
 * dialog, append formatted content rows, then end it with a closing date stamp.</p>
 */
public final class DialogHistory {
    /**
     * Maximum number of conversation entries retained in memory. Once this limit is reached,
     * the oldest entry is dropped each time a new conversation begins, so the history acts as a
     * sliding window of the most recent 1 000 conversations.
     */
    public static final int MAX_CONVERSATIONS = 1000;

    private final List<DialogHistoryEntry> entries = new ArrayList<>();
    private int openEntryIndex = -1;

    public DialogHistoryEntry beginDialog(String dialogId, GameDateTime startedAt) {
        if (openEntryIndex >= 0) {
            throw new IllegalStateException("A dialog history entry is already open.");
        }
        DialogHistoryEntry entry = DialogHistoryEntry.started(dialogId, startedAt);
        entries.add(entry);
        // Trim the oldest conversation once the sliding window is full. openEntryIndex always
        // points at the last element, so the index stays valid after the remove.
        if (entries.size() > MAX_CONVERSATIONS) {
            entries.remove(0);
        }
        openEntryIndex = entries.size() - 1;
        return entry;
    }

    public DialogHistoryEntry beginDialog(String dialogId, GameClock clock) {
        Validation.requireNonNull(clock, "Dialog history clock is required.");
        return beginDialog(dialogId, clock.currentTime());
    }

    public DialogHistoryEntry addMessage(DialogMessage message) {
        ensureOpen();
        DialogHistoryEntry updatedEntry = entries.get(openEntryIndex).withMessage(message);
        entries.set(openEntryIndex, updatedEntry);
        return updatedEntry;
    }

    public DialogHistoryEntry addMessage(DialogSpeaker speaker, String message) {
        return addMessage(DialogMessage.speakerMessage(speaker, message));
    }

    public Optional<DialogMessage> removeLastMessage() {
        ensureOpen();
        DialogHistoryEntry entry = entries.get(openEntryIndex);
        if (entry.messages().isEmpty()) {
            return Optional.empty();
        }
        DialogMessage removed = entry.messages().get(entry.messages().size() - 1);
        entries.set(openEntryIndex, entry.withoutLastMessage());
        return Optional.of(removed);
    }

    public DialogHistoryEntry endDialog(GameDateTime endedAt) {
        ensureOpen();
        DialogHistoryEntry updatedEntry = entries.get(openEntryIndex).ended(endedAt);
        entries.set(openEntryIndex, updatedEntry);
        openEntryIndex = -1;
        return updatedEntry;
    }

    public DialogHistoryEntry endDialog(GameClock clock) {
        Validation.requireNonNull(clock, "Dialog history clock is required.");
        return endDialog(clock.currentTime());
    }

    public List<DialogHistoryEntry> entries() {
        return List.copyOf(entries);
    }

    public Optional<DialogHistoryEntry> openDialog() {
        return openEntryIndex < 0 ? Optional.empty() : Optional.of(entries.get(openEntryIndex));
    }

    private void ensureOpen() {
        if (openEntryIndex < 0) {
            throw new IllegalStateException("No dialog history entry is open.");
        }
    }
}
