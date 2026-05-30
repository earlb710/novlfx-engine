package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.gamesupport.TimeSaveSnapshots;
import com.eb.javafx.text.DialogSpeaker;
import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON codec for {@link DialogEntriesView}'s visible entries list — used by
 * save/load to round-trip the live dialog block across sessions so the player
 * lands back into the same conversation state they saved from.
 *
 * <h2>Encoded shape</h2>
 *
 * <pre>{@code
 * {
 *   "entries": [
 *     { "type": "plain",   "text": "..." },
 *     { "type": "comment", "text": "..." },
 *     { "type": "spoken",  "lineType": "SAY" | "SHOUT" | "WHISPER" | "CHOICE",
 *                          "text": "...",
 *                          "speakerId": "...", "speakerLabel": "...",
 *                          "speakerColor": "...", "speakerIconId": "..." },
 *     { "type": "conversationStart",
 *                          "startedAt": { "day": 1, "timeSlotId": "morning" },
 *                          "participants": [ { speaker-fields }, ... ] },
 *     { "type": "conversationEnd",
 *                          "endedAt":   { "day": 1, "timeSlotId": "morning" } }
 *   ],
 *   "currentIndex": 5
 * }
 * }</pre>
 *
 * <h2>Speaker fidelity</h2>
 *
 * <p>Speakers are reconstructed from {@code id + label + color + iconId} via
 * {@link DialogSpeaker#iconText} — same construction the runtime resolver uses for
 * authored book characters.  The reconstruction is independent of the host's
 * character roster, so saves remain loadable even after roster regenerations: the
 * speaker's visual identity (colour, label) survives in the saved entries even if
 * the underlying NPC was rerolled.</p>
 */
public final class DialogEntriesViewSnapshotJson {

    private DialogEntriesViewSnapshotJson() {
    }

    public static String toJson(DialogEntriesView view) {
        Validation.requireNonNull(view, "Dialog entries view is required.");
        StringBuilder json = new StringBuilder("{\"entries\":[");
        List<DialogEntriesView.Entry> entries = view.dialogEntries();
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            json.append(entryToJson(entries.get(index)));
        }
        json.append("],\"currentIndex\":").append(view.currentIndex()).append('}');
        return json.toString();
    }

    public static void restore(DialogEntriesView view, String json, String sourceName) {
        Validation.requireNonNull(view, "Dialog entries view is required.");
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        List<Object> rawEntries = JsonData.optionalList(root, "entries", "dialog entries");
        List<DialogEntriesView.Entry> restored = new ArrayList<>(rawEntries.size());
        for (Object raw : rawEntries) {
            restored.add(entryFromJson(JsonData.requireObject(raw, "dialog entry"), sourceName));
        }
        int cursor = root.containsKey("currentIndex")
                ? ((Number) root.get("currentIndex")).intValue()
                : (restored.isEmpty() ? -1 : restored.size() - 1);
        view.restoreDialogEntries(restored, cursor);
    }

    // ---- entry encode/decode ----------------------------------------------------

    private static String entryToJson(DialogEntriesView.Entry entry) {
        if (entry instanceof DialogEntriesView.PlainEntry plain) {
            return "{\"type\":\"plain\",\"text\":" + JsonStrings.quote(plain.text()) + "}";
        }
        if (entry instanceof DialogEntriesView.CommentEntry comment) {
            return "{\"type\":\"comment\",\"text\":" + JsonStrings.quote(comment.text()) + "}";
        }
        if (entry instanceof DialogEntriesView.SpokenEntry spoken) {
            StringBuilder json = new StringBuilder("{\"type\":\"spoken\"");
            json.append(",\"lineType\":").append(JsonStrings.quote(spoken.type().name()));
            json.append(",\"text\":").append(JsonStrings.quote(spoken.text()));
            appendSpeakerFields(json, spoken.speaker());
            json.append('}');
            return json.toString();
        }
        if (entry instanceof DialogEntriesView.ConversationStart start) {
            StringBuilder json = new StringBuilder("{\"type\":\"conversationStart\"");
            json.append(",\"startedAt\":");
            if (start.startedAt() == null) {
                json.append("null");
            } else {
                json.append(TimeSaveSnapshots.toJson(start.startedAt()));
            }
            json.append(",\"participants\":[");
            List<DialogSpeaker> participants = start.participants();
            for (int i = 0; i < participants.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                json.append(speakerToJson(participants.get(i)));
            }
            json.append("]}");
            return json.toString();
        }
        if (entry instanceof DialogEntriesView.ConversationEnd end) {
            StringBuilder json = new StringBuilder("{\"type\":\"conversationEnd\",\"endedAt\":");
            if (end.endedAt() == null) {
                json.append("null");
            } else {
                json.append(TimeSaveSnapshots.toJson(end.endedAt()));
            }
            json.append('}');
            return json.toString();
        }
        throw new IllegalArgumentException("Unsupported dialog entry type for snapshot: "
                + entry.getClass().getName());
    }

    private static DialogEntriesView.Entry entryFromJson(Map<String, Object> object, String sourceName) {
        String type = JsonData.requiredString(object, "type", "dialog entry type");
        return switch (type) {
            case "plain" -> new DialogEntriesView.PlainEntry(
                    JsonData.optionalString(object, "text", "plain entry text").orElse(""));
            case "comment" -> new DialogEntriesView.CommentEntry(
                    JsonData.optionalString(object, "text", "comment entry text").orElse(""));
            case "spoken" -> new DialogEntriesView.SpokenEntry(
                    com.eb.javafx.scene.ConversationDefinition.LineType.valueOf(
                            JsonData.requiredString(object, "lineType", "spoken entry line type")),
                    speakerFromJson(object),
                    JsonData.optionalString(object, "text", "spoken entry text").orElse(""));
            case "conversationStart" -> {
                Object rawStartedAt = object.get("startedAt");
                GameDateTime startedAt = rawStartedAt == null
                        ? null
                        : timeFromObject(JsonData.requireObject(rawStartedAt, "conversation startedAt"));
                List<Object> rawParticipants = JsonData.optionalList(
                        object, "participants", "conversation participants");
                List<DialogSpeaker> participants = new ArrayList<>(rawParticipants.size());
                for (Object raw : rawParticipants) {
                    participants.add(speakerFromJson(JsonData.requireObject(raw, "conversation participant")));
                }
                yield new DialogEntriesView.ConversationStart(participants, startedAt);
            }
            case "conversationEnd" -> {
                Object rawEndedAt = object.get("endedAt");
                GameDateTime endedAt = rawEndedAt == null
                        ? null
                        : timeFromObject(JsonData.requireObject(rawEndedAt, "conversation endedAt"));
                yield new DialogEntriesView.ConversationEnd(endedAt);
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported dialog entry type in " + sourceName + ": " + type);
        };
    }

    // ---- speaker encode/decode --------------------------------------------------

    /** Appends {@code ,"speakerId":"...","speakerLabel":"..."[, ...]} to {@code json}.
     *  When {@code speaker} is null appends nothing.  Always starts with a leading
     *  comma so callers using this in a multi-field object don't need to track
     *  "is this the first field" — the comma is consumed by the prior field's emit. */
    private static void appendSpeakerFields(StringBuilder json, DialogSpeaker speaker) {
        if (speaker == null) {
            return;
        }
        json.append(",\"speakerId\":").append(JsonStrings.quote(speaker.id() == null ? "" : speaker.id()));
        json.append(",\"speakerLabel\":").append(JsonStrings.quote(speaker.label() == null ? "" : speaker.label()));
        if (speaker.hasTextColor()) {
            json.append(",\"speakerColor\":").append(JsonStrings.quote(speaker.textColor()));
        }
        if (speaker.hasIcon()) {
            json.append(",\"speakerIconId\":").append(JsonStrings.quote(speaker.iconId()));
        }
    }

    private static DialogSpeaker speakerFromJson(Map<String, Object> object) {
        String id = JsonData.optionalString(object, "speakerId", "speaker id").orElse("");
        String label = JsonData.optionalString(object, "speakerLabel", "speaker label").orElse("");
        String color = JsonData.optionalString(object, "speakerColor", "speaker color").orElse("");
        String iconId = JsonData.optionalString(object, "speakerIconId", "speaker icon").orElse("");
        if (id.isBlank() && label.isBlank()) {
            // Defensive fallback — the field set is required for spoken entries by the
            // writer, but a manually-edited save could omit them.  Use a sentinel id
            // so the renderer doesn't crash; the label drops to empty.
            id = "saved";
        }
        return DialogSpeaker.iconText(
                id.isBlank() ? "saved" : id,
                label.isBlank() ? id : label,
                iconId.isBlank() ? null : iconId,
                color.isBlank() ? null : color);
    }

    private static GameDateTime timeFromObject(Map<String, Object> object) {
        return new GameDateTime(
                JsonData.requiredInt(object, "day", "dialog entry game time day"),
                JsonData.requiredString(object, "timeSlotId", "dialog entry game time slot id"));
    }

    /** Returns a complete speaker JSON object — used by the participants list which
     *  needs full objects per speaker, not the leading-comma inline fields that
     *  {@link #appendSpeakerFields} emits.  Always non-null and non-empty even when
     *  {@code speaker} has only id+label set. */
    private static String speakerToJson(DialogSpeaker speaker) {
        if (speaker == null) {
            return "{}";
        }
        StringBuilder json = new StringBuilder("{");
        json.append("\"speakerId\":").append(JsonStrings.quote(speaker.id() == null ? "" : speaker.id()));
        json.append(",\"speakerLabel\":").append(JsonStrings.quote(speaker.label() == null ? "" : speaker.label()));
        if (speaker.hasTextColor()) {
            json.append(",\"speakerColor\":").append(JsonStrings.quote(speaker.textColor()));
        }
        if (speaker.hasIcon()) {
            json.append(",\"speakerIconId\":").append(JsonStrings.quote(speaker.iconId()));
        }
        json.append('}');
        return json.toString();
    }
}
