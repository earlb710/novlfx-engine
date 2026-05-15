package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.scene.ConversationDefinition.LineType;
import com.eb.javafx.text.DialogHistory;
import com.eb.javafx.text.DialogMessage;
import com.eb.javafx.text.DialogSpeaker;
import com.eb.javafx.util.Validation;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Reusable dialog entries panel for the {@link ScreenLayoutType#MAIN_APP_LAYOUT} dialog slot.
 *
 * <p>The panel renders a stack of dialog entries: the entry at the current cursor is shown at the
 * bottom of the panel at full opacity, while earlier entries are drawn above it at
 * {@value #PREVIOUS_ENTRY_OPACITY} opacity so the player sees the active line with recent context
 * fading away.</p>
 *
 * <p>Entries are one of four kinds (modelled as the sealed {@link Entry} hierarchy):</p>
 * <ul>
 *   <li>{@link PlainEntry} — a single narration string, rendered as a {@link Label}.</li>
 *   <li>{@link SpokenEntry} — a {@link LineType#SAY say} / {@link LineType#SHOUT shout} /
 *       {@link LineType#WHISPER whisper} line with an optional {@link DialogSpeaker}, rendered as
 *       a {@link TextFlow} so shout shows bold uppercase and whisper shows italic lowercase
 *       inline. The speaker label uses {@link DialogSpeaker#textColor()} when supplied.</li>
 *   <li>{@link ConversationStart} — a divider entry marking the start of a conversation with one
 *       or more participants. Rendered as a {@code ── Conversation: Alice, Bob ──} horizontal
 *       band.</li>
 *   <li>{@link ConversationEnd} — a closing divider marking the end of a conversation.</li>
 * </ul>
 *
 * <p>The widget owns a {@link DialogHistory}. The {@link #say}, {@link #shout}, and
 * {@link #whisper} helpers append a {@code SpokenEntry} <em>and</em> call
 * {@link DialogHistory#addMessage(DialogMessage)} when a conversation is open. The
 * {@link #startConversation} and {@link #endConversation} helpers append the matching divider
 * entries and call {@link DialogHistory#beginDialog} / {@link DialogHistory#endDialog}. Callers
 * that don't care about history can ignore the history side; calling {@link #say} without an open
 * conversation only updates the visible widget.</p>
 *
 * <p>Wire the standard {@link ScreenShell#footerBar() footer bar} back / forward labels to this
 * view with {@link #bindToFooter(Node)} so the footer drives dialog navigation. Use
 * {@link #installKeyboardShortcuts(javafx.scene.Scene)} to additionally enable the footer back /
 * forward shortcuts (Backspace and Space) on a scene.</p>
 *
 * <p>The view itself also handles mouse clicks: a primary (left) click advances with
 * {@link #goForward()} and a secondary (right) click rewinds with {@link #goBack()}. The handler
 * is installed in the constructor so the dialog window is click-driven out of the box.</p>
 */
public final class DialogEntriesView extends VBox {
    /** Opacity applied to entries above the current cursor. */
    public static final double PREVIOUS_ENTRY_OPACITY = 0.5;
    public static final String STYLE_CLASS = "dialog-entries-view";
    public static final String ENTRY_STYLE_CLASS = "dialog-entry";
    public static final String CURRENT_ENTRY_STYLE_CLASS = "dialog-entry-current";
    public static final String PREVIOUS_ENTRY_STYLE_CLASS = "dialog-entry-previous";
    public static final String SPEAKER_STYLE_CLASS = "dialog-entry-speaker";
    public static final String SAY_STYLE_CLASS = "dialog-entry-say";
    public static final String SHOUT_STYLE_CLASS = "dialog-entry-shout";
    public static final String WHISPER_STYLE_CLASS = "dialog-entry-whisper";
    public static final String DIVIDER_STYLE_CLASS = "dialog-entry-divider";
    public static final String DIVIDER_LINE_STYLE_CLASS = "dialog-entry-divider-line";
    public static final String DIVIDER_LABEL_STYLE_CLASS = "dialog-entry-divider-label";

    /** Default {@link GameDateTime} used when callers omit a clock; gives the history a valid timestamp. */
    private static final GameDateTime DEFAULT_TIMESTAMP = new GameDateTime(1, "default");
    private static final AtomicInteger AUTO_DIALOG_SEQ = new AtomicInteger();
    private static final double DEFAULT_SPACING = 4;
    private static final int DEFAULT_MAX_VISIBLE_ENTRIES = 5;

    /** Top-level sealed type for one row in the dialog panel. */
    public sealed interface Entry permits PlainEntry, SpokenEntry, ConversationStart, ConversationEnd {
        /** Plain text representation used by {@link #entries()} so callers can serialize the view. */
        String displayText();
    }

    /** Bare narration string with no speaker or styling. */
    public record PlainEntry(String text) implements Entry {
        public PlainEntry {
            Validation.requireNonNull(text, "Dialog entry text is required.");
        }
        @Override public String displayText() { return text; }
    }

    /** Say / shout / whisper line with optional speaker. */
    public record SpokenEntry(LineType type, DialogSpeaker speaker, String text) implements Entry {
        public SpokenEntry {
            Validation.requireNonNull(type, "Dialog spoken entry type is required.");
            Validation.requireNonNull(text, "Dialog spoken entry text is required.");
        }
        /** Returns the body text transformed per line type (uppercase for shout, lowercase for whisper). */
        public String formattedBody() {
            return switch (type) {
                case SHOUT -> text.toUpperCase(Locale.ROOT);
                case WHISPER -> text.toLowerCase(Locale.ROOT);
                case SAY, CHOICE -> text;
            };
        }
        @Override public String displayText() {
            String body = formattedBody();
            return speaker == null ? body : speaker.label() + ": " + body;
        }
    }

    /** Divider entry inserted at the start of a conversation, listing participants. */
    public record ConversationStart(List<DialogSpeaker> participants, GameDateTime startedAt) implements Entry {
        public ConversationStart {
            participants = List.copyOf(Validation.requireNonNull(participants, "Conversation participants are required."));
        }
        @Override public String displayText() {
            String names = participants.isEmpty()
                    ? "Conversation"
                    : "Conversation: " + participants.stream().map(DialogSpeaker::label).collect(Collectors.joining(", "));
            return startedAt == null ? "── " + names + " ──" : "── " + names + " (" + startedAt + ") ──";
        }
    }

    /** Divider entry inserted when a conversation ends. */
    public record ConversationEnd(GameDateTime endedAt) implements Entry {
        @Override public String displayText() {
            return endedAt == null ? "── End ──" : "── End (" + endedAt + ") ──";
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final DialogHistory history;
    private final ReadOnlyBooleanWrapper canGoBack = new ReadOnlyBooleanWrapper(this, "canGoBack", false);
    private final ReadOnlyBooleanWrapper canGoForward = new ReadOnlyBooleanWrapper(this, "canGoForward", false);
    private int currentIndex = -1;
    private int maxVisibleEntries = DEFAULT_MAX_VISIBLE_ENTRIES;

    public DialogEntriesView() {
        this(new DialogHistory());
    }

    /**
     * @param history history instance the helpers will mirror writes into. Allows callers to share
     *        a history with other systems (save/load, the conversation history screen, …).
     */
    public DialogEntriesView(DialogHistory history) {
        this.history = Validation.requireNonNull(history, "Dialog history is required.");
        getStyleClass().add(STYLE_CLASS);
        setSpacing(DEFAULT_SPACING);
        setAlignment(Pos.BOTTOM_LEFT);
        setMinSize(0, 0);
        setPickOnBounds(true);
        addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClick);
        rebuild();
    }

    private void handleMouseClick(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            if (canGoForward()) {
                goForward();
            }
            event.consume();
        } else if (event.getButton() == MouseButton.SECONDARY) {
            if (canGoBack()) {
                goBack();
            }
            event.consume();
        }
    }

    /** Returns the {@link DialogHistory} the helpers mirror writes into. */
    public DialogHistory history() {
        return history;
    }

    public void setMaxVisibleEntries(int maxVisibleEntries) {
        Validation.requirePositive(maxVisibleEntries, "maxVisibleEntries must be positive.");
        this.maxVisibleEntries = maxVisibleEntries;
        rebuild();
    }

    public int maxVisibleEntries() {
        return maxVisibleEntries;
    }

    // ----- Plain entry API ----------------------------------------------------------------------

    /** Replaces all entries with plain-text narration entries and parks the cursor on the last one. */
    public void setEntries(Collection<String> texts) {
        Validation.requireNonNull(texts, "Dialog entries are required.");
        entries.clear();
        for (String text : texts) {
            entries.add(new PlainEntry(text));
        }
        currentIndex = entries.isEmpty() ? -1 : entries.size() - 1;
        rebuild();
    }

    /** Appends a {@link PlainEntry} and moves the cursor to it (it becomes the newest visible). */
    public void addEntry(String text) {
        appendEntry(new PlainEntry(text));
    }

    // ----- Spoken-entry helpers (the "say / shout / whisper" verbs) -----------------------------

    /** Appends a {@link SpokenEntry} for a {@link LineType#SAY say} line. Mirrors into history if open. */
    public void say(DialogSpeaker speaker, String text) {
        appendSpoken(LineType.SAY, speaker, text);
    }

    /** Convenience overload for narration without an explicit speaker. */
    public void say(String text) {
        say(null, text);
    }

    /** Appends a {@link SpokenEntry} for a {@link LineType#SHOUT shout} line (uppercase + bold). */
    public void shout(DialogSpeaker speaker, String text) {
        appendSpoken(LineType.SHOUT, speaker, text);
    }

    /** Convenience overload for shouting without an explicit speaker. */
    public void shout(String text) {
        shout(null, text);
    }

    /** Appends a {@link SpokenEntry} for a {@link LineType#WHISPER whisper} line (lowercase + italic). */
    public void whisper(DialogSpeaker speaker, String text) {
        appendSpoken(LineType.WHISPER, speaker, text);
    }

    /** Convenience overload for whispering without an explicit speaker. */
    public void whisper(String text) {
        whisper(null, text);
    }

    // ----- Conversation lifecycle ---------------------------------------------------------------

    /**
     * Starts a new conversation with the supplied participants. Appends a {@link ConversationStart}
     * divider and calls {@link DialogHistory#beginDialog(String, GameDateTime)} with an
     * auto-generated id and a default {@link GameDateTime}.
     */
    public void startConversation(DialogSpeaker... participants) {
        startConversation(autoDialogId(), DEFAULT_TIMESTAMP, participants);
    }

    /**
     * Starts a new conversation tied to the supplied {@link GameClock}. Uses the clock's current
     * time for the {@link DialogHistory} entry; passes an auto-generated dialog id.
     */
    public void startConversation(GameClock clock, DialogSpeaker... participants) {
        Validation.requireNonNull(clock, "Game clock is required.");
        startConversation(autoDialogId(), clock.currentTime(), participants);
    }

    /**
     * Starts a new conversation with the supplied id and timestamp. The id is stored on the
     * {@link DialogHistory} entry; the timestamp is also surfaced in the visible divider.
     */
    public void startConversation(String dialogId, GameDateTime startedAt, DialogSpeaker... participants) {
        Validation.requireNonBlank(dialogId, "Dialog id is required.");
        Validation.requireNonNull(startedAt, "Conversation start time is required.");
        Validation.requireNonNull(participants, "Conversation participants are required.");
        List<DialogSpeaker> participantList = Arrays.stream(participants)
                .map(s -> Validation.requireNonNull(s, "Conversation participant is required."))
                .toList();
        history.beginDialog(dialogId, startedAt);
        appendEntry(new ConversationStart(participantList, startedAt));
    }

    /**
     * Ends the currently open conversation. Appends a {@link ConversationEnd} divider and calls
     * {@link DialogHistory#endDialog} with a default timestamp. No-op when no conversation is open.
     */
    public void endConversation() {
        endConversation(DEFAULT_TIMESTAMP);
    }

    /** Ends the currently open conversation using the supplied {@link GameClock} for the timestamp. */
    public void endConversation(GameClock clock) {
        Validation.requireNonNull(clock, "Game clock is required.");
        endConversation(clock.currentTime());
    }

    /** Ends the currently open conversation with the supplied end timestamp. */
    public void endConversation(GameDateTime endedAt) {
        Validation.requireNonNull(endedAt, "Conversation end time is required.");
        if (history.openDialog().isEmpty()) {
            return;
        }
        history.endDialog(endedAt);
        appendEntry(new ConversationEnd(endedAt));
    }

    // ----- Navigation ---------------------------------------------------------------------------

    public void clear() {
        entries.clear();
        currentIndex = -1;
        rebuild();
    }

    public void goBack() {
        if (currentIndex > 0) {
            currentIndex--;
            rebuild();
        }
    }

    public void goForward() {
        if (currentIndex >= 0 && currentIndex < entries.size() - 1) {
            currentIndex++;
            rebuild();
        }
    }

    public int currentIndex() {
        return currentIndex;
    }

    /** Returns the displayable text for each entry (plain text for narration; "speaker: body" for spoken; divider text for conversation markers). */
    public List<String> entries() {
        return entries.stream().map(Entry::displayText).toList();
    }

    /** Returns the raw entries (sealed {@link Entry} values) for callers that need the structured model. */
    public List<Entry> dialogEntries() {
        return List.copyOf(entries);
    }

    public ReadOnlyBooleanProperty canGoBackProperty() {
        return canGoBack.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty canGoForwardProperty() {
        return canGoForward.getReadOnlyProperty();
    }

    public boolean canGoBack() {
        return canGoBack.get();
    }

    public boolean canGoForward() {
        return canGoForward.get();
    }

    // ----- Footer wiring ------------------------------------------------------------------------

    public void bindToFooter(Node footerOrAncestor) {
        Validation.requireNonNull(footerOrAncestor, "Footer node is required.");
        HBox footer = findFooter(footerOrAncestor);
        if (footer == null) {
            return;
        }
        for (Node child : footer.getChildren()) {
            if (!(child instanceof Label label)) {
                continue;
            }
            if (!(label.getUserData() instanceof ScreenShell.FooterOption option)) {
                continue;
            }
            switch (option.id()) {
                case "back" -> label.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                    if (ScreenShell.isFooterOptionEnabled(label)) {
                        goBack();
                        event.consume();
                    }
                });
                case "forward" -> label.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                    if (ScreenShell.isFooterOptionEnabled(label)) {
                        goForward();
                        event.consume();
                    }
                });
                default -> {
                    // Other footer labels stay under application control.
                }
            }
        }
    }

    public void installKeyboardShortcuts(javafx.scene.Scene scene) {
        Validation.requireNonNull(scene, "Scene is required.");
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.BACK_SPACE && !event.isShortcutDown() && !event.isShiftDown() && !event.isAltDown()) {
                if (canGoBack()) {
                    goBack();
                    event.consume();
                }
            } else if (event.getCode() == KeyCode.SPACE && !event.isShortcutDown() && !event.isShiftDown() && !event.isAltDown()) {
                if (canGoForward()) {
                    goForward();
                    event.consume();
                }
            }
        });
    }

    // ----- Internals ----------------------------------------------------------------------------

    private void appendSpoken(LineType type, DialogSpeaker speaker, String text) {
        Validation.requireNonNull(type, "Line type is required.");
        Validation.requireNonNull(text, "Dialog text is required.");
        SpokenEntry entry = new SpokenEntry(type, speaker, text);
        if (history.openDialog().isPresent() && speaker != null) {
            history.addMessage(DialogMessage.speakerMessage(speaker, entry.formattedBody()));
        }
        appendEntry(entry);
    }

    private void appendEntry(Entry entry) {
        entries.add(entry);
        currentIndex = entries.size() - 1;
        rebuild();
    }

    private void rebuild() {
        getChildren().clear();
        if (currentIndex >= 0 && !entries.isEmpty()) {
            int firstVisible = Math.max(0, currentIndex - (maxVisibleEntries - 1));
            for (int i = firstVisible; i < currentIndex; i++) {
                getChildren().add(renderEntry(entries.get(i), true));
            }
            getChildren().add(renderEntry(entries.get(currentIndex), false));
        }
        canGoBack.set(currentIndex > 0);
        canGoForward.set(currentIndex >= 0 && currentIndex < entries.size() - 1);
    }

    private static Node renderEntry(Entry entry, boolean previous) {
        Node node;
        if (entry instanceof PlainEntry plain) {
            node = renderPlain(plain);
        } else if (entry instanceof SpokenEntry spoken) {
            node = renderSpoken(spoken);
        } else if (entry instanceof ConversationStart || entry instanceof ConversationEnd) {
            node = renderDivider(entry.displayText());
        } else {
            throw new IllegalStateException("Unhandled dialog entry type: " + entry.getClass());
        }
        applyFade(node, previous);
        return node;
    }

    private static Label renderPlain(PlainEntry entry) {
        Label label = new Label(entry.text());
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add(ENTRY_STYLE_CLASS);
        return label;
    }

    private static TextFlow renderSpoken(SpokenEntry entry) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().addAll(ENTRY_STYLE_CLASS, lineTypeStyleClass(entry.type()));
        flow.setMaxWidth(Double.MAX_VALUE);
        if (entry.speaker() != null) {
            Text speakerText = new Text(entry.speaker().label() + ": ");
            speakerText.getStyleClass().add(SPEAKER_STYLE_CLASS);
            speakerText.setFont(Font.font(Font.getDefault().getFamily(), FontWeight.BOLD, Font.getDefault().getSize()));
            if (entry.speaker().hasTextColor()) {
                try {
                    speakerText.setFill(Color.web(entry.speaker().textColor()));
                } catch (IllegalArgumentException ignored) {
                    // Unparseable color from authored data — leave the default fill.
                }
            }
            flow.getChildren().add(speakerText);
        }
        Text body = new Text(entry.formattedBody());
        body.getStyleClass().add(lineTypeStyleClass(entry.type()));
        applyLineTypeFont(body, entry.type());
        flow.getChildren().add(body);
        return flow;
    }

    private static HBox renderDivider(String label) {
        Region leftLine = dividerLine();
        Region rightLine = dividerLine();
        Label text = new Label(label);
        text.getStyleClass().add(DIVIDER_LABEL_STYLE_CLASS);
        HBox box = new HBox(8, leftLine, text, rightLine);
        box.getStyleClass().addAll(ENTRY_STYLE_CLASS, DIVIDER_STYLE_CLASS);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(leftLine, Priority.ALWAYS);
        HBox.setHgrow(rightLine, Priority.ALWAYS);
        return box;
    }

    private static Region dividerLine() {
        Region line = new Region();
        line.getStyleClass().add(DIVIDER_LINE_STYLE_CLASS);
        line.setMinHeight(1);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        return line;
    }

    private static void applyLineTypeFont(Text body, LineType type) {
        Font baseFont = Font.getDefault();
        switch (type) {
            case SHOUT -> body.setFont(Font.font(baseFont.getFamily(), FontWeight.BOLD, FontPosture.REGULAR, baseFont.getSize()));
            case WHISPER -> body.setFont(Font.font(baseFont.getFamily(), FontWeight.NORMAL, FontPosture.ITALIC, baseFont.getSize()));
            case SAY, CHOICE -> body.setFont(baseFont);
        }
    }

    private static String lineTypeStyleClass(LineType type) {
        return switch (type) {
            case SAY, CHOICE -> SAY_STYLE_CLASS;
            case SHOUT -> SHOUT_STYLE_CLASS;
            case WHISPER -> WHISPER_STYLE_CLASS;
        };
    }

    private static void applyFade(Node node, boolean previous) {
        if (previous) {
            node.getStyleClass().add(PREVIOUS_ENTRY_STYLE_CLASS);
            node.setOpacity(PREVIOUS_ENTRY_OPACITY);
        } else {
            node.getStyleClass().add(CURRENT_ENTRY_STYLE_CLASS);
        }
    }

    private static String autoDialogId() {
        return "dialog-" + AUTO_DIALOG_SEQ.incrementAndGet();
    }

    private static HBox findFooter(Node node) {
        if (node instanceof HBox hbox
                && hbox.getStyleClass().contains(ScreenShell.SCREEN_FOOTER_BAR_STYLE_CLASS)) {
            return hbox;
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                HBox found = findFooter(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
