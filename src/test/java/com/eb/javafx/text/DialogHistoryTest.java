package com.eb.javafx.text;

import com.eb.javafx.gamesupport.GameDateTime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DialogHistoryTest {
    @Test
    void startsAddsFormattedSpeakerMessageAndEndsDialog() {
        DialogHistory history = new DialogHistory();
        GameDateTime startedAt = new GameDateTime(2, "morning");
        GameDateTime endedAt = new GameDateTime(2, "afternoon");
        DialogSpeaker speaker = DialogSpeaker.iconText("narrator", "Narrator", "icons/narrator", "#66c1e0", "Serif");

        history.beginDialog("intro", startedAt);
        DialogHistoryEntry withMessage = history.addMessage(speaker, "Hello {b}traveler{/b}.");
        DialogHistoryEntry ended = history.endDialog(endedAt);

        assertEquals(1, history.entries().size());
        assertEquals("intro", ended.dialogId());
        assertSame(startedAt, ended.startedAt());
        assertSame(endedAt, ended.endedAt());
        assertFalse(ended.isOpen());
        assertFalse(history.openDialog().isPresent());
        assertEquals(1, ended.messages().size());
        assertEquals(1, withMessage.messages().size());

        DialogMessage message = ended.messages().get(0);
        assertSame(speaker, message.speaker());
        assertEquals(2, message.columns().size());
        assertEquals(DialogColumn.SPEAKER_COLUMN, message.columns().get(0).id());
        assertEquals("Narrator", message.columns().get(0).tokens().get(0).text());
        assertEquals("#66c1e0", message.columns().get(0).tokens().get(0).style().color());
        assertEquals("Serif", message.columns().get(0).tokens().get(0).style().fontFamily());
        assertEquals("icons/narrator", message.columns().get(0).tokens().get(0).style().effects().get("icon"));
        assertEquals(DialogColumn.MESSAGE_COLUMN, message.columns().get(1).id());
        assertEquals("traveler", message.columns().get(1).tokens().get(1).text());
        assertTrue(message.columns().get(1).tokens().get(1).style().bold());
    }

    @Test
    void supportsCustomFormattedColumns() {
        DialogHistory history = new DialogHistory();

        history.beginDialog("columns", new GameDateTime(1, "default"));
        history.addMessage(DialogMessage.columns(List.of(
                DialogColumn.parsed("left", "{color=#fff}{font=Monospace}Left{/font}{/color}"),
                DialogColumn.parsed("right", "{i}Right{/i}"))));
        DialogHistoryEntry ended = history.endDialog(new GameDateTime(1, "default"));

        assertFalse(ended.messages().get(0).hasSpeaker());
        assertEquals("left", ended.messages().get(0).columns().get(0).id());
        assertEquals("#fff", ended.messages().get(0).columns().get(0).tokens().get(0).style().color());
        assertEquals("Monospace", ended.messages().get(0).columns().get(0).tokens().get(0).style().fontFamily());
        assertTrue(ended.messages().get(0).columns().get(1).tokens().get(0).style().italic());
    }

    @Test
    void removesLastMessageFromOpenDialog() {
        DialogHistory history = new DialogHistory();
        DialogSpeaker speaker = DialogSpeaker.text("speaker", "Speaker");

        history.beginDialog("rewind", new GameDateTime(1, "default"));
        history.addMessage(speaker, "First");
        history.addMessage(speaker, "Second");

        assertTrue(history.removeLastMessage().isPresent());

        DialogHistoryEntry entry = history.openDialog().orElseThrow();
        assertEquals(1, entry.messages().size());
        assertEquals("First", entry.messages().get(0).columns().get(1).tokens().get(0).text());
    }

    @Test
    void removingLastMessageFromEmptyOpenDialogDoesNothing() {
        DialogHistory history = new DialogHistory();

        history.beginDialog("empty", new GameDateTime(1, "default"));

        assertFalse(history.removeLastMessage().isPresent());
        assertTrue(history.openDialog().orElseThrow().messages().isEmpty());
    }

    @Test
    void exposesImmutableHistoryCollections() {
        DialogHistory history = new DialogHistory();
        history.beginDialog("immutable", new GameDateTime(1, "default"));
        history.addMessage(DialogMessage.columns(List.of(DialogColumn.message("Read-only"))));
        DialogHistoryEntry ended = history.endDialog(new GameDateTime(1, "default"));

        assertThrows(UnsupportedOperationException.class, () -> history.entries().clear());
        assertThrows(UnsupportedOperationException.class, () -> ended.messages().clear());
        assertThrows(UnsupportedOperationException.class, () -> ended.messages().get(0).columns().clear());
        assertThrows(UnsupportedOperationException.class, () -> ended.messages().get(0).columns().get(0).tokens().clear());
    }

    @Test
    void validatesDialogLifecycle() {
        DialogHistory history = new DialogHistory();
        DialogSpeaker speaker = DialogSpeaker.text("speaker", "Speaker");

        assertThrows(IllegalStateException.class, () -> history.addMessage(speaker, "No dialog"));
        assertThrows(IllegalStateException.class, () -> history.endDialog(new GameDateTime(1, "default")));

        history.beginDialog("active", new GameDateTime(1, "default"));

        assertThrows(IllegalStateException.class, () -> history.beginDialog("nested", new GameDateTime(1, "default")));
        assertThrows(IllegalArgumentException.class, () -> history.addMessage(speaker, " "));

        history.addMessage(speaker, "Done");
        history.endDialog(new GameDateTime(1, "default"));

        assertThrows(IllegalStateException.class, () -> history.endDialog(new GameDateTime(1, "default")));
        assertThrows(IllegalArgumentException.class, () -> DialogSpeaker.text("", "Speaker"));
        assertThrows(IllegalArgumentException.class, () -> new DialogColumn("", List.of(TextToken.text("x", TextStyle.plain()))));
        assertThrows(IllegalArgumentException.class, () -> DialogMessage.columns(List.of()));
    }

    @Test
    void beginDialogTrimsOldestConversationWhenOverMaxLimit() {
        DialogHistory history = new DialogHistory();
        GameDateTime ts = new GameDateTime(1, "default");

        // Seed exactly MAX_CONVERSATIONS entries.
        for (int i = 0; i < DialogHistory.DEFAULT_MAX_CONVERSATIONS; i++) {
            history.beginDialog("dialog-" + i, ts);
            history.endDialog(ts);
        }
        assertEquals(DialogHistory.DEFAULT_MAX_CONVERSATIONS, history.entries().size());
        assertEquals("dialog-0", history.entries().get(0).dialogId());

        // One more conversation should drop the oldest.
        history.beginDialog("dialog-overflow", ts);
        history.endDialog(ts);

        assertEquals(DialogHistory.DEFAULT_MAX_CONVERSATIONS, history.entries().size());
        // "dialog-0" should have been evicted; "dialog-1" is now the oldest.
        assertEquals("dialog-1", history.entries().get(0).dialogId());
        assertEquals("dialog-overflow", history.entries().get(DialogHistory.DEFAULT_MAX_CONVERSATIONS - 1).dialogId());
        assertFalse(history.openDialog().isPresent());
    }

    @Test
    void beginDialogCanContinueAfterTrimWithoutCorruptingIndex() {
        DialogHistory history = new DialogHistory();
        GameDateTime ts = new GameDateTime(1, "default");

        // Fill to max + 5, exercising five trims.
        for (int i = 0; i < DialogHistory.DEFAULT_MAX_CONVERSATIONS + 5; i++) {
            history.beginDialog("d-" + i, ts);
            history.endDialog(ts);
        }

        // The open/close cycle after trimming must still work normally.
        history.beginDialog("final", ts);
        history.addMessage(DialogSpeaker.text("s", "Speaker"), "last line");
        DialogHistoryEntry ended = history.endDialog(ts);

        assertEquals(DialogHistory.DEFAULT_MAX_CONVERSATIONS, history.entries().size());
        assertEquals("final", ended.dialogId());
        assertEquals(1, ended.messages().size());
    }

    @Test
    void configuredCapTrimsToTheConfiguredSlidingWindow() {
        DialogHistory.setMaxConversations(3);
        try {
            assertEquals(3, DialogHistory.maxConversations());
            DialogHistory history = new DialogHistory();
            GameDateTime ts = new GameDateTime(1, "default");
            for (int i = 0; i < 6; i++) {
                history.beginDialog("d-" + i, ts);
                history.endDialog(ts);
            }
            assertEquals(3, history.entries().size());
            assertEquals("d-3", history.entries().get(0).dialogId());
            assertEquals("d-5", history.entries().get(2).dialogId());

            // Null / non-positive overrides are ignored (keep current value).
            DialogHistory.setMaxConversations(null);
            DialogHistory.setMaxConversations(0);
            DialogHistory.setMaxConversations(-5);
            assertEquals(3, DialogHistory.maxConversations());
        } finally {
            DialogHistory.setMaxConversations(DialogHistory.DEFAULT_MAX_CONVERSATIONS);
        }
    }
}
