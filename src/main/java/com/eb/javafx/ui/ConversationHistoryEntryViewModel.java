package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.List;

/**
 * Structured conversation-history entry.
 */
public record ConversationHistoryEntryViewModel(
        String dialogId,
        String startedAt,
        String status,
        String participants,
        List<ConversationHistoryRowViewModel> rows) {
    public ConversationHistoryEntryViewModel {
        Validation.requireNonBlank(dialogId, "Conversation history dialog id is required.");
        Validation.requireNonBlank(startedAt, "Conversation history start text is required.");
        Validation.requireNonBlank(status, "Conversation history status is required.");
        Validation.requireNonBlank(participants, "Conversation history participants are required.");
        rows = List.copyOf(Validation.requireNonNull(rows, "Conversation history rows are required."));
    }
}
