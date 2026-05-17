package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.scene.ConversationDefinition.LineType;
import com.eb.javafx.text.DialogHistory;
import com.eb.javafx.text.DialogHistoryEntry;
import com.eb.javafx.text.DialogSpeaker;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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

        assertEquals(0, view.entryNodes().size());
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

        List<Node> children = view.entryNodes();
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
        assertEquals(2, view.entryNodes().size());
        Label previous = (Label) view.entryNodes().get(0);
        Label current = (Label) view.entryNodes().get(1);
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

        assertEquals(2, view.entryNodes().size());
        Label previous = (Label) view.entryNodes().get(0);
        Label current = (Label) view.entryNodes().get(1);
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
        assertEquals(0, view.entryNodes().size());
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
    void bindToFooterSyncsEnabledStateImmediatelyAndOnNavigation() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.addEntry("Line 3.");
        // Cursor starts on the last entry: back is possible, forward is not.

        HBox footer = ScreenShell.footerBar();
        view.bindToFooter(footer);

        Label backLabel = footerLabelById(footer, "back");
        Label forwardLabel = footerLabelById(footer, "forward");

        // Initial state: back enabled, forward disabled.
        assertTrue(ScreenShell.isFooterOptionEnabled(backLabel),
                "back label should be enabled when cursor is not at first entry");
        assertFalse(backLabel.getStyleClass().contains(ScreenShell.SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS));
        assertFalse(ScreenShell.isFooterOptionEnabled(forwardLabel),
                "forward label should be disabled when cursor is at last entry");
        assertTrue(forwardLabel.getStyleClass().contains(ScreenShell.SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS));

        // Navigate back to the first entry — back becomes disabled, forward becomes enabled.
        view.goBack();
        view.goBack();
        assertFalse(ScreenShell.isFooterOptionEnabled(backLabel),
                "back label should be disabled at first entry");
        assertTrue(backLabel.getStyleClass().contains(ScreenShell.SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS));
        assertTrue(ScreenShell.isFooterOptionEnabled(forwardLabel),
                "forward label should be enabled when not at last entry");
        assertFalse(forwardLabel.getStyleClass().contains(ScreenShell.SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS));
    }

    @Test
    void historyModeShowsAllEntriesIncludingThoseAfterCursor() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.addEntry("Line 3.");
        // Navigate back so cursor is at entry 1 (index 1); entry 2 (index 2) is after cursor.
        view.goBack();
        assertEquals(1, view.currentIndex());
        // Normal mode: only entries 0 and 1 visible (up to cursor).
        assertEquals(2, view.entryNodes().size());

        view.setHistoryMode(true);

        // History mode: all 3 entries visible.
        assertEquals(3, view.entryNodes().size());
        // Entry at currentIndex is "current"; others are "previous".
        assertTrue(view.entryNodes().get(1).getStyleClass().contains(DialogEntriesView.CURRENT_ENTRY_STYLE_CLASS));
        assertTrue(view.entryNodes().get(0).getStyleClass().contains(DialogEntriesView.PREVIOUS_ENTRY_STYLE_CLASS));
        assertTrue(view.entryNodes().get(2).getStyleClass().contains(DialogEntriesView.PREVIOUS_ENTRY_STYLE_CLASS));
    }

    @Test
    void historyModeOffRestoresNormalCursorBoundRendering() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.addEntry("Line 3.");
        view.goBack(); // cursor at index 1

        view.setHistoryMode(true);
        assertEquals(3, view.entryNodes().size());

        view.setHistoryMode(false);
        // Back to normal: only lines 0 and 1 rendered (cursor at 1).
        assertEquals(2, view.entryNodes().size());
    }

    @Test
    void bindHistoryToggleWiresHistoryButtonToToggleMode() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");

        HBox footer = ScreenShell.footerBar();
        // Dummy story node — no real layout in a headless test; toggleHistoryLayout skips the
        // layout branch when the parent is not a BorderPane, but the mode flag still flips.
        javafx.scene.layout.StackPane storyNode = new javafx.scene.layout.StackPane();
        view.bindHistoryToggle(footer, storyNode, 0.5);

        assertFalse(view.isHistoryMode());

        Label historyLabel = footerLabelById(footer, "history");
        assertNotNull(historyLabel, "history footer label should exist");
        historyLabel.fireEvent(syntheticClick(historyLabel));

        assertTrue(view.isHistoryMode(), "history mode should be active after first click");

        historyLabel.fireEvent(syntheticClick(historyLabel));

        assertFalse(view.isHistoryMode(), "history mode should be off after second click");
    }

    @Test
    void bindHistoryToggleHidesAndRestoresStoryNodeWhenEmbeddedInBorderPane() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");

        javafx.scene.layout.StackPane storyNode = new javafx.scene.layout.StackPane();
        // Set up the same parent structure that MainAppLayoutRenderer produces: a BorderPane
        // whose center is the story slot and whose bottom is the dialog view.
        BorderPane centre = new BorderPane();
        centre.setCenter(storyNode);
        centre.setBottom(view);

        HBox footer = ScreenShell.footerBar();
        view.bindHistoryToggle(footer, storyNode, 0.5);

        Label historyLabel = footerLabelById(footer, "history");

        // Story node starts visible and managed.
        assertTrue(storyNode.isManaged());
        assertTrue(storyNode.isVisible());

        historyLabel.fireEvent(syntheticClick(historyLabel));

        // In history mode the story slot collapses.
        assertFalse(storyNode.isManaged(), "story node must be unmanaged in history mode");
        assertFalse(storyNode.isVisible(), "story node must be hidden in history mode");
        assertTrue(view.isHistoryMode());

        historyLabel.fireEvent(syntheticClick(historyLabel));

        // On exit the story slot is fully restored.
        assertTrue(storyNode.isManaged(), "story node must be managed after history mode exits");
        assertTrue(storyNode.isVisible(), "story node must be visible after history mode exits");
        assertFalse(view.isHistoryMode());
        // The dialog must still be in the centre pane (not removed or lost).
        assertSame(centre, view.getParent(), "dialog must still be a child of the centre pane");
    }

    @Test
    void sayRendersHBoxWithSpeakerAndPlainBody() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.say(alice, "Hello, Bob!");

        assertEquals(1, view.entryNodes().size());
        HBox row = assertInstanceOf(HBox.class, view.entryNodes().get(0));
        assertTrue(row.getStyleClass().contains(DialogEntriesView.SAY_STYLE_CLASS));
        assertEquals(2, row.getChildren().size());
        Label speaker = (Label) row.getChildren().get(0);
        Label body = (Label) row.getChildren().get(1);
        assertEquals("Alice:", speaker.getText());
        assertTrue(speaker.getStyleClass().contains(DialogEntriesView.SPEAKER_STYLE_CLASS));
        assertEquals("Hello, Bob!", body.getText());
        assertTrue(body.getStyleClass().contains(DialogEntriesView.BODY_STYLE_CLASS));
        assertTrue(body.isWrapText());
    }

    @Test
    void shoutRendersBodyAsUppercase() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker bob = DialogSpeaker.text("bob", "Bob");

        view.shout(bob, "Stop!");

        HBox row = (HBox) view.entryNodes().get(0);
        assertTrue(row.getStyleClass().contains(DialogEntriesView.SHOUT_STYLE_CLASS));
        Label body = (Label) row.getChildren().get(1);
        assertEquals("STOP!", body.getText());
        assertTrue(body.getStyleClass().contains(DialogEntriesView.SHOUT_STYLE_CLASS));
    }

    @Test
    void whisperRendersBodyAsLowercase() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.whisper(alice, "Don't TELL anyone");

        HBox row = (HBox) view.entryNodes().get(0);
        assertTrue(row.getStyleClass().contains(DialogEntriesView.WHISPER_STYLE_CLASS));
        Label body = (Label) row.getChildren().get(1);
        assertEquals("don't tell anyone", body.getText());
    }

    @Test
    void sayWithoutSpeakerRendersBodyOnly() {
        DialogEntriesView view = new DialogEntriesView();

        view.say("The room is dark.");

        HBox row = (HBox) view.entryNodes().get(0);
        assertEquals(1, row.getChildren().size());
        Label body = (Label) row.getChildren().get(0);
        assertEquals("The room is dark.", body.getText());
    }

    @Test
    void speakerWithTextColorTintsBothSpeakerAndBody() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker mc = new DialogSpeaker("mc", "Hero", null, "#88ddff");

        view.say(mc, "Let's go.");

        HBox row = (HBox) view.entryNodes().get(0);
        Label speaker = (Label) row.getChildren().get(0);
        Label body = (Label) row.getChildren().get(1);
        assertTrue(speaker.getStyle().contains("#88ddff"),
                "speaker inline style should carry the role colour, was: " + speaker.getStyle());
        assertTrue(body.getStyle().contains("#88ddff"),
                "body inline style should carry the role colour, was: " + body.getStyle());
    }

    @Test
    void speakerWithoutTextColorLeavesInlineStyleEmpty() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker plain = DialogSpeaker.text("plain", "Plain");

        view.say(plain, "Hi.");

        HBox row = (HBox) view.entryNodes().get(0);
        Label speaker = (Label) row.getChildren().get(0);
        Label body = (Label) row.getChildren().get(1);
        assertEquals("", speaker.getStyle());
        assertEquals("", body.getStyle());
    }

    @Test
    void speakerColumnHasFixedWidthSoMessagesAlign() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker shortName = DialogSpeaker.text("a", "A");
        DialogSpeaker longName = DialogSpeaker.text("verbose", "VeryLongName");

        view.say(shortName, "first");
        view.say(longName, "second");

        Label firstSpeaker = (Label) ((HBox) view.entryNodes().get(0)).getChildren().get(0);
        Label secondSpeaker = (Label) ((HBox) view.entryNodes().get(1)).getChildren().get(0);
        assertEquals(firstSpeaker.getPrefWidth(), secondSpeaker.getPrefWidth(), 1e-9);
        assertEquals(firstSpeaker.getMinWidth(), secondSpeaker.getMinWidth(), 1e-9);
        // Speaker column is right-aligned so text hugs the body column for a cleaner read.
        assertEquals(Pos.TOP_RIGHT, firstSpeaker.getAlignment());
        assertEquals(Pos.TOP_RIGHT, secondSpeaker.getAlignment());
    }

    @Test
    void setSpeakerColumnWidthAppliesToExistingAndNewEntries() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");
        view.say(alice, "first");
        // Default speaker column width is 160px and is applied to the existing row.
        Label firstSpeaker = (Label) ((HBox) view.entryNodes().get(0)).getChildren().get(0);
        assertEquals(DialogEntriesView.DEFAULT_SPEAKER_COLUMN_WIDTH, firstSpeaker.getPrefWidth(), 1e-9);

        view.setSpeakerColumnWidth(240);

        // After the setter both the existing rebuilt row and a newly-added row pick up the new width.
        firstSpeaker = (Label) ((HBox) view.entryNodes().get(0)).getChildren().get(0);
        assertEquals(240.0, firstSpeaker.getPrefWidth(), 1e-9);
        assertEquals(240.0, firstSpeaker.getMinWidth(), 1e-9);
        assertEquals(240.0, firstSpeaker.getMaxWidth(), 1e-9);
        view.say(alice, "second");
        Label secondSpeaker = (Label) ((HBox) view.entryNodes().get(1)).getChildren().get(0);
        assertEquals(240.0, secondSpeaker.getPrefWidth(), 1e-9);
        assertEquals(240.0, view.speakerColumnWidth(), 1e-9);
    }

    @Test
    void setSpeakerColumnWidthRejectsNonPositive() {
        DialogEntriesView view = new DialogEntriesView();
        assertThrows(IllegalArgumentException.class, () -> view.setSpeakerColumnWidth(0));
        assertThrows(IllegalArgumentException.class, () -> view.setSpeakerColumnWidth(-10));
    }

    @Test
    void installKeyboardShortcutsIsIdempotent() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");

        javafx.scene.layout.StackPane host = new javafx.scene.layout.StackPane(view);
        javafx.scene.Scene scene = new javafx.scene.Scene(host);

        // Adding the view to a Scene auto-installs the shortcut filter once. Calling the public
        // helper again must not double-install, otherwise Space would advance twice per press.
        view.installKeyboardShortcuts(scene);
        view.installKeyboardShortcuts(scene);
        assertEquals(1, view.currentIndex());

        javafx.event.Event.fireEvent(scene, new javafx.scene.input.KeyEvent(
                javafx.scene.input.KeyEvent.KEY_PRESSED, "", "", javafx.scene.input.KeyCode.BACK_SPACE,
                false, false, false, false));

        // Exactly one back-step.
        assertEquals(0, view.currentIndex());
    }

    @Test
    void bindToFooterIsIdempotent() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.addEntry("Line 3.");

        HBox footer = ScreenShell.footerBar();
        // Wire the same footer twice — must not stack listeners.
        view.bindToFooter(footer);
        view.bindToFooter(footer);

        Label backLabel = footerLabelById(footer, "back");
        backLabel.fireEvent(syntheticClick(backLabel));

        // A single click goes back exactly one entry, not two.
        assertEquals(1, view.currentIndex());
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
    void endConversationClosesHistoryWithoutAppendingEndDivider() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.startConversation(alice);
        view.say(alice, "Hi.");
        int sizeBeforeEnd = view.dialogEntries().size();

        view.endConversation(new GameDateTime(1, "noon"));

        // History still closes — only the visible ConversationEnd divider is suppressed.
        assertTrue(view.history().openDialog().isEmpty());
        assertEquals(sizeBeforeEnd, view.dialogEntries().size(),
                "endConversation should NOT append a ConversationEnd visible entry — only the "
                        + "start divider is kept as the visual separator between conversations.");
        assertTrue(view.dialogEntries().stream()
                        .noneMatch(e -> e instanceof DialogEntriesView.ConversationEnd),
                "ConversationEnd entries should never be appended by endConversation.");
        // The cursor still sits on the last spoken line, not on a vanished divider.
        assertTrue(view.dialogEntries().get(view.currentIndex())
                        instanceof DialogEntriesView.SpokenEntry,
                "Cursor should remain on the last spoken entry after endConversation.");
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

        HBox divider = assertInstanceOf(HBox.class, view.entryNodes().get(0));
        assertTrue(divider.getStyleClass().contains(DialogEntriesView.DIVIDER_STYLE_CLASS));
    }

    @Test
    void widgetCanShareExternalDialogHistory() {
        DialogHistory shared = new DialogHistory();
        DialogEntriesView view = new DialogEntriesView(shared);

        assertSame(shared, view.history());
    }

    @Test
    void viewIsAScrollPaneWithRightHandVerticalScrollbar() {
        DialogEntriesView view = new DialogEntriesView();

        assertTrue(view instanceof ScrollPane,
                "DialogEntriesView should extend ScrollPane to provide the scrollbar.");
        assertEquals(ScrollPane.ScrollBarPolicy.AS_NEEDED, view.getVbarPolicy(),
                "Vertical scrollbar should appear as needed so previous conversations can be scrolled.");
        assertEquals(ScrollPane.ScrollBarPolicy.NEVER, view.getHbarPolicy(),
                "Horizontal scrollbar should never appear — long messages wrap inside the dialog block.");
        assertTrue(view.isFitToWidth(),
                "Content should fit the viewport width so message bodies wrap to the dialog width.");
    }

    @Test
    void unboundedDefaultLetsAllEntriesRenderIntoTheScrollableContainer() {
        DialogEntriesView view = new DialogEntriesView();
        for (int i = 0; i < 50; i++) {
            view.addEntry("Line " + i);
        }

        assertEquals(50, view.entryNodes().size(),
                "Default render should include every entry; the ScrollPane provides the viewport.");
    }

    @Test
    void rebuildScrollsViewportToBottomToKeepCurrentEntryVisible() throws Exception {
        DialogEntriesView view = new DialogEntriesView();
        for (int i = 0; i < 10; i++) {
            view.addEntry("Line " + i);
        }

        // Scroll to the top, then add a new entry — rebuild() should snap back to the bottom so
        // the newest line is visible without manual scrolling.
        view.setVvalue(view.getVmin());
        view.addEntry("Newest line");

        // The scroll is deferred via a NESTED Platform.runLater so the first tick lets the JavaFX
        // layout pass commit the new content height before the second tick reads getVmax() and
        // pins the vvalue. Flushing once isn't enough — drain two pulses by chaining a sentinel
        // through another runLater so it lands on the tick after the nested scroll fires.
        CountDownLatch fxFlushed = new CountDownLatch(1);
        Platform.runLater(() -> Platform.runLater(fxFlushed::countDown));
        fxFlushed.await(5, TimeUnit.SECONDS);

        assertEquals(view.getVmax(), view.getVvalue(), 1e-9);
    }

    @Test
    void dialogEntriesReturnsStartDividerAndSpokenButNoEndDivider() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.startConversation(alice);
        view.say(alice, "Line 1.");
        view.endConversation();

        List<DialogEntriesView.Entry> raw = view.dialogEntries();
        assertEquals(2, raw.size(),
                "After the no-end-divider change, only the start divider + spoken entry should remain.");
        assertInstanceOf(DialogEntriesView.ConversationStart.class, raw.get(0));
        assertInstanceOf(DialogEntriesView.SpokenEntry.class, raw.get(1));
    }

    @Test
    void goBackSkipsConversationStartDividerToLandOnPreviousSpokenEntry() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.addEntry("Line A");
        view.startConversation(alice);
        view.say(alice, "Line B");
        // entries = [PlainEntry("Line A"), ConversationStart, SpokenEntry("Line B")]
        // currentIndex = 2 (on Line B).
        assertEquals(2, view.currentIndex());

        view.goBack();

        // Should jump straight over the ConversationStart divider to Line A.
        assertEquals(0, view.currentIndex(),
                "goBack should auto-skip the ConversationStart divider and land on Line A.");
        assertInstanceOf(DialogEntriesView.PlainEntry.class,
                view.dialogEntries().get(view.currentIndex()));
    }

    @Test
    void goForwardSkipsConversationStartDividerToLandOnNextSpokenEntry() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.addEntry("Line A");
        view.startConversation(alice);
        view.say(alice, "Line B");
        view.goBack();
        assertEquals(0, view.currentIndex(), "Sanity: cursor is on Line A after goBack.");

        view.goForward();

        // Should skip the ConversationStart at index 1 and land on the SpokenEntry at index 2.
        assertEquals(2, view.currentIndex(),
                "goForward should auto-skip the ConversationStart divider and land on Line B.");
        assertInstanceOf(DialogEntriesView.SpokenEntry.class,
                view.dialogEntries().get(view.currentIndex()));
    }

    @Test
    void disablingInternalNavigationMakesLeftRightClicksNoOps() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.addEntry("Line 3.");
        view.goBack();
        view.goBack();
        assertEquals(0, view.currentIndex());

        view.setInternalNavigationEnabled(false);

        view.fireEvent(syntheticClick(view, MouseButton.PRIMARY));
        view.fireEvent(syntheticClick(view, MouseButton.SECONDARY));

        assertEquals(0, view.currentIndex(),
                "Internal-navigation flag should suppress the built-in cursor walks on clicks.");
    }

    @Test
    void reenablingInternalNavigationRestoresClickHandling() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.goBack();
        assertEquals(0, view.currentIndex());

        view.setInternalNavigationEnabled(false);
        view.fireEvent(syntheticClick(view, MouseButton.PRIMARY));
        assertEquals(0, view.currentIndex(), "Sanity: click is ignored while disabled.");

        view.setInternalNavigationEnabled(true);
        view.fireEvent(syntheticClick(view, MouseButton.PRIMARY));

        assertEquals(1, view.currentIndex(),
                "Re-enabling internal navigation should resume cursor walks on click.");
    }

    @Test
    void historyClipsAtCursorRendersOnlyEntriesUpToAndIncludingCursor() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.addEntry("Line 3.");
        view.goBack(); // cursor at index 1

        view.setHistoryClipsAtCursor(true);
        view.setHistoryMode(true);

        assertEquals(2, view.entryNodes().size(),
                "Clipped history mode should render only entries [0..currentIndex].");
        assertTrue(view.entryNodes().get(1).getStyleClass().contains(DialogEntriesView.CURRENT_ENTRY_STYLE_CLASS));
        assertTrue(view.entryNodes().get(0).getStyleClass().contains(DialogEntriesView.PREVIOUS_ENTRY_STYLE_CLASS));
    }

    @Test
    void historyClipsAtCursorFlagTakesEffectMidHistoryMode() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");
        view.addEntry("Line 2.");
        view.addEntry("Line 3.");
        view.goBack(); // cursor at index 1

        view.setHistoryMode(true);
        assertEquals(3, view.entryNodes().size(), "Sanity: unclipped history renders all entries.");

        view.setHistoryClipsAtCursor(true);

        assertEquals(2, view.entryNodes().size(),
                "Flipping the clip flag while in history mode must rebuild and drop entries past the cursor.");
    }

    @Test
    void historyModeHeightBindsToCentreHeightNotContentHeight() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Just one short line.");

        javafx.scene.layout.StackPane storyNode = new javafx.scene.layout.StackPane();
        BorderPane centre = new BorderPane();
        centre.setCenter(storyNode);
        centre.setBottom(view);
        centre.resize(800, 600);
        centre.layout();

        HBox footer = ScreenShell.footerBar();
        view.bindHistoryToggle(footer, storyNode, 0.5);

        Label historyLabel = footerLabelById(footer, "history");
        historyLabel.fireEvent(syntheticClick(historyLabel));

        // In history mode the dialog must claim the full centre height — not collapse to the
        // content's tiny natural height. Binding to content.height previously created a feedback
        // loop that oscillated the dialog between two sizes; we now pin to centre.height.
        assertEquals(centre.getHeight(), view.getPrefHeight(), 0.001,
                "History mode should pin the dialog pref height to the centre height.");
        assertEquals(centre.getHeight(), view.getMinHeight(), 0.001);
        assertEquals(centre.getHeight(), view.getMaxHeight(), 0.001);
    }

    @Test
    void enableAutoFitDialogHeightClampsBetweenNormalAndMaxShare() {
        DialogEntriesView view = new DialogEntriesView();
        // Build the same parent shape MainAppLayoutRenderer produces: dialog in BorderPane.bottom.
        javafx.scene.layout.StackPane storyNode = new javafx.scene.layout.StackPane();
        BorderPane centre = new BorderPane();
        centre.setCenter(storyNode);
        centre.setBottom(view);
        centre.resize(800, 1000);
        centre.layout();

        view.enableAutoFitDialogHeight(centre, 0.20, 0.60);

        // Add one entry then drive the CURRENT-ENTRY node's size (not the container's). The new
        // binding tracks just the cursor entry's height so the dialog grows only as much as the
        // active line needs — earlier the binding watched entriesContainer.height which summed
        // every previous-entry fade above the cursor and over-expanded by ~2×.
        view.addEntry("Short");
        javafx.scene.layout.VBox container = (javafx.scene.layout.VBox) view.getContent();
        javafx.scene.Node currentEntryNode = container.getChildren().get(container.getChildren().size() - 1);

        // Tiny current entry → dialog clamps at the resting share (centre × 0.20 = 200).
        currentEntryNode.resize(800, 50);
        assertEquals(200.0, view.getPrefHeight(), 0.001,
                "Tiny current entry should leave the dialog at the resting share.");

        // Mid-range current entry → dialog tracks the entry's height directly (chrome ~= 0 in a
        // stylesheet-less test where no CSS padding is applied).
        currentEntryNode.resize(800, 350);
        assertEquals(350.0, view.getPrefHeight(), 0.001,
                "Mid-range current entry should drive the dialog height.");

        // Push past the cap (centre × 0.60 = 600); dialog clamps at the cap.
        currentEntryNode.resize(800, 900);
        assertEquals(600.0, view.getPrefHeight(), 0.001,
                "Current entry past the max share should clamp the dialog at the cap.");
    }

    @Test
    void enableAutoFitDialogHeightIgnoresHeightOfPreviousEntriesAboveTheCursor() {
        DialogEntriesView view = new DialogEntriesView();
        javafx.scene.layout.StackPane storyNode = new javafx.scene.layout.StackPane();
        BorderPane centre = new BorderPane();
        centre.setCenter(storyNode);
        centre.setBottom(view);
        centre.resize(800, 1000);
        centre.layout();

        view.enableAutoFitDialogHeight(centre, 0.20, 0.60);

        // Two previous entries plus a current one. Resize the previous entries to be tall; the
        // dialog must IGNORE them and size only against the current entry. This is the regression
        // the original (entriesContainer-based) binding caused — summing previous + current made
        // the dialog grow to almost twice what the active line needed.
        view.addEntry("Previous 1");
        view.addEntry("Previous 2");
        view.addEntry("Current");
        javafx.scene.layout.VBox container = (javafx.scene.layout.VBox) view.getContent();
        javafx.scene.Node prev1 = container.getChildren().get(0);
        javafx.scene.Node prev2 = container.getChildren().get(1);
        javafx.scene.Node current = container.getChildren().get(2);
        prev1.resize(800, 300);
        prev2.resize(800, 300);
        current.resize(800, 80);

        // Current entry is tiny (80) — under the resting share (200), so the dialog stays at base.
        // If the binding were still summing the whole container we'd see something around 680.
        assertEquals(200.0, view.getPrefHeight(), 0.001,
                "Tall previous entries above the cursor must NOT inflate the dialog height.");
    }

    @Test
    void enableAutoFitDialogHeightSurvivesHistoryModeRoundTrip() {
        DialogEntriesView view = new DialogEntriesView();
        view.addEntry("Line 1.");

        javafx.scene.layout.StackPane storyNode = new javafx.scene.layout.StackPane();
        BorderPane centre = new BorderPane();
        centre.setCenter(storyNode);
        centre.setBottom(view);
        centre.resize(800, 1000);

        view.enableAutoFitDialogHeight(centre, 0.20, 0.60);
        HBox footer = ScreenShell.footerBar();
        view.bindHistoryToggle(footer, storyNode, 0.20);

        // Auto-fit binding is active before history mode.
        double normalPref = view.getPrefHeight();

        Label historyLabel = footerLabelById(footer, "history");
        historyLabel.fireEvent(syntheticClick(historyLabel));

        // History mode takes over: dialog pinned to full centre height.
        assertEquals(centre.getHeight(), view.getPrefHeight(), 0.001,
                "History mode should still pin to full centre height.");

        historyLabel.fireEvent(syntheticClick(historyLabel));

        // Auto-fit binding restored on history exit — NOT the plain proportional fallback.
        // The two would only differ if content changed, but the binding identity must match.
        assertEquals(normalPref, view.getPrefHeight(), 0.001,
                "Leaving history must restore the auto-fit binding, not the proportional fallback.");
    }

    @Test
    void enableAutoFitDialogHeightRejectsInvalidShares() {
        DialogEntriesView view = new DialogEntriesView();
        javafx.scene.layout.StackPane storyNode = new javafx.scene.layout.StackPane();
        BorderPane centre = new BorderPane();
        centre.setCenter(storyNode);
        centre.setBottom(view);

        assertThrows(IllegalArgumentException.class,
                () -> view.enableAutoFitDialogHeight(centre, 0.60, 0.20),
                "maxShare smaller than normalShare must be rejected.");
        assertThrows(IllegalArgumentException.class,
                () -> view.enableAutoFitDialogHeight(centre, -0.1, 0.5),
                "Negative normalShare must be rejected.");
        assertThrows(IllegalArgumentException.class,
                () -> view.enableAutoFitDialogHeight(centre, 0.2, 1.5),
                "maxShare above 1.0 must be rejected.");
    }

    @Test
    void canGoBackAndCanGoForwardIgnoreLeadingTrailingDividers() {
        DialogEntriesView view = new DialogEntriesView();
        DialogSpeaker alice = DialogSpeaker.text("alice", "Alice");

        view.startConversation(alice);
        view.say(alice, "Only line.");
        // entries = [ConversationStart, SpokenEntry]; currentIndex = 1.
        // Nothing non-divider before the cursor → canGoBack should be false even though raw index > 0.
        assertFalse(view.canGoBack(),
                "Only entry before the cursor is a divider — canGoBack should report false.");
        assertFalse(view.canGoForward());
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
