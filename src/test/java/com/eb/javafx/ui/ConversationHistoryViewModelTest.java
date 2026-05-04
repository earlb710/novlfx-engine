package com.eb.javafx.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class ConversationHistoryViewModelTest {
    @Test
    void rejectsBlankConversationHistoryColumnId() {
        assertThrows(IllegalArgumentException.class, () -> new ConversationHistoryColumnViewModel(" ", "Text"));
    }

    @Test
    void rejectsBlankConversationHistoryRowText() {
        assertThrows(IllegalArgumentException.class, () -> new ConversationHistoryRowViewModel(null, " ", List.of()));
    }

    @Test
    void rejectsBlankConversationHistoryTitle() {
        assertThrows(IllegalArgumentException.class, () -> new ConversationHistoryViewModel(" ", List.of(), List.of(), List.of()));
    }
}
