package com.eb.javafx.ui;

import com.eb.javafx.util.Validation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Reusable error screen for surfacing an exception or non-fatal failure to the player.
 *
 * <p>The screen is intentionally distinct from normal gameplay screens — it is themed in dark red
 * so the player immediately recognises that something has gone wrong. The layout is three zones:</p>
 *
 * <ul>
 *   <li><b>Title</b> — the kind of error, e.g. the {@link Throwable} class name or a caller-supplied
 *       category. Falls back to {@link #DEFAULT_TITLE} when nothing is provided.</li>
 *   <li><b>Body</b> — an optional short message line followed by a read-only {@link TextArea}
 *       displaying the long technical details (a full stack trace by default). The text area is
 *       selectable so the player can {@code Ctrl+A} / {@code Ctrl+C} the content; a dedicated
 *       <b>Copy details</b> button copies everything to the clipboard in one click.</li>
 *   <li><b>Actions</b> — a <b>Continue</b> button (shown only when the caller supplies a
 *       {@code continueAction}, signalling that the failure is recoverable) and an <b>Exit</b>
 *       button. Both are styled as dark-red emphasis buttons.</li>
 * </ul>
 *
 * <p>The screen is built from a {@link Options} record so it can be constructed both manually and
 * from a {@link Throwable} via {@link Options#ofException(Throwable, Runnable, Runnable)}. The
 * public API is a static {@link #createScene(Options, double, double)} factory plus a
 * {@link #buildRoot(Options)} helper for embedding the screen into an existing scene.</p>
 */
public final class ErrorScreen {

    /** Title used when {@link Options#title()} is {@code null} or blank. */
    public static final String DEFAULT_TITLE = "Something went wrong";

    public static final String STYLE_CLASS = "error-screen";
    public static final String TITLE_STYLE_CLASS = "error-screen-title";
    public static final String MESSAGE_STYLE_CLASS = "error-screen-message";
    public static final String DETAILS_STYLE_CLASS = "error-screen-details";
    public static final String ACTIONS_STYLE_CLASS = "error-screen-actions";
    public static final String CONTINUE_BUTTON_STYLE_CLASS = "error-screen-continue-button";
    public static final String EXIT_BUTTON_STYLE_CLASS = "error-screen-exit-button";
    public static final String COPY_BUTTON_STYLE_CLASS = "error-screen-copy-button";

    private ErrorScreen() {
    }

    /**
     * Configuration for the error screen.
     *
     * @param title           the heading shown at the top — typically the exception class name or
     *                        a category string; falls back to {@link #DEFAULT_TITLE} when
     *                        {@code null} or blank
     * @param message         optional short user-facing description; rendered as a single-line
     *                        label above the details text area; may be {@code null} to omit
     * @param details         the long technical body — full stack trace, validation report, etc.;
     *                        rendered into a read-only, copyable {@link TextArea}; never
     *                        {@code null} (use empty string instead)
     * @param continueAction  invoked when the player clicks <b>Continue</b>; when {@code null} the
     *                        Continue button is hidden, signalling that the failure is fatal
     * @param exitAction      invoked when the player clicks <b>Exit</b>; required (an error screen
     *                        without an exit path would trap the player)
     */
    public record Options(
            String title,
            String message,
            String details,
            Runnable continueAction,
            Runnable exitAction) {

        public Options {
            // title may be null / blank — effectiveTitle() falls back.
            // message may be null — the message label is omitted in that case.
            details = details == null ? "" : details;
            Validation.requireNonNull(exitAction, "ErrorScreen.Options.exitAction is required.");
        }

        /** Returns {@link #title()} when present, otherwise {@link ErrorScreen#DEFAULT_TITLE}. */
        public String effectiveTitle() {
            return title == null || title.isBlank() ? DEFAULT_TITLE : title;
        }

        /** {@code true} when {@link #continueAction()} is non-null; the Continue button shows iff true. */
        public boolean continueAvailable() {
            return continueAction != null;
        }

        /**
         * Builds options for a fatal exception — no Continue button, just Exit. The title is
         * populated from the throwable's simple class name and the details are the full stack
         * trace.
         */
        public static Options ofException(Throwable error, Runnable exitAction) {
            return ofException(error, null, exitAction);
        }

        /**
         * Builds options for any throwable. When {@code continueAction} is non-null the screen
         * shows the Continue button so the player can recover from the failure; pass {@code null}
         * for fatal errors.
         */
        public static Options ofException(Throwable error, Runnable continueAction, Runnable exitAction) {
            Validation.requireNonNull(error, "Throwable is required.");
            String title = error.getClass().getSimpleName();
            String message = error.getMessage();
            String details = stackTraceText(error);
            return new Options(title, message, details, continueAction, exitAction);
        }
    }

    /**
     * Builds a {@link Scene} hosting the error screen at the requested dimensions.
     *
     * <p>Callers that already have a {@link Scene} should use {@link #buildRoot(Options)} and set
     * the result as the scene's root instead.</p>
     */
    public static Scene createScene(Options options, double width, double height) {
        return new Scene(buildRoot(options), width, height);
    }

    /** Builds the JavaFX node tree for the error screen. */
    public static Parent buildRoot(Options options) {
        Validation.requireNonNull(options, "ErrorScreen.Options is required.");

        Label titleLabel = new Label(options.effectiveTitle());
        titleLabel.getStyleClass().add(TITLE_STYLE_CLASS);
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        VBox body = new VBox(8);
        body.setAlignment(Pos.TOP_LEFT);
        body.setFillWidth(true);

        if (options.message() != null && !options.message().isBlank()) {
            Label messageLabel = new Label(options.message());
            messageLabel.getStyleClass().add(MESSAGE_STYLE_CLASS);
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(Double.MAX_VALUE);
            body.getChildren().add(messageLabel);
        }

        TextArea details = new TextArea(options.details());
        details.getStyleClass().add(DETAILS_STYLE_CLASS);
        details.setEditable(false);
        // setWrapText(false) is the deliberate default: stack traces are already line-broken and
        // wrapping them mangles the call-site columns. Players who want softer wrapping can
        // resize the window; copyable selection survives either way.
        details.setWrapText(false);
        details.setFocusTraversable(true);
        VBox.setVgrow(details, Priority.ALWAYS);
        body.getChildren().add(details);

        Button copyButton = new Button("Copy details");
        copyButton.getStyleClass().add(COPY_BUTTON_STYLE_CLASS);
        copyButton.setOnAction(event -> copyToClipboard(options.details()));

        HBox actions = new HBox(8);
        actions.getStyleClass().add(ACTIONS_STYLE_CLASS);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getChildren().add(copyButton);

        if (options.continueAvailable()) {
            Button continueButton = new Button("Continue");
            continueButton.getStyleClass().add(CONTINUE_BUTTON_STYLE_CLASS);
            continueButton.setOnAction(event -> options.continueAction().run());
            actions.getChildren().add(continueButton);
        }

        Button exitButton = new Button("Exit");
        exitButton.getStyleClass().add(EXIT_BUTTON_STYLE_CLASS);
        exitButton.setOnAction(event -> options.exitAction().run());
        actions.getChildren().add(exitButton);

        BorderPane root = new BorderPane();
        root.getStyleClass().add(STYLE_CLASS);
        root.setPadding(new Insets(16));
        root.setTop(titleLabel);
        root.setCenter(body);
        root.setBottom(actions);
        BorderPane.setMargin(titleLabel, new Insets(0, 0, 12, 0));
        BorderPane.setMargin(actions, new Insets(12, 0, 0, 0));
        return root;
    }

    /**
     * Returns the supplied throwable's stack trace as a single string — the same content that
     * {@link Throwable#printStackTrace()} would write to {@code System.err}.
     */
    public static String stackTraceText(Throwable error) {
        Validation.requireNonNull(error, "Throwable is required.");
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    /**
     * Copies the supplied text to the system clipboard. Visible-for-testing so headless tests can
     * verify clipboard wiring; production callers normally hit it through the Copy button.
     */
    static void copyToClipboard(String details) {
        ClipboardContent content = new ClipboardContent();
        content.putString(details == null ? "" : details);
        Clipboard.getSystemClipboard().setContent(content);
    }
}
