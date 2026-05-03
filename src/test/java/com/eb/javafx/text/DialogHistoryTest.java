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
        DialogSpeaker speaker = DialogSpeaker.iconText("narrator", "Narrator", "icons/narrator", "#66c1e0");

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
                DialogColumn.parsed("left", "{color=#fff}Left{/color}"),
                DialogColumn.parsed("right", "{i}Right{/i}"))));
        DialogHistoryEntry ended = history.endDialog(new GameDateTime(1, "default"));

        assertFalse(ended.messages().get(0).hasSpeaker());
        assertEquals("left", ended.messages().get(0).columns().get(0).id());
        assertEquals("#fff", ended.messages().get(0).columns().get(0).tokens().get(0).style().color());
        assertTrue(ended.messages().get(0).columns().get(1).tokens().get(0).style().italic());
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
}
