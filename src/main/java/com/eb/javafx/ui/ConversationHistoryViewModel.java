package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;

import java.util.List;

/**
 * Structured conversation-history screen model.
 */
public record ConversationHistoryViewModel(
        String title,
        List<String> messages,
        List<ConversationHistoryEntryViewModel> entries,
        List<ScreenActionViewModel> actions) {
    public ConversationHistoryViewModel {
        Validation.requireNonBlank(title, "Conversation history title is required.");
        messages = List.copyOf(Validation.requireNonNull(messages, "Conversation history messages are required."));
        entries = List.copyOf(Validation.requireNonNull(entries, "Conversation history entries are required."));
        actions = List.copyOf(Validation.requireNonNull(actions, "Conversation history actions are required."));
        messages.forEach(message -> Validation.requireNonBlank(message, "Conversation history message text is required."));
    }
}
