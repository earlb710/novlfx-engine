package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.scene.ConversationDefinition.LineType;
import com.eb.javafx.text.DialogHistory;
import com.eb.javafx.text.DialogHistoryEntry;
import com.eb.javafx.text.DialogSpeaker;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DialogEntriesViewTest {
    private static final AtomicBoolean JAVAFX_STARTED = new AtomicBoolean();

    @BeforeAll
    static void initializeJavaFxToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        if (JAVAFX_STARTED.compareAndSet(false, true)) {
            try {
                Platform.startup(() -> {
                    Platform.setImplicitExit(false);
                    started.countDown();
                });
            } catch (IllegalStateException exception) {
                Platform.setImplicitExit(false);
                started.countDown();
            }
        } else {
            Platform.setImplicitExit(false);
            started.countDown();
        }
        assertTrue(started.await(5, TimeUnit.SECONDS), "JavaFX toolkit did not start.");
    }

    @Test
    void emptyViewHasNoChildrenAndNoNavigation() {
        DialogEntriesView view = new DialogEntriesView();

        assertEquals(0, view.getChildren().size());
        assertEquals(-1, view.currentIndex());
        assertFalse(view.canGoBack());
        assertFalse(view.canGoForward());
        assertTrue(view.entries().isEmpty());
    }

    @Test
    void addEntryAppendsAndMakesItCurrent() {
        DialogEntriesView view = new DialogEntriesView();

        view.addEntry("Hello.");
        view.addEntry("How are you?");

        assertEquals(List.of("Hello.", "How are you?"), view.entries());
        assertEquals(1, view.currentIndex());
        assertTrue(view.canGoBack());
        assertFalse(view.canGoForward());
    }

    @Test
    void newestEntryIsBottomChildAtFullOpacityAndPreviousAreFaded() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.addEntry("Line 3.");

        List<Node> children = view.getChildren();
        assertEquals(3, children.size());

        Label firstShown = (Label) children.get(0);
        Label secondShown = (Label) children.get(1);
        Label bottom = (Label) children.get(2);

        assertEquals("Line 1.", firstShown.getText());
        assertEquals("Line 2.", secondShown.getText());
        assertEquals("Line 3.", bottom.getText());

        assertEquals(DialogEntriesView.PREVIOUS_ENTRY_OPACITY, firstShown.getOpacity(), 1e-9);
        assertEquals(DialogEntriesView.PREVIOUS_ENTRY_OPACITY, secondShown.getOpacity(), 1e-9);
        assertEquals(1.0, bottom.getOpacity(), 1e-9);

        assertTrue(firstShown.getStyleClass().contains(DialogEntriesView.PREVIOUS_ENTRY_STYLE_CLASS));
        assertTrue(bottom.getStyleClass().contains(DialogEntriesView.CURRENT_ENTRY_STYLE_CLASS));
    }

    @Test
    void goBackMovesCursorAndRedrawsWithEarlierEntryAsCurrent() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.addEntry("Line 3.");

        view.goBack();

        assertEquals(1, view.currentIndex());
        assertEquals(2, view.getChildren().size());
        Label previous = (Label) view.getChildren().get(0);
        Label current = (Label) view.getChildren().get(1);
        assertEquals("Line 1.", previous.getText());
        assertEquals("Line 2.", current.getText());
        assertEquals(DialogEntriesView.PREVIOUS_ENTRY_OPACITY, previous.getOpacity(), 1e-9);
        assertEquals(1.0, current.getOpacity(), 1e-9);
        assertTrue(view.canGoBack());
        assertTrue(view.canGoForward());
    }

    @Test
    void goForwardMovesCursorBackToLatestEntry() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");

        view.goBack();
        assertEquals(0, view.currentIndex());

        view.goForward();
        assertEquals(1, view.currentIndex());
        assertFalse(view.canGoForward());
    }

    @Test
    void navigationClampsAtEnds() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Only line.");

        view.goBack();
        assertEquals(0, view.currentIndex());
        view.goForward();
        assertEquals(0, view.currentIndex());
        assertFalse(view.canGoBack());
        assertFalse(view.canGoForward());
    }

    @Test
    void maxVisibleEntriesCapsRenderedHistory() {
        DialogEntriesView view = new DialogEntriesView();
        view.setMaxVisibleEntries(2);
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.addEntry("Line 3.");
        view.addEntry("Line 4.");

        assertEquals(2, view.getChildren().size());
        Label previous = (Label) view.getChildren().get(0);
        Label current = (Label) view.getChildren().get(1);
        assertEquals("Line 3.", previous.getText());
        assertEquals("Line 4.", current.getText());
    }

    @Test
    void setEntriesReplacesHistoryAndMovesCursorToLast() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Old.");

        view.setEntries(List.of("A", "B", "C"));

        assertEquals(List.of("A", "B", "C"), view.entries());
        assertEquals(2, view.currentIndex());
    }

    @Test
    void clearRemovesAllEntries() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("A");
        view.addEntry("B");

        view.clear();

        assertTrue(view.entries().isEmpty());
        assertEquals(-1, view.currentIndex());
        assertEquals(0, view.getChildren().size());
    }

    @Test
    void rejectsNullText() {
        DialogEntriesView view = new DialogEntriesView();

        assertThrows(IllegalArgumentException.class, () -> view.addEntry(null));
    }

    @Test
    void bindToFooterInstallsClickHandlersOnBackAndForwardLabels() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");

        HBox footer = ScreenShell.footerBar();
        view.bindToFooter(footer);

        Label backLabel = footerLabelById(footer, "back");
        Label forwardLabel = footerLabelById(footer, "forward");
        assertNotNull(backLabel);
        assertNotNull(forwardLabel);

        backLabel.fireEvent(syntheticClick(backLabel));
        assertEquals(0, view.currentIndex());

        forwardLabel.fireEvent(syntheticClick(forwardLabel));
        assertEquals(1, view.currentIndex());
    }

    @Test
    void leftClickOnViewAdvancesAndRightClickRewinds() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.addEntry("Line 3.");
        view.goBack();
        view.goBack();
        assertEquals(0, view.currentIndex());

        view.fireEvent(syntheticClick(view, MouseButton.PRIMARY));
        assertEquals(1, view.currentIndex());

        view.fireEvent(syntheticClick(view, MouseButton.PRIMARY));
        assertEquals(2, view.currentIndex());

        view.fireEvent(syntheticClick(view, MouseButton.SECONDARY));
        assertEquals(1, view.currentIndex());

        view.fireEvent(syntheticClick(view, MouseButton.SECONDARY));
        assertEquals(0, view.currentIndex());
    }

    @Test
    void leftClickAtNewestEntryDoesNotMoveCursor() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Only line.");

        view.fireEvent(syntheticClick(view, MouseButton.PRIMARY));

        assertEquals(0, view.currentIndex());
    }

    @Test
    void rightClickAtOldestEntryDoesNotMoveCursor() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.goBack();
        assertEquals(0, view.currentIndex());

        view.fireEvent(syntheticClick(view, MouseButton.SECONDARY));

        assertEquals(0, view.currentIndex());
    }

    @Test
    void bindToFooterAcceptsAncestorContainingFooter() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");

        HBox footer = ScreenShell.footerBar();
        BorderPane root = new BorderPane();
        root.setBottom(footer);

        view.bindToFooter(root);

        Label backLabel = footerLabelById(footer, "back");
        backLabel.fireEvent(syntheticClick(backLabel));

        assertEquals(0, view.currentIndex());
    }

    @Test
    void sayRendersTextFlowWithSpeakerAndPlainBody() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.say(alice, "Hello, Bob!");

        assertEquals(1, view.getChildren().size());
        TextFlow flow = assertInstanceOf(TextFlow.class, view.getChildren().get(0));
        assertTrue(flow.getStyleClass().contains(DialogEntriesView.SAY_STYLE_CLASS));
        assertEquals(2, flow.getChildren().size());
        Text speaker = (Text) flow.getChildren().get(0);
        Text body = (Text) flow.getChildren().get(1);
        assertEquals("Alice: ", speaker.getText());
        assertEquals("Hello, Bob!", body.getText());
    }

    @Test
    void shoutRendersBodyAsUppercase() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker bob = DialogSpeaker.text("bob", "Bob");

        view.shout(bob, "Stop!");

        TextFlow flow = (TextFlow) view.getChildren().get(0);
        assertTrue(flow.getStyleClass().contains(DialogEntriesView.SHOUT_STYLE_CLASS));
        Text body = (Text) flow.getChildren().get(1);
        assertEquals("STOP!", body.getText());
        assertTrue(body.getStyleClass().contains(DialogEntriesView.SHOUT_STYLE_CLASS));
    }

    @Test
    void whisperRendersBodyAsLowercase() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.whisper(alice, "Don't TELL anyone");

        TextFlow flow = (TextFlow) view.getChildren().get(0);
        assertTrue(flow.getStyleClass().contains(DialogEntriesView.WHISPER_STYLE_CLASS));
        Text body = (Text) flow.getChildren().get(1);
        assertEquals("don't tell anyone", body.getText());
    }

    @Test
    void sayWithoutSpeakerRendersBodyOnly() {
        DialogEntriesView view = new DialogEntriesView();

        view.say("The room is dark.");

        TextFlow flow = (TextFlow) view.getChildren().get(0);
        assertEquals(1, flow.getChildren().size());
        Text body = (Text) flow.getChildren().get(0);
        assertEquals("The room is dark.", body.getText());
    }

    @Test
    void spokenEntryExposesStructuredEntryThroughDialogEntries() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.shout(alice, "Stop!");

        List<DialogEntriesView.Entry> raw = view.dialogEntries();
        assertEquals(1, raw.size());
        DialogEntriesView.SpokenEntry spoken = assertInstanceOf(DialogEntriesView.SpokenEntry.class, raw.get(0));
        assertEquals(LineType.SHOUT, spoken.type());
        assertSame(alice, spoken.speaker());
        assertEquals("Stop!", spoken.text());
        assertEquals("STOP!", spoken.formattedBody());
        assertEquals("Alice: STOP!", spoken.displayText());
    }

    @Test
    void startConversationOpensDialogHistoryAndAppendsStartDivider() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");
        DialogSpeaker bob = DialogSpeaker.text("bob", "Bob");

        view.startConversation("first-chat", new GameDateTime(2, "morning"), alice, bob);

        assertEquals(1, view.dialogEntries().size());
        DialogEntriesView.ConversationStart start = assertInstanceOf(
                DialogEntriesView.ConversationStart.class, view.dialogEntries().get(0));
        assertEquals(List.of(alice, bob), start.participants());
        assertEquals(2, start.startedAt().day());
        assertEquals("morning", start.startedAt().timeSlotId());

        DialogHistoryEntry openEntry = view.history().openDialog().orElseThrow();
        assertEquals("first-chat", openEntry.dialogId());
    }

    @Test
    void sayDuringOpenConversationMirrorsIntoHistory() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.startConversation(alice);
        view.say(alice, "Hello.");
        view.shout(alice, "Listen!");
        view.whisper(alice, "Quiet now.");

        DialogHistoryEntry open = view.history().openDialog().orElseThrow();
        assertEquals(3, open.messages().size());
        // Body is stored already transformed (uppercase / lowercase) per line type.
        assertEquals("Hello.", open.messages().get(0).columns().get(1).tokens().get(0).text());
        assertEquals("LISTEN!", open.messages().get(1).columns().get(1).tokens().get(0).text());
        assertEquals("quiet now.", open.messages().get(2).columns().get(1).tokens().get(0).text());
    }

    @Test
    void sayWithoutOpenConversationStillRendersButSkipsHistory() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.say(alice, "No conversation open.");

        assertEquals(1, view.dialogEntries().size());
        assertTrue(view.history().openDialog().isEmpty());
        assertTrue(view.history().entries().isEmpty());
    }

    @Test
    void endConversationClosesHistoryAndAppendsEndDivider() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.startConversation(alice);
        view.say(alice, "Hi.");
        view.endConversation(new GameDateTime(1, "noon"));

        assertTrue(view.history().openDialog().isEmpty());
        List<DialogEntriesView.Entry> raw = view.dialogEntries();
        DialogEntriesView.ConversationEnd endDivider = assertInstanceOf(
                DialogEntriesView.ConversationEnd.class, raw.get(raw.size() - 1));
        assertEquals(1, endDivider.endedAt().day());
        assertEquals("noon", endDivider.endedAt().timeSlotId());
    }

    @Test
    void endConversationIsNoOpWhenNoConversationOpen() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Just a line.");

        view.endConversation();

        assertEquals(1, view.dialogEntries().size());
        assertTrue(view.history().entries().isEmpty());
    }

    @Test
    void conversationDividersRenderAsHBoxWithDividerStyleClass() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.startConversation(alice);

        HBox divider = assertInstanceOf(HBox.class, view.getChildren().get(0));
        assertTrue(divider.getStyleClass().contains(DialogEntriesView.DIVIDER_STYLE_CLASS));
    }

    @Test
    void widgetCanShareExternalDialogHistory() {
        DialogHistory shared = new DialogHistory();
        DialogEntriesView view = new DialogEntriesView(shared);

        assertSame(shared, view.history());
    }

    @Test
    void dialogEntriesReturnsAllEntryKindsIncludingDividers() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.startConversation(alice);
        view.say(alice, "Line 1.");
        view.endConversation();

        List<DialogEntriesView.Entry> raw = view.dialogEntries();
        assertEquals(3, raw.size());
        assertInstanceOf(DialogEntriesView.ConversationStart.class, raw.get(0));
        assertInstanceOf(DialogEntriesView.SpokenEntry.class, raw.get(1));
        assertInstanceOf(DialogEntriesView.ConversationEnd.class, raw.get(2));
    }

    private static Label footerLabelById(HBox footer, String id) {
        for (Node child : footer.getChildren()) {
            if (child instanceof Label label
                    && label.getUserData() instanceof ScreenShell.FooterOption option
                    && option.id().equals(id)) {
                return label;
            }
        }
        return null;
    }

    private static MouseEvent syntheticClick(Node target) {
        return syntheticClick(target, MouseButton.PRIMARY);
    }

    private static MouseEvent syntheticClick(Node target, MouseButton button) {
        return new MouseEvent(
                target,
                target,
                MouseEvent.MOUSE_CLICKED,
                0, 0, 0, 0,
                button,
                1,
                false, false, false, false,
                button == MouseButton.PRIMARY, false, button == MouseButton.SECONDARY, false, false, false,
                null);
    }
}
