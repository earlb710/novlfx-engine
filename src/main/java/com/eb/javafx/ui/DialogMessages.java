package com.eb.javafx.ui;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * In-scene modal-dialog helpers that paint themselves on top of the current scene's root
 * rather than spawning a separate {@link javafx.stage.Stage} via {@link javafx.scene.control.Alert}.
 *
 * <h2>Why not Alert?</h2>
 *
 * <p>JavaFX's stock {@link javafx.scene.control.Alert} creates a child {@code Stage}, and
 * child stages do not cooperate with a fullscreen primary stage on Windows / macOS / Linux —
 * the alert frequently appears behind the fullscreen window or steals focus without
 * surfacing.  Hosts that ship in fullscreen (most novlfx games) therefore need an in-scene
 * fallback: a {@link StackPane} overlay that sits on top of the live scene root, blocks
 * clicks behind it, and resolves via a callback.  This helper centralises that pattern so
 * every host gets the same look + behaviour without each one rolling its own modal.</p>
 *
 * <h2>Theme integration</h2>
 *
 * <p>Pass a {@link UiTheme} to drive the accent colours used on the dialog card's border,
 * the primary button background, and the title text fill.  When the theme is {@code null}
 * the helpers fall back to a neutral grey palette so error reporting in tests / headless
 * paths still produces a usable surface.</p>
 *
 * <h2>Requirements on the scene</h2>
 *
 * <p>The supplied scene's root must be a {@link Pane} (or extend it — {@link StackPane},
 * {@link VBox}, {@link javafx.scene.layout.BorderPane}, etc. all qualify).  Almost every
 * novlfx-built shell wraps its content in a StackPane via
 * {@code ScreenShell.withConfiguredBackground}, so this requirement is met out of the box.
 * If the root isn't a {@code Pane} the helpers silently fall back to {@link javafx.scene.control.Alert}
 * so the dialog still surfaces somehow.</p>
 *
 * <h2>Threading</h2>
 *
 * <p>JavaFX application thread only — same constraint as every other scene-graph
 * manipulation in the engine.</p>
 */
public final class DialogMessages {

    /** Result of a {@link #confirm} prompt — the action the player chose. */
    public enum Result { OK, CANCEL }

    private static final String DEFAULT_ACCENT = "#888888";
    private static final String DEFAULT_TEXT   = "#e6e6e6";
    private static final String CARD_BG        = "rgba(28, 32, 40, 0.96)";
    private static final String BACKDROP_BG    = "rgba(0, 0, 0, 0.55)";
    private static final double DEFAULT_CARD_MIN_WIDTH = 360.0;
    private static final double DEFAULT_CARD_MAX_WIDTH = 520.0;

    // Config-overridable card width (config.json -> ui.dialog.minWidth / maxWidth, wired at boot
    // by BootstrapService).  Defaults preserve the original 360 / 520 layout.
    private static double cardMinWidth = DEFAULT_CARD_MIN_WIDTH;
    private static double cardMaxWidth = DEFAULT_CARD_MAX_WIDTH;

    private DialogMessages() {
    }

    /** Override the confirm/info/error dialog card width.  Null or non-positive values keep the
     *  current value; a max below the min is clamped up to the min.  Called once at boot. */
    public static void setCardWidth(Double minWidth, Double maxWidth) {
        if (minWidth != null && minWidth > 0) {
            cardMinWidth = minWidth;
        }
        if (maxWidth != null && maxWidth > 0) {
            cardMaxWidth = maxWidth;
        }
        if (cardMaxWidth < cardMinWidth) {
            cardMaxWidth = cardMinWidth;
        }
    }

    /** Yes/No confirmation dialog.  Calls {@code callback} with the chosen result after the
     *  player clicks OK or Cancel, or presses Enter / Escape respectively.  Clicking the
     *  dim backdrop outside the card dismisses with {@link Result#CANCEL}.
     *
     *  <p>{@code header} and {@code content} both render in the card body; pass an empty
     *  string for either to omit that row.</p> */
    public static void confirm(Scene scene, UiTheme theme, String title, String header,
                                 String content, Consumer<Result> callback) {
        if (callback == null) {
            return;
        }
        if (!hasOverlayContainer(scene)) {
            fallbackConfirm(title, header, content, callback);
            return;
        }
        showOverlay(scene, theme, title, header, content,
                /*okLabel*/ "OK", /*cancelLabel*/ "Cancel", callback);
    }

    /** Convenience variant that derives the {@link Scene} from {@code anchor.getScene()}.
     *  Useful for callers that have a click-source Node but not the scene reference. */
    public static void confirm(Node anchor, UiTheme theme, String title, String header,
                                 String content, Consumer<Result> callback) {
        confirm(anchor == null ? null : anchor.getScene(), theme, title, header, content, callback);
    }

