package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.state.GameState;
import com.eb.javafx.text.DialogColumn;
import com.eb.javafx.text.DialogMessage;
import com.eb.javafx.text.DialogSpeaker;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

final class ConversationHistoryScreenTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();
    private static final AtomicBoolean JAVAFX_AVAILABLE = new AtomicBoolean(true);

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

    @Test
    void rendersHistoryRowsAsTwoMultilineColumnsWithRightAlignedSpeaker() throws Exception {
        GameState gameState = new GameState("main-menu");
        DialogSpeaker ava = DialogSpeaker.text("ava", "Ava");
        gameState.conversationHistory().beginDialog("dock-talk", new GameDateTime(3, "evening"));
        gameState.conversationHistory().addMessage(ava, "Meet me by the docks.");

        ConversationHistoryEntryViewModel entry = ConversationHistoryScreen.viewModel("Conversation History", gameState)
                .entries()
                .get(0);
        AtomicReference<GridPane> rowsReference = new AtomicReference<>();

        runOnJavaFxThread(() -> rowsReference.set(ConversationHistoryScreen.historyRows(entry)));

        GridPane rows = rowsReference.get();
        runOnJavaFxThread(() -> {
            assertEquals(2, rows.getColumnCount());
            assertEquals(HPos.RIGHT, rows.getColumnConstraints().get(0).getHalignment());
            Label speaker = (Label) rows.getChildren().get(0);
            Label message = (Label) rows.getChildren().get(1);
            assertEquals("Ava", speaker.getText());
            assertEquals("Meet me by the docks.", message.getText());
            assertEquals(0, GridPane.getColumnIndex(speaker));
            assertEquals(1, GridPane.getColumnIndex(message));
            assertTrue(speaker.isWrapText());
            assertTrue(message.isWrapText());
        });
    }

    private static void runOnJavaFxThread(Runnable action) throws Exception {
        assumeTrue(startJavaFxToolkit());
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });
        assertTrue(completed.await(5, TimeUnit.SECONDS), "JavaFX action did not complete.");
        if (failure.get() instanceof Exception exception) {
            throw exception;
        }
        if (failure.get() instanceof Error error) {
            throw error;
        }
        assertNull(failure.get(), () -> "JavaFX action failed: " + failure.get());
    }

    private static boolean startJavaFxToolkit() throws InterruptedException {
        if (!JAVAFX_AVAILABLE.get()) {
            return false;
        }

        CountDownLatch started = new CountDownLatch(1);
        if (JAVAFX_STARTED.compareAndSet(false, true)) {
            try {
                Platform.startup(() -> {
                    Platform.setImplicitExit(false);
                    started.countDown();
                });
            } catch (IllegalStateException exception) {
                return markJavaFxAvailableIfRunning(started);
            } catch (UnsupportedOperationException exception) {
                JAVAFX_AVAILABLE.set(false);
                started.countDown();
                return false;
            }
        } else {
            Platform.setImplicitExit(false);
            started.countDown();
        }
        assertTrue(started.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start.");
        return true;
    }

    private static boolean markJavaFxAvailableIfRunning(CountDownLatch started) throws InterruptedException {
        try {
            Platform.runLater(started::countDown);
        } catch (IllegalStateException exception) {
            JAVAFX_AVAILABLE.set(false);
            started.countDown();
            return false;
        }
        if (!started.await(1, TimeUnit.SECONDS)) {
            JAVAFX_AVAILABLE.set(false);
            return false;
        }
        Platform.setImplicitExit(false);
        return true;
    }
}
