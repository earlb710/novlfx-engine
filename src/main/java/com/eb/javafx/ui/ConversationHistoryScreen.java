package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.state.GameState;
import com.eb.javafx.text.DialogColumn;
import com.eb.javafx.text.DialogHistory;
import com.eb.javafx.text.DialogHistoryEntry;
import com.eb.javafx.text.DialogMessage;
import com.eb.javafx.text.DialogSpeaker;
import com.eb.javafx.text.TextToken;
import com.eb.javafx.text.TextTokenType;
import javafx.scene.Scene;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reusable helper route for reviewing past conversations.
 *
 * <p>The screen summarizes when each conversation started, who participated, and
 * the recorded speaker/message rows so players can review what the main
 * character discussed without needing authored game-specific UI.</p>
 */
public final class ConversationHistoryScreen {
    private ConversationHistoryScreen() {
    }

    public static Scene createScene(RouteContext context) {
        return ViewModelScreen.createScene(context, viewModel(context));
    }

    public static ScreenViewModel viewModel(RouteContext context) {
        return viewModel(
                context.contentRegistry().definition("ui.conversationHistory.title"),
                context.gameState());
    }

    public static ScreenViewModel viewModel(String title, GameState gameState) {
        List<String> lines;
        if (gameState == null) {
            lines = List.of("Conversation history is unavailable.");
        } else {
            lines = linesFor(gameState.conversationHistory());
        }
        return new ScreenViewModel(
                title,
                lines,
                List.of(new ScreenActionViewModel("Back to main menu", SceneRouter.MAIN_MENU_ROUTE, true)));
    }

    private static List<String> linesFor(DialogHistory history) {
        List<DialogHistoryEntry> entries = history.entries();
        if (entries.isEmpty()) {
            return List.of("No conversations have been recorded yet.");
        }
        return entries.stream()
                .flatMap(entry -> linesFor(entry).stream())
                .toList();
    }

    private static List<String> linesFor(DialogHistoryEntry entry) {
        List<String> lines = new java.util.ArrayList<>();
        String participants = participants(entry);
        String status = entry.isOpen() ? "open" : "ended " + entry.endedAt();
        lines.add(entry.dialogId() + " started " + entry.startedAt() + " with " + participants + " (" + status + ")");
        for (DialogMessage message : entry.messages()) {
            lines.add("  " + messageLine(message));
        }
        return List.copyOf(lines);
    }

    private static String participants(DialogHistoryEntry entry) {
        Set<String> names = new LinkedHashSet<>();
        for (DialogMessage message : entry.messages()) {
            DialogSpeaker speaker = message.speaker();
            if (speaker != null) {
                names.add(speaker.label());
            }
        }
        return names.isEmpty() ? "unknown participants" : String.join(", ", names);
    }

    private static String messageLine(DialogMessage message) {
        if (message.hasSpeaker()) {
            return message.speaker().label() + ": " + columnText(message, DialogColumn.MESSAGE_COLUMN);
        }
        return message.columns().stream()
                .map(column -> column.id() + ": " + tokensText(column.tokens()))
                .collect(java.util.stream.Collectors.joining(" | "));
    }

    private static String columnText(DialogMessage message, String columnId) {
        return message.columns().stream()
                .filter(column -> columnId.equals(column.id()))
                .findFirst()
                .map(column -> tokensText(column.tokens()))
                .orElse("");
    }

    private static String tokensText(List<TextToken> tokens) {
        StringBuilder text = new StringBuilder();
        for (TextToken token : tokens) {
            if (token.type() == TextTokenType.TEXT) {
                text.append(token.text());
            } else if (token.type() == TextTokenType.PARAGRAPH) {
                text.append(" ");
            }
        }
        return text.toString().trim();
    }
}
