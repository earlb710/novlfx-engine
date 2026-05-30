package com.eb.javafx.text;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.gamesupport.TimeSaveSnapshots;
import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON codec for {@link DialogHistory} — used by save/load to round-trip the
 * conversation-history accumulator across sessions.
 *
 * <h2>Encoded shape</h2>
 *
 * <pre>{@code
 * {
 *   "entries": [
 *     {
 *       "dialogId": "altlife.introduce.npc-001",
 *       "startedAt": { "day": 1, "timeSlotId": "morning" },
 *       "endedAt":   { "day": 1, "timeSlotId": "morning" } | null,
 *       "messages": [
 *         { "speakerLabel": "Marsh", "text": "How do you feel?",
 *           "speakerId": "marsh", "speakerColor": "#a0c0ff" | null },
 *         ...
 *       ]
 *     },
 *     ...
 *   ],
 *   "openEntryIndex": -1
 * }
 * }</pre>
 *
 * <h2>Fidelity</h2>
 *
 * <p>The full {@link DialogMessage} model carries {@link DialogColumn} values with
 * parsed {@link TextToken} streams (bold/italic/colour spans, icon effects, etc.).
 * The save format collapses each message to the simpler speaker-label + plain-text
 * shape because:</p>
 *
 * <ul>
 *   <li>The conversation-history screen renders speaker-label + body — formatting spans
 *       inside the body are not surfaced today, so dropping them is invisible.</li>
 *   <li>Reconstructing styled tokens from a save would tie the save schema to the text
 *       parser's grammar — every parser change would require a migration.  Keeping the
 *       saved form to plain text makes the save format independent of parser internals.</li>
 *   <li>Speaker styling (colour + label) IS preserved so the restored entries still
 *       render in the right speaker colour.</li>
 * </ul>
 *
 * <p>The decoder rebuilds each message via {@link DialogMessage#speakerMessage}, which
 * produces a two-column (speaker + message) shape — the same shape the
 * {@link DialogHistory#addMessage(DialogSpeaker, String) addMessage(speaker, text)}
 * convenience adds at runtime, so the restored history is structurally identical to a
 * history accumulated through that path.</p>
 */
public final class DialogHistorySnapshotJson {

    private DialogHistorySnapshotJson() {
    }

    public static String toJson(DialogHistory history) {
        Validation.requireNonNull(history, "Dialog history is required.");
        StringBuilder json = new StringBuilder("{\"entries\":[");
        List<DialogHistoryEntry> entries = history.entries();
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append(entryToJson(entries.get(index)));
        }
        json.append("],\"openEntryIndex\":").append(history.openEntryIndex()).append('}');
        return json.toString();
    }

    /** Mutates {@code history} so its state matches the supplied JSON snapshot.  Replaces
     *  any existing entries; the history is left exactly as the snapshot described. */
    public static void restore(DialogHistory history, String json, String sourceName) {
        Validation.requireNonNull(history, "Dialog history is required.");
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<Object> rawEntries = JsonData.optionalList(root, "entries", "dialog history entries");
        List<DialogHistoryEntry> restored = new ArrayList<>(rawEntries.size());
        for (Object raw : rawEntries) {
            restored.add(entryFromJson(JsonData.requireObject(raw, "dialog history entry"), sourceName));
        }
        int openIndex = root.containsKey("openEntryIndex")
                ? ((Number) root.get("openEntryIndex")).intValue()
                : -1;
        history.restoreEntries(restored, openIndex);
    }

    // ---- entry encode/decode ----------------------------------------------------

    private static String entryToJson(DialogHistoryEntry entry) {
        StringBuilder json = new StringBuilder("{\"dialogId\":").append(JsonStrings.quote(entry.dialogId()));
        json.append(",\"startedAt\":").append(TimeSaveSnapshots.toJson(entry.startedAt()));
        json.append(",\"endedAt\":");
        if (entry.endedAt() == null) {
            json.append("null");
        } else {
            json.append(TimeSaveSnapshots.toJson(entry.endedAt()));
        }
        json.append(",\"messages\":[");
        List<DialogMessage> messages = entry.messages();
        for (int index = 0; index < messages.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append(messageToJson(messages.get(index)));
        }
        json.append("]}");
        return json.toString();
    }

    private static DialogHistoryEntry entryFromJson(Map<String, Object> object, String sourceName) {
        String dialogId = JsonData.requiredString(object, "dialogId", "dialog history entry id");
        GameDateTime startedAt = timeFromObject(
                JsonData.requireObject(object.get("startedAt"), "dialog history started-at"));
        Object rawEndedAt = object.get("endedAt");
        GameDateTime endedAt = rawEndedAt == null
                ? null
                : timeFromObject(JsonData.requireObject(rawEndedAt, "dialog history ended-at"));
        List<Object> rawMessages = JsonData.optionalList(object, "messages", "dialog history messages");
        List<DialogMessage> messages = new ArrayList<>(rawMessages.size());
        for (Object raw : rawMessages) {
            messages.add(messageFromJson(JsonData.requireObject(raw, "dialog history message")));
        }
        return new DialogHistoryEntry(dialogId, startedAt, endedAt, messages);
    }

    // ---- message encode/decode --------------------------------------------------

    private static String messageToJson(DialogMessage message) {
        StringBuilder json = new StringBuilder("{");
        DialogSpeaker speaker = message.speaker();
        String label = speaker == null ? "" : speaker.label();
        json.append("\"speakerLabel\":").append(JsonStrings.quote(label == null ? "" : label));
        json.append(",\"text\":").append(JsonStrings.quote(plainTextOfMessage(message)));
        if (speaker != null) {
            json.append(",\"speakerId\":").append(JsonStrings.quote(speaker.id() == null ? "" : speaker.id()));
            if (speaker.hasTextColor()) {
                json.append(",\"speakerColor\":").append(JsonStrings.quote(speaker.textColor()));
            }
            if (speaker.hasIcon()) {
                json.append(",\"speakerIconId\":").append(JsonStrings.quote(speaker.iconId()));
            }
        }
        json.append('}');
        return json.toString();
    }

    private static DialogMessage messageFromJson(Map<String, Object> object) {
        String text = JsonData.optionalString(object, "text", "dialog history message text").orElse("");
        String label = JsonData.optionalString(object, "speakerLabel", "dialog history speaker label").orElse("");
        // When a speakerId is present we rebuild a full DialogSpeaker so the renderer
        // can apply the saved colour / icon.  When it isn't (rare — older saves or
        // hosts that wrote pure narration), build a label-only speaker which still
        // gives the speaker column something to render.
        String speakerId = JsonData.optionalString(object, "speakerId", "dialog history speaker id").orElse("");
        if (speakerId.isBlank() && label.isBlank()) {
            // Pure narration message — DialogMessage.speakerMessage requires a non-null
            // speaker, so synthesise a placeholder label-only speaker.  Lossy but
            // sufficient for the history view.
            DialogSpeaker placeholder = DialogSpeaker.text("narrator", "");
            return DialogMessage.speakerMessage(placeholder, text.isBlank() ? " " : text);
        }
        String color = JsonData.optionalString(object, "speakerColor", "dialog history speaker color").orElse("");
        String iconId = JsonData.optionalString(object, "speakerIconId", "dialog history speaker icon").orElse("");
        DialogSpeaker speaker = DialogSpeaker.iconText(
                speakerId.isBlank() ? "saved" : speakerId,
                label.isBlank() ? speakerId : label,
                iconId.isBlank() ? null : iconId,
                color.isBlank() ? null : color);
        return DialogMessage.speakerMessage(speaker, text.isBlank() ? " " : text);
    }

    /** Flattens a {@link DialogMessage}'s columns into a single plain-text body string.
     *  Walks every column (in declaration order — typically [speaker, message] for
     *  speaker-tagged messages, or a single message column for narration), concatenating
     *  every {@link TextToken} the column carries.  Strips speaker labels from the
     *  body output since {@link #messageToJson} captures the speaker label separately;
     *  including it inline would double-write the label on restore. */
    private static String plainTextOfMessage(DialogMessage message) {
        StringBuilder body = new StringBuilder();
        for (DialogColumn column : message.columns()) {
            if (DialogColumn.SPEAKER_COLUMN.equals(column.id())) {
                continue;
            }
            for (TextToken token : column.tokens()) {
                body.append(token.text());
            }
        }
        return body.toString();
    }

    private static GameDateTime timeFromObject(Map<String, Object> object) {
        return new GameDateTime(
                JsonData.requiredInt(object, "day", "dialog history game time day"),
                JsonData.requiredString(object, "timeSlotId", "dialog history game time slot id"));
    }
}
