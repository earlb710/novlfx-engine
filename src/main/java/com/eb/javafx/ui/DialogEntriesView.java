package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.scene.ConversationDefinition.LineType;
import com.eb.javafx.text.DialogHistory;
import com.eb.javafx.text.DialogMessage;
import com.eb.javafx.text.DialogSpeaker;
import com.eb.javafx.util.Validation;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

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
 *       an {@link HBox} with a fixed-width speaker column (so message bodies align across entries)
 *       plus a wrapping body {@link Label}. Shout uppercases its body and applies a bold
 *       declaration via the {@code .dialog-entry-shout} CSS rule; whisper lowercases and italicises
 *       via {@code .dialog-entry-whisper}. When {@link DialogSpeaker#textColor()} is set, that
 *       colour is applied as an inline style to <em>both</em> the speaker label and the message
 *       body so the whole line is tinted (e.g. one colour per role: narrator, MC, book girl,
 *       random girl).</li>
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
 *
 * <p>The view extends {@link ScrollPane} and renders entries into an inner {@link VBox}. A
 * vertical scrollbar appears on the right as soon as the rendered entries exceed the viewport
 * height, so the player can scroll back through previous conversations without having to use the
 * cursor navigation. After every append the view automatically scrolls to the bottom so the
 * newest entry stays in view. Tests and renderers that need the actual entry node list should use
 * {@link #entryNodes()} rather than {@code getChildren()} (which now returns the {@code
 * ScrollPane}'s skin children, not the entry list).</p>
 */
public final class DialogEntriesView extends ScrollPane {
    /** Opacity applied to entries above the current cursor. */
    public static final double PREVIOUS_ENTRY_OPACITY = 0.5;
    public static final String STYLE_CLASS = "dialog-entries-view";
    public static final String ENTRY_STYLE_CLASS = "dialog-entry";
    public static final String CURRENT_ENTRY_STYLE_CLASS = "dialog-entry-current";
    public static final String PREVIOUS_ENTRY_STYLE_CLASS = "dialog-entry-previous";
    public static final String SPEAKER_STYLE_CLASS = "dialog-entry-speaker";
    public static final String BODY_STYLE_CLASS = "dialog-entry-body";
    public static final String SAY_STYLE_CLASS = "dialog-entry-say";
    public static final String SHOUT_STYLE_CLASS = "dialog-entry-shout";
    public static final String WHISPER_STYLE_CLASS = "dialog-entry-whisper";
    public static final String DIVIDER_STYLE_CLASS = "dialog-entry-divider";
    public static final String DIVIDER_LINE_STYLE_CLASS = "dialog-entry-divider-line";
    public static final String DIVIDER_LABEL_STYLE_CLASS = "dialog-entry-divider-label";

    /** Default {@link GameDateTime} used when callers omit a clock; gives the history a valid timestamp. */
    private static final GameDateTime DEFAULT_TIMESTAMP = new GameDateTime(1, "default");
    private static final AtomicInteger AUTO_DIALOG_SEQ = new AtomicInteger();
    private static final double DEFAULT_SPACING = 6;
    /** No cap by default — the {@link ScrollPane} viewport plus the right-hand scrollbar reveal the
     *  full history. Callers can still set a positive cap via {@link #setMaxVisibleEntries(int)} to
     *  trim render work for very long histories. */
    private static final int DEFAULT_MAX_VISIBLE_ENTRIES = Integer.MAX_VALUE;
    /** Default fixed-width speaker column so message bodies line up across entries with different speaker name lengths. */
    public static final double DEFAULT_SPEAKER_COLUMN_WIDTH = 160;

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

    public static final String ENTRIES_CONTAINER_STYLE_CLASS = "dialog-entries-container";

    private final List<Entry> entries = new ArrayList<>();
    private final DialogHistory history;
    private final ReadOnlyBooleanWrapper canGoBack = new ReadOnlyBooleanWrapper(this, "canGoBack", false);
    private final ReadOnlyBooleanWrapper canGoForward = new ReadOnlyBooleanWrapper(this, "canGoForward", false);
    /**
     * Inner column that actually holds rendered entry nodes. Wrapping it in a {@link ScrollPane}
     * (this class) is what produces the right-hand scrollbar so the player can scroll back through
     * previous conversations.
     */
    private final VBox entriesContainer = new VBox(DEFAULT_SPACING);
    private int currentIndex = -1;
    private int maxVisibleEntries = DEFAULT_MAX_VISIBLE_ENTRIES;
    private double speakerColumnWidth = DEFAULT_SPEAKER_COLUMN_WIDTH;
    /**
     * When {@code true} the view renders every entry in the full list (not just up to the cursor)
     * so the player can scroll through the complete conversation record. Toggled by
     * {@link #bindHistoryToggle}.
     */
    private boolean historyMode = false;
    /** Saved ratio used to restore the height binding when leaving history mode. */
    private double savedDialogHeightShare = 0;
    /** Tracks which footer the view is already wired to so {@link #bindToFooter(Node)} is idempotent. */
    private HBox wiredFooter;
    /** Tracks which footer the history toggle is already wired to so {@link #bindHistoryToggle(Node, Node, double)} is idempotent. */
    private HBox wiredHistoryFooter;
    /** Tracks scenes that already have the keyboard shortcut filter installed so installation is idempotent. */
    private final java.util.Set<javafx.scene.Scene> wiredScenes = new java.util.HashSet<>();

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
        entriesContainer.getStyleClass().add(ENTRIES_CONTAINER_STYLE_CLASS);
        entriesContainer.setAlignment(Pos.BOTTOM_LEFT);
        entriesContainer.setMinSize(0, 0);
        entriesContainer.setPickOnBounds(true);
        setContent(entriesContainer);
        setFitToWidth(true);
        // Don't stretch the content vertically — when entries are short the container should sit at
        // its natural height (so the BOTTOM_LEFT alignment pins them to the bottom of the viewport);
        // when entries are long the container grows past the viewport and the vbar appears.
        setFitToHeight(false);
        setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setMinSize(0, 0);
        setPickOnBounds(true);
        addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClick);
        // Auto-install Backspace / Space shortcuts as soon as the view is added to a scene, so
        // applications never have to wire them by hand. The installation is idempotent — a manual
        // {@link #installKeyboardShortcuts(javafx.scene.Scene)} call before the view is attached
        // still works and the auto-listener will skip the already-wired scene.
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                installKeyboardShortcuts(newScene);
            }
        });
        rebuild();
    }

    /**
     * Returns an unmodifiable view of the rendered entry nodes (one per visible entry, in stacking
     * order). Use this in tests and renderers instead of {@link #getChildren()} — the latter now
     * returns the {@link ScrollPane}'s skin children, not the entry list.
     */
    public List<Node> entryNodes() {
        return entriesContainer.getChildrenUnmodifiable();
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

    /**
     * Sets the fixed width of the speaker column in pixels. The column applies to every
     * {@link SpokenEntry} that has a speaker, so message bodies stay aligned across rows even when
     * speaker labels vary in length. Defaults to {@link #DEFAULT_SPEAKER_COLUMN_WIDTH} (160px) — bump
     * it up for apps with longer speaker names, or drop it for compact layouts. Triggers an
     * immediate {@link #rebuild()} so existing entries pick up the new width.
     *
     * @param speakerColumnWidth column width in pixels; must be positive
     */
    public void setSpeakerColumnWidth(double speakerColumnWidth) {
        if (speakerColumnWidth <= 0.0) {
            throw new IllegalArgumentException("speakerColumnWidth must be positive, was: " + speakerColumnWidth);
        }
        this.speakerColumnWidth = speakerColumnWidth;
        rebuild();
    }

    /** Returns the current speaker column width in pixels. */
    public double speakerColumnWidth() {
        return speakerColumnWidth;
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

    /**
     * Ends the currently open conversation with the supplied end timestamp.
     *
     * <p>The widget no longer appends a {@link ConversationEnd} divider entry — only the
     * {@link ConversationStart} divider acts as the visual separator between conversations, since
     * the start of a new conversation already implies that the previous one has ended. The
     * underlying {@link DialogHistory} still records {@code endedAt} so persistence and the
     * conversation-history screens keep their full lifecycle metadata.</p>
     */
    public void endConversation(GameDateTime endedAt) {
        Validation.requireNonNull(endedAt, "Conversation end time is required.");
        if (history.openDialog().isEmpty()) {
            return;
        }
        history.endDialog(endedAt);
        // Intentionally no appendEntry(new ConversationEnd(...)) — see Javadoc above. Callers
        // that still need a closing visual marker can construct one explicitly via
        // dialogEntries() / a custom renderer, but the standard flow no longer produces one.
    }

    // ----- Navigation ---------------------------------------------------------------------------

    public void clear() {
        entries.clear();
        currentIndex = -1;
        rebuild();
    }

    /**
     * Moves the cursor to the previous non-divider entry.
     *
     * <p>Divider entries ({@link ConversationStart} / {@link ConversationEnd}) are auto-skipped
     * so the cursor always lands on a spoken or plain line — the dividers are visual section
     * markers, not "clickable" reading positions. No-op when there is no earlier non-divider
     * entry.</p>
     */
    public void goBack() {
        int target = previousNonDividerIndex(currentIndex);
        if (target >= 0 && target != currentIndex) {
            currentIndex = target;
            rebuild();
        }
    }

    /**
     * Moves the cursor to the next non-divider entry.
     *
     * <p>Divider entries are auto-skipped (see {@link #goBack()}). No-op when there is no later
     * non-divider entry.</p>
     */
    public void goForward() {
        int target = nextNonDividerIndex(currentIndex);
        if (target >= 0 && target != currentIndex) {
            currentIndex = target;
            rebuild();
        }
    }

    private int previousNonDividerIndex(int fromIndex) {
        for (int i = fromIndex - 1; i >= 0; i--) {
            if (!isDivider(entries.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int nextNonDividerIndex(int fromIndex) {
        for (int i = fromIndex + 1; i < entries.size(); i++) {
            if (!isDivider(entries.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isDivider(Entry entry) {
        return entry instanceof ConversationStart || entry instanceof ConversationEnd;
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

    /**
     * Wires the standard {@link ScreenShell#footerBar() footer bar} back / forward labels to drive
     * {@link #goBack()} / {@link #goForward()} on this view.
     *
     * <p>In addition to installing click handlers, this method immediately applies the correct
     * enabled / disabled visual state to the back and forward labels and keeps them in sync with
     * {@link #canGoBackProperty()} / {@link #canGoForwardProperty()} via property listeners.
     * That means the back label greys out automatically when the cursor is at the first entry and
     * the forward label greys out when it is at the last, without any extra wiring by the caller.</p>
     *
     * @param footerOrAncestor the footer {@link HBox} itself, or any ancestor node that contains it
     *        (e.g. the {@link StackPane} root returned by
     *        {@link MainAppLayoutRenderer#render MainAppLayoutRenderer.render(...)})
     */
    public void bindToFooter(Node footerOrAncestor) {
        Validation.requireNonNull(footerOrAncestor, "Footer node is required.");
        HBox footer = findFooter(footerOrAncestor);
        if (footer == null) {
            return;
        }
        if (footer == wiredFooter) {
            // Already wired to this footer (e.g. MainAppLayoutRenderer auto-wired and the
            // application called bindToFooter again, or a test invokes it twice). Skip so we
            // don't stack duplicate listeners on the back / forward labels.
            return;
        }
        wiredFooter = footer;
        for (Node child : footer.getChildren()) {
            if (!(child instanceof Label label)) {
                continue;
            }
            if (!(label.getUserData() instanceof ScreenShell.FooterOption option)) {
                continue;
            }
            switch (option.id()) {
                case "back" -> {
                    applyFooterLabelEnabled(label, canGoBack());
                    canGoBack.addListener((obs, oldVal, newVal) -> applyFooterLabelEnabled(label, newVal));
                    label.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                        if (ScreenShell.isFooterOptionEnabled(label)) {
                            goBack();
                            event.consume();
                        }
                    });
                }
                case "forward" -> {
                    applyFooterLabelEnabled(label, canGoForward());
                    canGoForward.addListener((obs, oldVal, newVal) -> applyFooterLabelEnabled(label, newVal));
                    label.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                        if (ScreenShell.isFooterOptionEnabled(label)) {
                            goForward();
                            event.consume();
                        }
                    });
                }
                default -> {
                    // Other footer labels stay under application control.
                }
            }
        }
    }

    /**
     * Updates a footer label's enabled / disabled visual state and its stored {@link ScreenShell.FooterOption}
     * user data so that {@link ScreenShell#isFooterOptionEnabled(Label)} stays consistent.
     */
    private static void applyFooterLabelEnabled(Label label, boolean enabled) {
        if (label.getUserData() instanceof ScreenShell.FooterOption option) {
            label.setUserData(option.withEnabled(enabled));
        }
        if (enabled) {
            label.getStyleClass().remove(ScreenShell.SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS);
        } else if (!label.getStyleClass().contains(ScreenShell.SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS)) {
            label.getStyleClass().add(ScreenShell.SCREEN_FOOTER_OPTION_DISABLED_STYLE_CLASS);
        }
    }

    public void installKeyboardShortcuts(javafx.scene.Scene scene) {
        Validation.requireNonNull(scene, "Scene is required.");
        if (!wiredScenes.add(scene)) {
            // Already installed on this scene — most likely the sceneProperty listener auto-wired
            // it and the application is also calling this helper directly. Skip so we don't
            // double-handle every key press.
            return;
        }
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

    // ----- History-mode (expanded full-height view) ---------------------------------------------

    /**
     * Returns {@code true} while the view is in history mode.
     *
     * <p>History mode renders every entry (not just up to the cursor) and expands the dialog slot
     * to fill the full layout height so the player can scroll through the complete conversation
     * record. Wire the toggle via {@link #bindHistoryToggle}.</p>
     */
    public boolean isHistoryMode() {
        return historyMode;
    }

    /**
     * Sets history mode directly (without layout-expand side effects). Prefer
     * {@link #bindHistoryToggle} when the view is embedded in a {@code MAIN_APP_LAYOUT} so the
     * layout expansion also fires. This setter is mainly useful for tests.
     */
    public void setHistoryMode(boolean historyMode) {
        this.historyMode = historyMode;
        rebuild();
    }

    /**
     * Wires the standard {@link ScreenShell#footerBar() footer} history (◷) button so that
     * clicking it toggles the dialog block between its normal split-view height and a full-height
     * expanded view that covers the story slot.
     *
     * <p>The expansion works by hiding the story node ({@code setManaged/setVisible false}) so
     * the layout's centre slot collapses, then rebinding this view's height to
     * {@code min(renderedContentHeight, centreHeight)}. Short histories stay compact; long ones
     * fill the viewport. On the second click the story node is restored and the normal
     * proportional height binding is re-applied.</p>
     *
     * <p>In history mode {@link #rebuild()} renders <em>all</em> entries (including any beyond the
     * current cursor) so the player gets a complete scrollable record.</p>
     *
     * @param footerOrAncestor the footer {@link HBox} itself or any ancestor that contains it
     * @param storyNode the story-slot node that will be hidden when history mode is active
     *        (its parent must be the same {@link BorderPane} as this view's parent)
     * @param dialogHeightShare the fraction of the centre pane height allocated to this dialog
     *        in normal mode, e.g. {@code 1.0 - MainAppLayoutPlan.DEFAULT_STORY_DIALOG_RATIO}.
     *        Used to recreate the height binding when history mode is deactivated.
     */
    public void bindHistoryToggle(Node footerOrAncestor, Node storyNode, double dialogHeightShare) {
        Validation.requireNonNull(footerOrAncestor, "Footer ancestor is required.");
        Validation.requireNonNull(storyNode, "Story node is required.");
        this.savedDialogHeightShare = dialogHeightShare;
        HBox footer = findFooter(footerOrAncestor);
        if (footer == null) {
            return;
        }
        if (footer == wiredHistoryFooter) {
            // Already wired to this footer's history button (e.g. MainAppLayoutRenderer auto-wired
            // and the application is also calling this helper directly). Skip so we don't stack
            // duplicate handlers that would toggle the mode twice per click.
            return;
        }
        wiredHistoryFooter = footer;
        for (Node child : footer.getChildren()) {
            if (child instanceof Label label
                    && label.getUserData() instanceof ScreenShell.FooterOption option
                    && "history".equals(option.id())) {
                label.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                    toggleHistoryLayout(storyNode);
                    event.consume();
                });
                break;
            }
        }
    }

    /**
     * Performs the layout toggle when the history button is clicked.
     *
     * <p>The dialog node stays in {@code BorderPane.bottom} throughout — no node-moving is needed.
     * Instead the story slot is hidden/shown and the dialog's height binding is swapped:</p>
     *
     * <p><b>Entering history mode</b></p>
     * <ul>
     *   <li>The story node is hidden ({@code setManaged(false); setVisible(false)}) so the
     *       {@code BorderPane} collapses the center slot and the full centre height is free.</li>
     *   <li>The dialog's height is bound to
     *       {@code min(entriesContainer.height, centre.height)} so it only grows as far as the
     *       rendered content requires, capped at the full available window height. Short histories
     *       stay compact; long ones fill the viewport and let the scrollbar do the rest.</li>
     * </ul>
     *
     * <p><b>Leaving history mode</b></p>
     * <ul>
     *   <li>The story node is restored ({@code setManaged(true); setVisible(true)}).</li>
     *   <li>The height binding is re-applied as
     *       {@code centre.height × savedDialogHeightShare}.</li>
     * </ul>
     *
     * <p>If the view's parent is not a {@link BorderPane} (e.g. in a test without a layout),
     * the mode flag still flips and {@link #rebuild()} still fires; only the layout side-effects
     * are skipped.</p>
     */
    private void toggleHistoryLayout(Node storyNode) {
        historyMode = !historyMode;
        if (!(getParent() instanceof BorderPane centre)) {
            // Not embedded in the expected layout — toggle entry rendering only.
            rebuild();
            return;
        }
        prefHeightProperty().unbind();
        minHeightProperty().unbind();
        maxHeightProperty().unbind();
        if (historyMode) {
            // Collapse the story slot so the full centre height is free for the dialog.
            storyNode.setManaged(false);
            storyNode.setVisible(false);
            // Bind to the smaller of: content's natural height vs full available height.
            // entriesContainer.heightProperty() is live — it updates after the layout pass
            // as entries are rendered, so the dialog expands smoothly to exactly the size
            // it needs rather than always claiming the full window height.
            // createDoubleBinding is used because Bindings.min returns NumberBinding (not
            // DoubleBinding) in JavaFX 17, and prefHeightProperty().bind() requires a
            // DoubleBinding-compatible type.
            DoubleBinding expandedHeight = Bindings.createDoubleBinding(
                    () -> Math.min(entriesContainer.getHeight(), centre.getHeight()),
                    entriesContainer.heightProperty(),
                    centre.heightProperty());
            prefHeightProperty().bind(expandedHeight);
            minHeightProperty().bind(expandedHeight);
            maxHeightProperty().bind(expandedHeight);
        } else {
            // Restore the story slot and reinstate the normal proportional height binding.
            storyNode.setManaged(true);
            storyNode.setVisible(true);
            DoubleBinding normalHeight = centre.heightProperty().multiply(savedDialogHeightShare);
            prefHeightProperty().bind(normalHeight);
            minHeightProperty().bind(normalHeight);
            maxHeightProperty().bind(normalHeight);
        }
        rebuild();
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
        entriesContainer.getChildren().clear();
        if (!entries.isEmpty()) {
            if (historyMode) {
                // History mode: render ALL entries (including any after the cursor) so the player
                // can scroll through the complete conversation record in the expanded viewport.
                // The cursor entry is still highlighted as "current"; everything else is "previous".
                for (int i = 0; i < entries.size(); i++) {
                    entriesContainer.getChildren().add(renderEntry(entries.get(i), i != currentIndex));
                }
            } else if (currentIndex >= 0) {
                int firstVisible = Math.max(0, currentIndex - (maxVisibleEntries - 1));
                for (int i = firstVisible; i < currentIndex; i++) {
                    entriesContainer.getChildren().add(renderEntry(entries.get(i), true));
                }
                entriesContainer.getChildren().add(renderEntry(entries.get(currentIndex), false));
            }
        }
        // Navigation booleans reflect "is there a non-divider entry in that direction" so the
        // footer back/forward affordances stay greyed out when only dividers separate the cursor
        // from the ends — clicking them would otherwise feel like a no-op.
        canGoBack.set(currentIndex >= 0 && previousNonDividerIndex(currentIndex) >= 0);
        canGoForward.set(currentIndex >= 0 && nextNonDividerIndex(currentIndex) >= 0);
        // Defer the scroll-to-bottom until after the JavaFX layout pass. The content VBox changes
        // size when entries are added/removed; calling setVvalue(getVmax()) synchronously would
        // read the stale Vmax from the previous layout, so the pin would fall short. The deferred
        // call fires once the new heights are committed, guaranteeing the bottom entry stays visible
        // after every append and every cursor navigation.
        Platform.runLater(() -> setVvalue(getVmax()));
    }

    private Node renderEntry(Entry entry, boolean previous) {
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

    private HBox renderSpoken(SpokenEntry entry) {
        HBox row = new HBox(8);
        row.getStyleClass().addAll(ENTRY_STYLE_CLASS, lineTypeStyleClass(entry.type()));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setAlignment(Pos.TOP_LEFT);

        String speakerColor = entry.speaker() != null && entry.speaker().hasTextColor()
                ? entry.speaker().textColor()
                : null;

        if (entry.speaker() != null) {
            Label speakerLabel = new Label(entry.speaker().label() + ":");
            speakerLabel.getStyleClass().add(SPEAKER_STYLE_CLASS);
            speakerLabel.setMinWidth(speakerColumnWidth);
            speakerLabel.setPrefWidth(speakerColumnWidth);
            speakerLabel.setMaxWidth(speakerColumnWidth);
            speakerLabel.setAlignment(Pos.TOP_RIGHT);
            applyInlineColor(speakerLabel, speakerColor);
            row.getChildren().add(speakerLabel);
        }

        Label bodyLabel = new Label(entry.formattedBody());
        bodyLabel.getStyleClass().addAll(BODY_STYLE_CLASS, lineTypeStyleClass(entry.type()));
        bodyLabel.setWrapText(true);
        bodyLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bodyLabel, Priority.ALWAYS);
        applyInlineColor(bodyLabel, speakerColor);
        row.getChildren().add(bodyLabel);
        return row;
    }

    private static void applyInlineColor(Label label, String webColor) {
        if (webColor == null) {
            return;
        }
        // Inline style overrides class-based -fx-text-fill so the speaker's color survives the
        // .dialog-entry-current / .dialog-entry-previous default white. An invalid colour is left
        // unparsed — JavaFX CSS will simply ignore an unparseable declaration.
        label.setStyle("-fx-text-fill: " + webColor + ";");
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
