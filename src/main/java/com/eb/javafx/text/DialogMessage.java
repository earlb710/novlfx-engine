package com.eb.javafx.text;

import com.eb.javafx.util.ImmutableCollections;
import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Map;

/**
 * One message row inside a dialog history entry.
 *
 * <p>Messages may be narration with arbitrary columns or the common speaker/message
 * shape. The stored columns contain parsed {@link TextToken} values so formatting
 * tags can be rendered or inspected later.</p>
 */
public final class DialogMessage {
    private final DialogSpeaker speaker;
    private final List<DialogColumn> columns;
    private final Map<String, String> metadata;

    public DialogMessage(DialogSpeaker speaker, List<DialogColumn> columns, Map<String, String> metadata) {
        this.speaker = speaker;
        this.columns = List.copyOf(Validation.requireNonEmpty(columns, "Dialog message columns are required."));
        this.metadata = ImmutableCollections.copyMap(metadata);
    }

    public static DialogMessage speakerMessage(DialogSpeaker speaker, String message) {
        Validation.requireNonNull(speaker, "Dialog speaker is required.");
        return new DialogMessage(speaker, List.of(DialogColumn.speaker(speaker), DialogColumn.message(message)), Map.of());
    }

    public static DialogMessage columns(List<DialogColumn> columns) {
        return new DialogMessage(null, columns, Map.of());
    }

    public DialogSpeaker speaker() {
        return speaker;
    }

    public boolean hasSpeaker() {
        return speaker != null;
    }

    public List<DialogColumn> columns() {
        return columns;
    }

    public Map<String, String> metadata() {
        return metadata;
    }
}
