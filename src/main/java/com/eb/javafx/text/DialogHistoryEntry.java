package com.eb.javafx.text;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable record of one dialog lifecycle in history.
 *
 * <p>An entry starts with a game date/time stamp, accumulates content messages,
 * and receives an end stamp when the dialog closes.</p>
 */
public final class DialogHistoryEntry {
    private final String dialogId;
    private final GameDateTime startedAt;
    private final GameDateTime endedAt;
    private final List<DialogMessage> messages;

    public DialogHistoryEntry(String dialogId, GameDateTime startedAt, GameDateTime endedAt, List<DialogMessage> messages) {
        this.dialogId = Validation.requireNonBlank(dialogId, "Dialog id is required.");
        this.startedAt = Validation.requireNonNull(startedAt, "Dialog start date is required.");
        this.endedAt = endedAt;
        this.messages = List.copyOf(messages == null ? List.of() : messages);
    }

    public static DialogHistoryEntry started(String dialogId, GameDateTime startedAt) {
        return new DialogHistoryEntry(dialogId, startedAt, null, List.of());
    }

    public DialogHistoryEntry withMessage(DialogMessage message) {
        if (endedAt != null) {
            throw new IllegalStateException("Dialog history entry is already ended.");
        }
        List<DialogMessage> updatedMessages = new ArrayList<>(messages);
        updatedMessages.add(Validation.requireNonNull(message, "Dialog message is required."));
        return new DialogHistoryEntry(dialogId, startedAt, null, updatedMessages);
    }

    public DialogHistoryEntry ended(GameDateTime endedAt) {
        if (this.endedAt != null) {
            throw new IllegalStateException("Dialog history entry is already ended.");
        }
        return new DialogHistoryEntry(
                dialogId,
                startedAt,
                Validation.requireNonNull(endedAt, "Dialog end date is required."),
                messages);
    }

    public String dialogId() {
        return dialogId;
    }

    public GameDateTime startedAt() {
        return startedAt;
    }

    public GameDateTime endedAt() {
        return endedAt;
    }

    public boolean isOpen() {
        return endedAt == null;
    }

    public List<DialogMessage> messages() {
        return messages;
    }
}
