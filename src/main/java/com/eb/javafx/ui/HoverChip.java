package com.eb.javafx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.Locale;

/**
 * A small clickable chip: an icon glyph + a bold label, with a rest/hover background swap and a hand
 * cursor. The rest/hover inline styles and an optional stable-id prefix are caller-supplied, so the
 * chip carries no game-specific look of its own.
 *
 * <p>When {@code idPrefix} is non-blank the chip gets a stable id of
 * {@code <idPrefix>-<label lower-cased, spaces→'-'>} (e.g. {@code "launcher-journal"}) so automation /
 * a test driver can find and press it by name independent of the glyph. Extracted from AltLife's
 * {@code launcherChip}. JavaFX-thread-only.</p>
 */
public final class HoverChip {

    private HoverChip() {
    }

    /**
     * Builds the chip.
     *
     * @param icon      the leading glyph (emoji / symbol)
     * @param label     the bold text label (also seeds the stable id)
     * @param restStyle inline style at rest
     * @param hoverStyle inline style while hovered
     * @param idPrefix  stable-id prefix, or {@code null}/blank to leave the chip un-id'd
     * @param onClick   run on click, or {@code null} for a non-interactive chip
     */
    public static StackPane build(String icon, String label, String restStyle, String hoverStyle,
            String idPrefix, Runnable onClick) {
        Label glyph = new Label(icon);
        glyph.setStyle("-fx-font-size: 16;");
        Label text = new Label(label);
        text.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold;");
        HBox inner = new HBox(6, glyph, text);
        inner.setAlignment(Pos.CENTER_LEFT);
        StackPane chip = new StackPane(inner);
        if (idPrefix != null && !idPrefix.isBlank()) {
            chip.setId(idPrefix + "-" + label.toLowerCase(Locale.ROOT).replace(' ', '-'));
        }
        chip.setPadding(new Insets(4, 12, 4, 12));
        chip.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        chip.setStyle(restStyle);
        chip.setCursor(Cursor.HAND);
        chip.setOnMouseEntered(e -> chip.setStyle(hoverStyle));
        chip.setOnMouseExited(e -> chip.setStyle(restStyle));
        if (onClick != null) {
            chip.setOnMouseClicked(e -> onClick.run());
        }
        return chip;
    }
}
