package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

/**
 * Structured conversation-history column preview.
 */
public record ConversationHistoryColumnViewModel(String id, String text) {
    public ConversationHistoryColumnViewModel {
        Validation.requireNonBlank(id, "Conversation history column id is required.");
        Validation.requireNonBlank(text, "Conversation history column text is required.");
    }
}