    /** Single-button information dialog.  Calls {@code onClose} (may be null) after the
     *  player dismisses via OK / Enter / Escape / backdrop click. */
    public static void info(Scene scene, UiTheme theme, String title, String header,
                              String content, Runnable onClose) {
        showSingleButton(scene, theme, title, header, content, "OK", onClose);
    }

    /** Single-button error dialog.  Visually identical to {@link #info} today; kept as a
     *  separate entry point so future styling tweaks (red accent, exclamation icon, etc.)
     *  can target only error surfaces. */
    public static void error(Scene scene, UiTheme theme, String title, String header,
                               String content, Runnable onClose) {
        showSingleButton(scene, theme, title, header, content, "OK", onClose);
    }

    /** Text-input prompt.  Renders a card with title / header / content + a single-line
     *  {@link javafx.scene.control.TextField} pre-populated with {@code defaultValue},
     *  plus OK and Cancel buttons.  The {@code callback} receives the entered text on
     *  OK, or {@code null} on Cancel / Escape / backdrop click — null is the unambiguous
     *  "user dismissed without confirming" signal.  Enter on the text field submits OK.
     *
     *  <p>Falls back to {@link javafx.scene.control.TextInputDialog} when the scene root
     *  isn't a {@link Pane} (same back-compat shape as {@link #confirm}).</p> */
    public static void prompt(Scene scene, UiTheme theme, String title, String header,
                                String content, String defaultValue,
                                java.util.function.Consumer<String> callback) {
        if (callback == null) {
            return;
        }
        if (!hasOverlayContainer(scene)) {
            fallbackPrompt(title, header, content, defaultValue, callback);
            return;
        }
        showPromptOverlay(scene, theme, title, header, content, defaultValue, callback);
    }

    // ----- Implementation ---------------------------------------------------------------

    private static void showSingleButton(Scene scene, UiTheme theme, String title, String header,
                                          String content, String okLabel, Runnable onClose) {
        if (!hasOverlayContainer(scene)) {
            fallbackInfo(title, header, content, onClose);
            return;
        }
        Consumer<Result> bridge = result -> {
            if (onClose != null) {
                onClose.run();
            }
        };
        // The single-button surface still uses the two-button overlay shape but with the
        // cancelLabel set to null so only the OK button renders.
        showOverlay(scene, theme, title, header, content, okLabel, /*cancelLabel*/ null, bridge);
    }

    private static void showOverlay(Scene scene, UiTheme theme, String title, String header,
                                      String content, String okLabel, String cancelLabel,
                                      Consumer<Result> callback) {
        Pane container = (Pane) scene.getRoot();
        String accent = theme == null ? DEFAULT_ACCENT : safeColor(theme.accentColor(), DEFAULT_ACCENT);
        String textHex = theme == null ? DEFAULT_TEXT : safeColor(theme.textColor(), DEFAULT_TEXT);

        // ---- Card body ---------------------------------------------------------------
        // setMaxHeight(USE_PREF_SIZE) is the critical bit — VBox's default maxHeight is
        // unbounded (Double.MAX_VALUE), so when the card sits in the full-screen overlay
        // StackPane, StackPane resizes the resizable VBox up to its max and the card
        // ends up stretching floor-to-ceiling.  Pinning maxHeight to USE_PREF_SIZE makes
        // the card lay out at its content's preferred height (title + header + content +
        // buttons + padding) so it reads as a tidy centred dialog rather than a
        // full-height panel.  Same treatment on maxWidth via CARD_MAX_WIDTH below.
        VBox card = new VBox(12);
        card.setMinWidth(cardMinWidth);
        card.setMaxWidth(cardMaxWidth);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("dialog-message-card");
        // Corner radius lives in CSS (.dialog-message-card); colours/border-width stay inline.
        card.setStyle(
                "-fx-background-color: " + CARD_BG + ";"
                + " -fx-border-color: " + accent + ";"
                + " -fx-border-width: 1.5;");

        if (title != null && !title.isBlank()) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("dialog-message-title");
            titleLabel.setStyle("-fx-text-fill: " + accent + ";");
            card.getChildren().add(titleLabel);
        }
        if (header != null && !header.isBlank()) {
            Label headerLabel = new Label(header);
            headerLabel.setWrapText(true);
            headerLabel.getStyleClass().add("dialog-message-header");
            headerLabel.setStyle("-fx-text-fill: " + textHex + ";");
            card.getChildren().add(headerLabel);
        }
        if (content != null && !content.isBlank()) {
            Label contentLabel = new Label(content);
            contentLabel.setWrapText(true);
            contentLabel.getStyleClass().add("dialog-message-content");
            contentLabel.setStyle("-fx-text-fill: " + textHex + ";");
            card.getChildren().add(contentLabel);
        }

