package com.eb.javafx.text;

import com.eb.javafx.util.Validation;

/**
 * Speaker metadata used by dialog history messages.
 *
 * <p>The optional icon, text color, and font fields let UI renderers show a
 * portrait/icon alongside a colored speaker label without hard-coding
 * presentation details into history storage.</p>
 */
public final class DialogSpeaker {
    private final String id;
    private final String label;
    private final String iconId;
    private final String textColor;
    private final String textFont;

    public DialogSpeaker(String id, String label, String iconId, String textColor) {
        this(id, label, iconId, textColor, null);
    }

    public DialogSpeaker(String id, String label, String iconId, String textColor, String textFont) {
        this.id = Validation.requireNonBlank(id, "Dialog speaker id is required.");
        // A blank/null label is allowed: it marks an unnamed narrator / system line, which
        // DialogEntriesView renders as bare body (no speaker column, but still in the speaker's
        // colour).  Normalise null to "" so callers of label() never see null.
        this.label = label == null ? "" : label;
        this.iconId = iconId;
        this.textColor = textColor;
        this.textFont = textFont;
    }

    public static DialogSpeaker text(String id, String label) {
        return new DialogSpeaker(id, label, null, null);
    }

    public static DialogSpeaker iconText(String id, String label, String iconId, String textColor) {
        return new DialogSpeaker(id, label, iconId, textColor);
    }

    public static DialogSpeaker iconText(String id, String label, String iconId, String textColor, String textFont) {
        return new DialogSpeaker(id, label, iconId, textColor, textFont);
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

    public String textFont() {
        return textFont;
    }

    public boolean hasIcon() {
        return iconId != null && !iconId.isBlank();
    }

    public boolean hasTextColor() {
        return textColor != null && !textColor.isBlank();
    }

    public boolean hasTextFont() {
        return textFont != null && !textFont.isBlank();
    }
}
