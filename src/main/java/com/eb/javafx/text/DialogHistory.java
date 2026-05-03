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
    private final List<DialogHistoryEntry> entries = new ArrayList<>();
    private int openEntryIndex = -1;

    public DialogHistoryEntry beginDialog(String dialogId, GameDateTime startedAt) {
        if (openEntryIndex >= 0) {
            throw new IllegalStateException("A dialog history entry is already open.");
        }
        DialogHistoryEntry entry = DialogHistoryEntry.started(dialogId, startedAt);
        entries.add(entry);
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