        // ---- Button row -------------------------------------------------------------
        Button okButton = themedButton(okLabel, /*primary*/ true, accent, textHex);
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 0, 0, 0));
        Region buttonSpacer = new Region();
        HBox.setHgrow(buttonSpacer, javafx.scene.layout.Priority.ALWAYS);
        buttons.getChildren().add(buttonSpacer);

        final Button cancelButton = cancelLabel == null
                ? null
                : themedButton(cancelLabel, /*primary*/ false, accent, textHex);
        if (cancelButton != null) {
            buttons.getChildren().add(cancelButton);
        }
        buttons.getChildren().add(okButton);
        card.getChildren().add(buttons);

        // ---- Backdrop + overlay -----------------------------------------------------
        StackPane overlay = new StackPane(card);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: " + BACKDROP_BG + ";");
        overlay.setPickOnBounds(true);

        // Resizes the overlay to match the container's current width / height so it
        // fully covers whatever the scene root is showing — including fullscreen-stretched
        // scenes where stock Alert dialogs fail.
        overlay.prefWidthProperty().bind(container.widthProperty());
        overlay.prefHeightProperty().bind(container.heightProperty());

        Runnable[] dismiss = new Runnable[1];
        Consumer<Result> resolve = result -> {
            if (dismiss[0] != null) {
                dismiss[0].run();
            }
            callback.accept(result);
        };

        // ---- Wire actions ----------------------------------------------------------
        okButton.setOnAction(event -> resolve.accept(Result.OK));
        if (cancelButton != null) {
            cancelButton.setOnAction(event -> resolve.accept(Result.CANCEL));
        }
        // Click outside the card dismisses as CANCEL (or as OK for single-button surfaces
        // so the player can dismiss an info / error toast by clicking anywhere).
        overlay.setOnMouseClicked(event -> {
            if (event.getTarget() == overlay) {
                resolve.accept(cancelButton == null ? Result.OK : Result.CANCEL);
            }
        });
        // Card consumes its own clicks so backdrop dismissal doesn't trigger when the
        // player clicks the dialog's own surface.
        card.setOnMouseClicked(event -> event.consume());

        // Keyboard: Enter = OK, Escape = Cancel (or Enter / Escape both = OK on single-button).
        EventHandler<KeyEvent> keyFilter = event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                resolve.accept(Result.OK);
            } else if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                resolve.accept(cancelButton == null ? Result.OK : Result.CANCEL);
            }
        };
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);

        dismiss[0] = () -> {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            container.getChildren().remove(overlay);
        };

        container.getChildren().add(overlay);
        okButton.requestFocus();
    }

    private static Button themedButton(String label, boolean primary, String accent, String textHex) {
        Button btn = new Button(label);
        btn.setMinWidth(90);
        btn.setFocusTraversable(true);
        btn.getStyleClass().add("dialog-message-button");
        String bg = primary ? accent : "rgba(255, 255, 255, 0.08)";
        String fg = primary ? "#ffffff" : textHex;
        // Corner radius lives in CSS (.dialog-message-button); colours/padding stay inline.
        btn.setStyle(
                "-fx-background-color: " + bg + ";"
                + " -fx-text-fill: " + fg + ";"
                + " -fx-padding: 6 18 6 18;"
                + " -fx-border-color: " + accent + ";"
                + " -fx-border-width: 1;"
                + " -fx-cursor: hand;"
                + " -fx-focus-color: transparent;"
                + " -fx-faint-focus-color: transparent;");
        return btn;
    }

    /** Renders a confirm-shaped card with a TextField between the header and the buttons.
     *  Shape is intentionally the same as {@link #showOverlay} so visual treatment stays
     *  consistent across dialog types; the only delta is the prompt field and the
     *  callback signature ({@code Consumer<String>} → text on OK, null on Cancel). */
    private static void showPromptOverlay(Scene scene, UiTheme theme, String title, String header,
                                            String content, String defaultValue,
                                            java.util.function.Consumer<String> callback) {
        Pane container = (Pane) scene.getRoot();
        String accent = theme == null ? DEFAULT_ACCENT : safeColor(theme.accentColor(), DEFAULT_ACCENT);
        String textHex = theme == null ? DEFAULT_TEXT : safeColor(theme.textColor(), DEFAULT_TEXT);

        VBox card = new VBox(12);
        card.setMinWidth(cardMinWidth);
        card.setMaxWidth(cardMaxWidth);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("dialog-message-card");
        // Corner radius lives in CSS (.dialog-message-card); colours/border-width stay inline.
        card.setStyle(
                "-fx-background-color: " + CARD_BG + ";"
                + " -fx-border-color: " + accent + ";"
                + " -fx-border-width: 1.5;");

        if (title != null && !title.isBlank()) {
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("dialog-message-title");
            titleLabel.setStyle("-fx-text-fill: " + accent + ";");
            card.getChildren().add(titleLabel);
        }
        if (header != null && !header.isBlank()) {
            Label headerLabel = new Label(header);
            headerLabel.setWrapText(true);
            headerLabel.getStyleClass().add("dialog-message-header");
            headerLabel.setStyle("-fx-text-fill: " + textHex + ";");
            card.getChildren().add(headerLabel);
        }
        if (content != null && !content.isBlank()) {
            Label contentLabel = new Label(content);
            contentLabel.setWrapText(true);
            contentLabel.getStyleClass().add("dialog-message-content");
            contentLabel.setStyle("-fx-text-fill: " + textHex + ";");
            card.getChildren().add(contentLabel);
        }

        javafx.scene.control.TextField textField = new javafx.scene.control.TextField(
                defaultValue == null ? "" : defaultValue);
        textField.getStyleClass().add("dialog-message-input");
        // Corner radius lives in CSS (.dialog-message-input); colours/padding stay inline.
        textField.setStyle(
                "-fx-background-color: rgba(255,255,255,0.08);"
                + " -fx-text-fill: " + textHex + ";"
                + " -fx-prompt-text-fill: rgba(230,230,230,0.5);"
                + " -fx-font-size: 14px;"
                + " -fx-padding: 6 10 6 10;"
                + " -fx-border-color: " + accent + ";"
                + " -fx-border-width: 1;");
        card.getChildren().add(textField);

        Button okButton = themedButton("OK", true, accent, textHex);
        Button cancelButton = themedButton("Cancel", false, accent, textHex);
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 0, 0, 0));
        Region buttonSpacer = new Region();
        HBox.setHgrow(buttonSpacer, javafx.scene.layout.Priority.ALWAYS);
        buttons.getChildren().addAll(buttonSpacer, cancelButton, okButton);
        card.getChildren().add(buttons);

        StackPane overlay = new StackPane(card);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: " + BACKDROP_BG + ";");
        overlay.setPickOnBounds(true);
        overlay.prefWidthProperty().bind(container.widthProperty());
        overlay.prefHeightProperty().bind(container.heightProperty());

        Runnable[] dismiss = new Runnable[1];
        java.util.function.Consumer<String> resolve = value -> {
            if (dismiss[0] != null) {
                dismiss[0].run();
            }
            callback.accept(value);
        };

        okButton.setOnAction(event -> resolve.accept(textField.getText()));
        cancelButton.setOnAction(event -> resolve.accept(null));
        overlay.setOnMouseClicked(event -> {
            if (event.getTarget() == overlay) {
                resolve.accept(null);
            }
        });
        card.setOnMouseClicked(event -> event.consume());

        // Enter inside the text field submits OK; Escape on either field or backdrop
        // cancels.  Enter routed via the scene-level filter so the text field's default
        // newline handling doesn't swallow it first.
        EventHandler<KeyEvent> keyFilter = event -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                resolve.accept(textField.getText());
            } else if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                resolve.accept(null);
            }
        };
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);

        dismiss[0] = () -> {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
            container.getChildren().remove(overlay);
        };

        container.getChildren().add(overlay);
        // Pre-select the default value so the player can either accept it (Enter) or
        // type over it immediately — same affordance as the OS Save dialog.
        textField.requestFocus();
        textField.selectAll();
    }

    private static boolean hasOverlayContainer(Scene scene) {
        return scene != null && scene.getRoot() instanceof Pane;
    }

    private static String safeColor(String hex, String fallback) {
        return (hex == null || hex.isBlank()) ? fallback : hex;
    }

    // ----- Stage-based fallbacks (used only when the scene root isn't a Pane) ----------

    private static void fallbackConfirm(String title, String header, String content,
                                         Consumer<Result> callback) {
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait().ifPresent(button ->
                callback.accept(button == javafx.scene.control.ButtonType.OK
                        ? Result.OK : Result.CANCEL));
    }

    private static void fallbackPrompt(String title, String header, String content,
                                         String defaultValue,
                                         java.util.function.Consumer<String> callback) {
        javafx.scene.control.TextInputDialog dialog =
                new javafx.scene.control.TextInputDialog(defaultValue == null ? "" : defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(content);
        dialog.showAndWait().ifPresentOrElse(
                callback::accept,
                () -> callback.accept(null));
    }

    private static void fallbackInfo(String title, String header, String content, Runnable onClose) {
        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
        if (onClose != null) {
            onClose.run();
        }
    }
}
