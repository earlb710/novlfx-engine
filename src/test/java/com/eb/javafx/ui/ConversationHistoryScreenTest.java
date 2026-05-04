package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.state.GameState;
import com.eb.javafx.text.DialogColumn;
import com.eb.javafx.text.DialogMessage;
import com.eb.javafx.text.DialogSpeaker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ConversationHistoryScreenTest {
    @Test
    void showsEmptyConversationHistory() {
        ConversationHistoryViewModel viewModel = ConversationHistoryScreen.viewModel("Conversation History", new GameState("main-menu"));

        assertEquals("Conversation History", viewModel.title());
        assertEquals(List.of("No conversations have been recorded yet."), viewModel.messages());
        assertEquals(List.of(), viewModel.entries());
        assertEquals("Back to main menu", viewModel.actions().get(0).label());
    }

    @Test
    void summarizesWhenConversationStartedWhoParticipatedAndMessages() {
        GameState gameState = new GameState("main-menu");
        DialogSpeaker ava = DialogSpeaker.iconText("ava", "Ava", "icons/ava", "#ffcc00", "Serif");
        DialogSpeaker mc = DialogSpeaker.text("mc", "MC");

        gameState.conversationHistory().beginDialog("dock-talk", new GameDateTime(3, "evening"));
        gameState.conversationHistory().addMessage(ava, "{font=Serif}Meet me by the docks.{/font}");
        gameState.conversationHistory().addMessage(mc, "I'll be there.");
        gameState.conversationHistory().addMessage(DialogMessage.columns(List.of(
                DialogColumn.parsed("note", "{i}A folded map changes hands.{/i}"))));
        gameState.conversationHistory().endDialog(new GameDateTime(3, "night"));

        ConversationHistoryViewModel viewModel = ConversationHistoryScreen.viewModel("Conversation History", gameState);

        assertEquals(List.of(), viewModel.messages());
        assertEquals(1, viewModel.entries().size());

        ConversationHistoryEntryViewModel entry = viewModel.entries().get(0);
        assertEquals("dock-talk", entry.dialogId());
        assertEquals("day 3 evening", entry.startedAt());
        assertEquals("ended day 3 night", entry.status());
        assertEquals("Ava, MC", entry.participants());
        assertEquals(3, entry.rows().size());

        assertEquals("Ava", entry.rows().get(0).speakerLabel());
        assertEquals("Meet me by the docks.", entry.rows().get(0).text());
        assertEquals("message", entry.rows().get(0).columns().get(1).id());
        assertEquals("Meet me by the docks.", entry.rows().get(0).columns().get(1).text());

        assertEquals("MC", entry.rows().get(1).speakerLabel());
        assertEquals("I'll be there.", entry.rows().get(1).text());

        assertEquals(null, entry.rows().get(2).speakerLabel());
        assertEquals("note: A folded map changes hands.", entry.rows().get(2).text());
        assertEquals(List.of(new ConversationHistoryColumnViewModel("note", "A folded map changes hands.")), entry.rows().get(2).columns());
    }

    @Test
    void marksOpenConversationHistoryEntry() {
        GameState gameState = new GameState("main-menu");
        gameState.conversationHistory().beginDialog("current", new GameDateTime(1, "default"));

        ConversationHistoryViewModel viewModel = ConversationHistoryScreen.viewModel("Conversation History", gameState);

        assertEquals(List.of(), viewModel.messages());
        assertEquals(1, viewModel.entries().size());
        assertEquals("current", viewModel.entries().get(0).dialogId());
        assertEquals("day 1 default", viewModel.entries().get(0).startedAt());
        assertEquals("unknown participants", viewModel.entries().get(0).participants());
        assertEquals("open", viewModel.entries().get(0).status());
    }
}
