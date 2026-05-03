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
        ScreenViewModel viewModel = ConversationHistoryScreen.viewModel("Conversation History", new GameState("main-menu"));

        assertEquals("Conversation History", viewModel.title());
        assertEquals(List.of("No conversations have been recorded yet."), viewModel.lines());
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

        ScreenViewModel viewModel = ConversationHistoryScreen.viewModel("Conversation History", gameState);

        assertEquals(List.of(
                "dock-talk started day 3 evening with Ava, MC (ended day 3 night)",
                "  Ava: Meet me by the docks.",
                "  MC: I'll be there.",
                "  note: A folded map changes hands."), viewModel.lines());
    }

    @Test
    void marksOpenConversationHistoryEntry() {
        GameState gameState = new GameState("main-menu");
        gameState.conversationHistory().beginDialog("current", new GameDateTime(1, "default"));

        ScreenViewModel viewModel = ConversationHistoryScreen.viewModel("Conversation History", gameState);

        assertEquals(List.of("current started day 1 default with unknown participants (open)"), viewModel.lines());
    }
}
