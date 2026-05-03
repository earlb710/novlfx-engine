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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        ConversationHistoryViewModel viewModel = viewModel(context);
        return context.themedScene(ScreenShell.titled(viewModel.title(), createContent(context, viewModel)));
    }

    public static ConversationHistoryViewModel viewModel(RouteContext context) {
        return viewModel(
                context.contentRegistry().definition("ui.conversationHistory.title"),
                context.gameState());
    }

    public static ConversationHistoryViewModel viewModel(String title, GameState gameState) {
        List<String> messages;
        List<ConversationHistoryEntryViewModel> entries;
        if (gameState == null) {
            messages = List.of("Conversation history is unavailable.");
            entries = List.of();
        } else {
            messages = messagesFor(gameState.conversationHistory());
            entries = entriesFor(gameState.conversationHistory());
        }
        return new ConversationHistoryViewModel(
                title,
                messages,
                entries,
                List.of(new ScreenActionViewModel("Back to main menu", SceneRouter.MAIN_MENU_ROUTE, true)));
    }

    private static VBox createContent(RouteContext context, ConversationHistoryViewModel viewModel) {
        VBox content = new VBox(ScreenShell.BODY_SPACING);
        for (String message : viewModel.messages()) {
            content.getChildren().add(new Label(message));
        }
        for (ConversationHistoryEntryViewModel entry : viewModel.entries()) {
            content.getChildren().add(entryPanel(entry));
        }
        for (ScreenActionViewModel action : viewModel.actions()) {
            Button button = ScreenNavigation.button(context, action.label(), action.routeId());
            button.setDisable(!action.enabled());
            content.getChildren().add(button);
        }
        return content;
    }

    private static VBox entryPanel(ConversationHistoryEntryViewModel entry) {
        VBox panel = ScreenShell.styledPanel(null);
        panel.getChildren().add(new Label(formatEntryHeader(entry)));
        for (ConversationHistoryRowViewModel row : entry.rows()) {
            panel.getChildren().add(new Label("  " + rowText(row)));
        }
        return panel;
    }

    private static String formatEntryHeader(ConversationHistoryEntryViewModel entry) {
        return entry.dialogId() + " started " + entry.startedAt() + " with " + entry.participants() + " (" + entry.status() + ")";
    }

    private static String rowText(ConversationHistoryRowViewModel row) {
        if (row.speakerLabel() != null && !row.speakerLabel().isBlank()) {
            return row.speakerLabel() + ": " + row.text();
        }
        return row.columns().stream()
                .map(column -> column.id() + ": " + column.text())
                .collect(Collectors.joining(" | "));
    }

    private static List<String> messagesFor(DialogHistory history) {
        return history.entries().isEmpty()
                ? List.of("No conversations have been recorded yet.")
                : List.of();
    }

    private static List<ConversationHistoryEntryViewModel> entriesFor(DialogHistory history) {
        return history.entries().stream()
                .map(ConversationHistoryScreen::entryViewModel)
                .toList();
    }

    private static ConversationHistoryEntryViewModel entryViewModel(DialogHistoryEntry entry) {
        return new ConversationHistoryEntryViewModel(
                entry.dialogId(),
                entry.startedAt().toString(),
                entry.isOpen() ? "open" : "ended " + entry.endedAt(),
                participants(entry),
                entry.messages().stream()
                        .map(ConversationHistoryScreen::rowViewModel)
                        .toList());
    }

    private static ConversationHistoryRowViewModel rowViewModel(DialogMessage message) {
        return new ConversationHistoryRowViewModel(
                message.hasSpeaker() ? message.speaker().label() : null,
                messageText(message),
                message.columns().stream()
                        .map(column -> new ConversationHistoryColumnViewModel(column.id(), tokensText(column.tokens())))
                        .toList());
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

    private static String messageText(DialogMessage message) {
        if (message.hasSpeaker()) {
            return columnText(message, DialogColumn.MESSAGE_COLUMN);
        }
        return message.columns().stream()
                .map(column -> column.id() + ": " + tokensText(column.tokens()))
                .collect(Collectors.joining(" | "));
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
