package com.eb.javafx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Reusable button-styling helpers layered over {@link ButtonVisuals}: apply bevel SVG artwork to a
 * button (or every button in a subtree), build a centered button row, and center button-only
 * containers. Style-class names, spacing, padding, and the hover tint are all caller-supplied so
 * nothing here is tied to a particular game's look.
 *
 * <p>Threading: JavaFX application thread only.</p>
 */
public final class ButtonStyling {

    private ButtonStyling() {
    }

    /**
     * Applies bevel SVG artwork to {@code button}, tags it with {@code bevelStyleClass}, and lets it
     * grow vertically for multi-line labels. On hover the supplied {@code hoverTint} (nullable) is
     * applied to the button's image graphic and cleared on exit.
     */
    public static Button bevel(Button button, String bevelStyleClass, ColorAdjust hoverTint) {
        button.setPrefWidth(ButtonVisuals.BUTTON_ARTWORK_WIDTH);
        // Leave prefHeight unset so the rasterised artwork uses computed text height for multiline labels.
        ButtonVisuals.applyBevelSvgArtwork(button);
        if (bevelStyleClass != null && !button.getStyleClass().contains(bevelStyleClass)) {
            button.getStyleClass().add(bevelStyleClass);
        }
        button.setMaxWidth(Region.USE_PREF_SIZE);
        // Override the fixed height applyBevelSvgArtwork set so the button can expand vertically.
        button.setMinHeight(Region.USE_COMPUTED_SIZE);
        button.setPrefHeight(Region.USE_COMPUTED_SIZE);
        button.setMaxHeight(Double.MAX_VALUE);
        button.setOnMouseEntered(e -> applyImageEffect(button.getGraphic(), hoverTint));
        button.setOnMouseExited(e -> applyImageEffect(button.getGraphic(), null));
        return button;
    }

    /** Recursively bevels every {@link Button} in {@code node}'s subtree. */
    public static void bevelDescendants(Node node, String bevelStyleClass, ColorAdjust hoverTint) {
        if (node instanceof Button button) {
            bevel(button, bevelStyleClass, hoverTint);
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable()
                    .forEach(child -> bevelDescendants(child, bevelStyleClass, hoverTint));
        }
    }

    /** Builds a horizontally-centered row of {@code buttons} with the given style class, spacing,
     *  and padding. */
    public static HBox centeredRow(String rowStyleClass, double spacing, Insets padding,
            Button... buttons) {
        HBox row = new HBox(spacing);
        if (rowStyleClass != null) {
            row.getStyleClass().add(rowStyleClass);
        }
        row.setAlignment(Pos.CENTER);
        row.setPadding(padding);
        row.getChildren().addAll(buttons);
        return row;
    }

    /** Recursively centers any {@link VBox}/{@link HBox} in {@code node}'s subtree that contains
     *  only buttons, applying at least {@code spacing} and {@code padding}. */
    public static void centerButtonContainers(Node node, double spacing, Insets padding) {
        if (node instanceof VBox vbox && containsOnlyButtons(vbox)) {
            vbox.setAlignment(Pos.CENTER);
            vbox.setFillWidth(false);
            vbox.setSpacing(Math.max(vbox.getSpacing(), spacing));
            vbox.setPadding(maxPadding(vbox.getPadding(), padding));
        }
        if (node instanceof HBox hbox && containsOnlyButtons(hbox)) {
            hbox.setAlignment(Pos.CENTER);
            hbox.setSpacing(Math.max(hbox.getSpacing(), spacing));
            hbox.setPadding(maxPadding(hbox.getPadding(), padding));
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable()
                    .forEach(child -> centerButtonContainers(child, spacing, padding));
        }
    }

    private static boolean containsOnlyButtons(Region region) {
        return !region.getChildrenUnmodifiable().isEmpty()
                && region.getChildrenUnmodifiable().stream().allMatch(Button.class::isInstance);
    }

    private static Insets maxPadding(Insets original, Insets update) {
        Insets existing = original == null ? Insets.EMPTY : original;
        return new Insets(
                Math.max(existing.getTop(), update.getTop()),
                Math.max(existing.getRight(), update.getRight()),
                Math.max(existing.getBottom(), update.getBottom()),
                Math.max(existing.getLeft(), update.getLeft()));
    }

    private static void applyImageEffect(Node node, ColorAdjust effect) {
        if (node == null) {
            return;
        }
        if (node instanceof ImageView imageView) {
            imageView.setEffect(effect);
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(child -> applyImageEffect(child, effect));
        }
    }
}
