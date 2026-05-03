package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.List;

/**
 * Structured conversation-history message row.
 */
public record ConversationHistoryRowViewModel(
        String speakerLabel,
        String text,
        List<ConversationHistoryColumnViewModel> columns) {
    public ConversationHistoryRowViewModel {
        text = Validation.requireNonBlank(text, "Conversation history row text is required.");
        columns = List.copyOf(Validation.requireNonNull(columns, "Conversation history row columns are required."));
    }
}
