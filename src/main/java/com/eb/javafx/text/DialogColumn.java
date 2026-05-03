package com.eb.javafx.text;

import com.eb.javafx.util.Validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One formatted column in a dialog history row.
 *
 * <p>Columns carry parsed text tokens so renderers can preserve inline formatting
 * while still arranging speaker, message, or custom content in separate visual
 * columns.</p>
 */
public final class DialogColumn {
    public static final String SPEAKER_COLUMN = "speaker";
    public static final String MESSAGE_COLUMN = "message";

    private static final TextTagParser PARSER = new TextTagParser();

    private final String id;
    private final List<TextToken> tokens;

    public DialogColumn(String id, List<TextToken> tokens) {
        this.id = Validation.requireNonBlank(id, "Dialog column id is required.");
        this.tokens = List.copyOf(Validation.requireNonEmpty(tokens, "Dialog column tokens are required."));
    }

    public static DialogColumn parsed(String id, String source) {
        Validation.requireNonBlank(source, "Dialog column text is required.");
        return new DialogColumn(id, PARSER.parse(source));
    }

    public static DialogColumn message(String source) {
        return parsed(MESSAGE_COLUMN, source);
    }

    public static DialogColumn speaker(DialogSpeaker speaker) {
        Validation.requireNonNull(speaker, "Dialog speaker is required.");
        Map<String, String> effects = new LinkedHashMap<>();
        if (speaker.hasIcon()) {
            effects.put("icon", speaker.iconId());
        }
        TextStyle style = new TextStyle(false, false, speaker.textColor(), speaker.textFont(), effects);
        return new DialogColumn(SPEAKER_COLUMN, List.of(TextToken.text(speaker.label(), style)));
    }

    public String id() {
        return id;
    }

    public List<TextToken> tokens() {
        return tokens;
    }
}
