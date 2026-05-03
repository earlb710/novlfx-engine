package com.eb.javafx.text;

import com.eb.javafx.util.Validation;

/**
 * Speaker metadata used by dialog history messages.
 *
 * <p>The optional icon and text color fields let UI renderers show a portrait/icon
 * alongside a colored speaker label without hard-coding presentation details into
 * history storage.</p>
 */
public final class DialogSpeaker {
    private final String id;
    private final String label;
    private final String iconId;
    private final String textColor;

    public DialogSpeaker(String id, String label, String iconId, String textColor) {
        this.id = Validation.requireNonBlank(id, "Dialog speaker id is required.");
        this.label = Validation.requireNonBlank(label, "Dialog speaker label is required.");
        this.iconId = iconId;
        this.textColor = textColor;
    }

    public static DialogSpeaker text(String id, String label) {
        return new DialogSpeaker(id, label, null, null);
    }

    public static DialogSpeaker iconText(String id, String label, String iconId, String textColor) {
        return new DialogSpeaker(id, label, iconId, textColor);
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public String iconId() {
        return iconId;
    }

    public String textColor() {
        return textColor;
    }

    public boolean hasIcon() {
        return iconId != null && !iconId.isBlank();
    }

    public boolean hasTextColor() {
        return textColor != null && !textColor.isBlank();
    }
}
