package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.scene.ConversationDefinition.LineType;
import com.eb.javafx.text.DialogHistory;
import com.eb.javafx.text.DialogMessage;
import com.eb.javafx.text.DialogSpeaker;
import com.eb.javafx.util.Validation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
import javafx.util.Duration;

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
    /** Style class applied to {@link CommentEntry} rows — small font, italic, dimmed by
     *  default in the stylesheet so movement/action notes read as side commentary
     *  alongside the regular dialog lines. */
    public static final String COMMENT_STYLE_CLASS = "dialog-entry-comment";

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
    public sealed interface Entry permits PlainEntry, SpokenEntry, ConversationStart, ConversationEnd, CommentEntry {
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

    /**
     * Side-commentary entry — typically a brief stage direction such as a character's
     * movement, posture, glance, or environmental note ({@code "She walks to the bar."},
     * {@code "The lights dim."}).  Rendered in a smaller italic font through the
     * {@link #COMMENT_STYLE_CLASS} CSS hook so it visually separates from the regular
     * spoken / plain lines around it.  Use {@link #comment(String)} to append one.
     */
    public record CommentEntry(String text) implements Entry {
        public CommentEntry {
            Validation.requireNonNull(text, "Comment entry text is required.");
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

    /** Divider entry inserted at the start of a conversation, listing participants.
     *  When the participants list is empty the divider drops the "Conversation:" prefix
     *  entirely — hosts can use that to mark a section break (narrator-only block,
     *  game-time transition, etc.) without the participant-list framing. */
    public record ConversationStart(List<DialogSpeaker> participants, GameDateTime startedAt) implements Entry {
        public ConversationStart {
            participants = List.copyOf(Validation.requireNonNull(participants, "Conversation participants are required."));
        }
        @Override public String displayText() {
            if (participants.isEmpty()) {
                return startedAt == null ? "──" : "── (" + startedAt + ") ──";
            }
            String names = "Conversation: "
                    + participants.stream().map(DialogSpeaker::label).collect(Collectors.joining(", "));
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
    /** Floor for the normal-mode visible window — entries before this index are kept in the
     *  list (so history mode still walks them) but hidden from the live dialog block.  Used
     *  by hosts that want to visually "clear" the dialog at a section boundary without
     *  losing the prior conversation log.  Reset to 0 by {@link #clear()} and clamped to a
     *  valid range by {@link #setMinVisibleIndex(int)}. */
    private int minVisibleIndex = 0;
    private double speakerColumnWidth = DEFAULT_SPEAKER_COLUMN_WIDTH;
    /**
     * When {@code true} the view renders every entry in the full list (not just up to the cursor)
     * so the player can scroll through the complete conversation record. Toggled by
     * {@link #bindHistoryToggle}.
     */
    private final ReadOnlyBooleanWrapper historyModeProperty = new ReadOnlyBooleanWrapper(this, "historyMode", false);
    /** True while a conversation is open between {@link #startConversation} and the
     *  matching {@link #endConversation}.  Exposed as a read-only property so host UIs
     *  can bind chrome visibility (e.g. hide the gameplay action menu while the player
     *  is reading dialog).  Reset to {@code false} by {@link #clear()}. */
    private final ReadOnlyBooleanWrapper conversationOpenProperty =
            new ReadOnlyBooleanWrapper(this, "conversationOpen", false);

    /**
     * HUD nodes registered via {@link #addHudNode} that are hidden while history is open and
     * restored when it closes.  The same nodes are also toggled by a middle-mouse-button click
     * (together with the dialog block itself).
     */
    private final List<Node> hudNodes = new ArrayList<>();

    /** Tracks the visibility state driven by a middle-click "clean view" toggle. */
    private boolean cleanViewActive = false;
    /**
     * When {@code true} (default) the view advances/rewinds its own cursor on left/right clicks and
     * Space/Backspace keys. Set to {@code false} when the embedding application drives navigation
     * itself (e.g. AltLife routes back/forward through its scene-flow state machine) so the engine's
     * internal cursor walks don't compete with the host's state changes.
     */
    private boolean internalNavigationEnabled = true;
    /**
     * When {@code true}, history mode only renders entries up to and including the current cursor
     * instead of every entry in the list. Lets hosts that walk the cursor through scene state show a
     * history that matches "the conversation so far," not "everything that will eventually be said."
     */
    private boolean historyClipsAtCursor = false;
    /** Saved ratio used to restore the height binding when leaving history mode. */
    private double savedDialogHeightShare = 0;
    /**
     * Optional binding that produces the "normal" (non-history) target height. Set by
     * {@link #enableAutoFitDialogHeight}; takes precedence over the simple
     * {@code centre.height × savedDialogHeightShare} fallback when leaving history mode.
     */
    private DoubleBinding normalHeightBinding;
    /** Centre region recorded by {@link #enableAutoFitDialogHeight} for height-share math. */
    private Region autoFitCenterRegion;
    /** Resting share of the centre region when auto-fit is enabled and content fits in normal share. */
    private double autoFitNormalShare;
    /** Maximum share of the centre region when auto-fit expands to fit a tall current entry. */
    private double autoFitMaxShare;
    /**
     * Tracks the current entry node so the auto-fit binding can react to JUST that node's measured
     * height — not the whole {@code entriesContainer.height}, which sums every previously-faded
     * entry above it and made the dialog expand to nearly twice the size actually needed.
     */
    private javafx.beans.property.DoubleProperty currentEntryHeight = new javafx.beans.property.SimpleDoubleProperty(this, "currentEntryHeight", 0);
    private Node trackedCurrentEntry;
    private javafx.beans.value.ChangeListener<javafx.geometry.Bounds> trackedCurrentEntryListener;
    /** Tracks which footer the view is already wired to so {@link #bindToFooter(Node)} is idempotent. */
    private HBox wiredFooter;
    /** Tracks which footer the history toggle is already wired to so {@link #bindHistoryToggle(Node, Node, double)} is idempotent. */
    private HBox wiredHistoryFooter;
    /** Tracks scenes that already have the keyboard shortcut filter installed so installation is idempotent. */
    private final java.util.Set<javafx.scene.Scene> wiredScenes = new java.util.HashSet<>();
    /**
     * Sentinel value representing the dialog widget itself as a hover source — distinguishes
     * self-hover from companion-node hover in {@link #activeHoverSources} so each source can be
     * toggled independently.
     */
    private static final Object SELF_HOVER_SOURCE = new Object();
    /**
     * Set of hover sources currently reporting "the pointer is over me". The set always contains
     * either zero entries (no hover anywhere) or one-plus entries (at least one source is hovered).
     * The effective hover state is {@code !activeHoverSources.isEmpty()}; the fade only drops back
     * to "unhovered" once every source has reported exit.
     */
    private final java.util.Set<Object> activeHoverSources = new java.util.HashSet<>();
    /**
     * Duration of the animated scroll-to-bottom that fires after every {@link #rebuild()}. Defaults
     * to {@link Duration#ZERO} so the scroll snaps immediately — keeping the legacy "drain two
     * pulses and assert vvalue == vmax" expectation working for existing consumers and tests.
     * Callers that want an eased scroll (e.g. a gameplay dialog block) opt in via
     * {@link #setScrollAnimationDuration(Duration)}.
     */
    private Duration scrollAnimationDuration = Duration.ZERO;
    /** Active scroll animation; replaced (and cancelled) every time a new rebuild fires. */
    private Timeline activeScrollAnimation;
    /**
     * Content height (entries container's local bounds height) captured at the end of the previous
     * scroll-to-bottom. Used to detect "content has grown" between rebuilds so we can push the
     * scroll position back before animating — otherwise {@code vvalue} is already pinned at
     * {@code vmax} after the last rebuild and animating to {@code vmax} produces no visible motion.
     */
    private double lastKnownContentHeight = -1;
    /**
     * Whether the dialog block should fade out when the pointer leaves its bounds. Disabled by
     * default so the existing engine tests and consumers keep their fully-opaque widget; gameplay
     * shells opt in via {@link #setHoverFadeEnabled(boolean)} to keep the background art visible
     * between reads.
     */
    private boolean hoverFadeEnabled = false;
    /** Opacity applied to this view when the pointer is inside the widget. Defaults to fully opaque. */
    private double hoverFadeHoveredOpacity = 1.0;
    /**
     * Opacity applied to this view when the pointer is outside the widget. Defaults to {@code 0.2}
     * — i.e. "80% more transparent than fully opaque" — to keep the background art legible while
     * still hinting at the dialog block's presence.
     */
    private double hoverFadeUnhoveredOpacity = 0.2;

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
        // Stretch the content vertically to fill the viewport so the {@code BOTTOM_LEFT}
        // alignment on {@link #entriesContainer} actually pins children to the bottom of
        // the visible area (a {@code false} setting here would let the VBox shrink to its
        // natural height and the alignment would only apply within that shrunken box —
        // children would render top-aligned in the viewport, defeating the chat-style
        // bottom-pin).  When entries grow past the viewport, the VBox overflows and the
        // vbar appears as usual.
        setFitToHeight(true);
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
        // Hover fade: when the pointer leaves the widget the dialog block fades to the configured
        // unhovered opacity, and fades back to full opacity when it re-enters. Disabled by default
        // (see {@link #hoverFadeEnabled}) so the change is opt-in for shells that want it.
        //
        // Wire both {@link Node#hoverProperty() hoverProperty} (cheap binding that fires on the
        // pulse after the actual enter/exit) and explicit MOUSE_ENTERED / MOUSE_EXITED handlers
        // (fire on the same tick as the event) as a safety net — some nested layouts can suppress
        // hoverProperty change events for the parent until layout settles, which made the fade
        // feel like it wasn't reacting at all. The non-{@code _TARGET} variants only fire when the
        // pointer actually crosses the widget's outer bounds, so moving the mouse between entries
        // inside the dialog does NOT toggle the fade on and off.
        //
        // Each enter/exit pair routes through {@link #updateHoverSource} so companion nodes
        // registered via {@link #addHoverCompanion(Node)} can also keep the fade lifted while the
        // pointer is over them (used by gameplay shells to extend hover-on to the footer bar).
        hoverProperty().addListener((obs, was, hovering) -> updateHoverSource(SELF_HOVER_SOURCE, hovering));
        addEventHandler(MouseEvent.MOUSE_ENTERED, event -> updateHoverSource(SELF_HOVER_SOURCE, true));
        addEventHandler(MouseEvent.MOUSE_EXITED, event -> updateHoverSource(SELF_HOVER_SOURCE, false));
        rebuild();
    }

    /**
     * Adds {@code companion} as an additional hover source — while the pointer is inside
     * {@code companion}'s bounds (or any of its descendants, via {@code hoverProperty}), the
     * dialog block treats itself as hovered too. This lets gameplay shells extend the hover-fade
     * "lifted" state to nearby UI (typically the footer bar) so players can read the dialog
     * clearly while clicking back/forward/auto-skip controls that sit outside the widget itself.
     *
     * <p>Idempotent registration isn't enforced — handing the same node to this method twice
     * registers two listeners. Wire it once during layout assembly.</p>
     */
    public void addHoverCompanion(Node companion) {
        Validation.requireNonNull(companion, "Hover companion is required.");
        Object source = companion;
        companion.hoverProperty().addListener((obs, was, hovering) -> updateHoverSource(source, hovering));
        companion.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> updateHoverSource(source, true));
        companion.addEventHandler(MouseEvent.MOUSE_EXITED, event -> updateHoverSource(source, false));
    }

    /**
     * Records a hover transition for {@code source} (the widget itself or a registered companion)
     * and recomputes the effective hover state. The fade stays on as long as ANY source reports
     * "hovered" — it only drops to the unhovered opacity once every source has reported exit.
     */
    private void updateHoverSource(Object source, boolean hovering) {
        if (hovering) {
            activeHoverSources.add(source);
        } else {
            activeHoverSources.remove(source);
        }
        applyHoverFade(!activeHoverSources.isEmpty());
    }

    /**
     * Returns an unmodifiable view of the rendered entry nodes (one per visible entry, in stacking
     * order). Use this in tests and renderers instead of {@link #getChildren()} — the latter now
     * returns the {@link ScrollPane}'s skin children, not the entry list.
     */
    public List<Node> entryNodes() {
        return entriesContainer.getChildrenUnmodifiable();
    }

    /** Toggles "clean view": hide the dialog block and all registered HUD nodes so the
     *  player can see the background art unobstructed.  A second call restores them.
     *  Exposed publicly so hosts can wire scene-wide middle-click handlers that route
     *  through here instead of duplicating the logic. */
    public void toggleCleanView() {
        cleanViewActive = !cleanViewActive;
        setVisible(!cleanViewActive);
        setManaged(!cleanViewActive);
        setHudNodesVisible(!cleanViewActive);
    }

    /** True while the dialog widget + HUD nodes are hidden via {@link #toggleCleanView}. */
    public boolean isCleanViewActive() {
        return cleanViewActive;
    }

    private void handleMouseClick(MouseEvent event) {
        if (event.getButton() == MouseButton.MIDDLE) {
            toggleCleanView();
            event.consume();
            return;
        }
        if (!internalNavigationEnabled) {
            return;
        }
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

    /** Appends a {@link CommentEntry} — a small italic side-commentary line such as a
     *  movement or stage direction ({@code "She walks to the bar."}).  Renders through the
     *  {@link #COMMENT_STYLE_CLASS} style hook so the styling is theme-driven, and
     *  advances the cursor to the new row like any other appendEntry call. */
    public void comment(String text) {
        appendEntry(new CommentEntry(text));
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
        conversationOpenProperty.set(true);
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
        conversationOpenProperty.set(false);
        // Intentionally no appendEntry(new ConversationEnd(...)) — see Javadoc above. Callers
        // that still need a closing visual marker can construct one explicitly via
        // dialogEntries() / a custom renderer, but the standard flow no longer produces one.
    }

    // ----- Navigation ---------------------------------------------------------------------------

    public void clear() {
        entries.clear();
        currentIndex = -1;
        minVisibleIndex = 0;
        conversationOpenProperty.set(false);
        rebuild();
    }

    /** Removes the most-recently-appended entry, if any.  The cursor and
     *  {@link #minVisibleIndex} are adjusted so they stay valid (cursor backs up to the
     *  new last entry; the floor is clamped to the new entries-size).  No-op when the
     *  entries list is empty.  Use this to collapse rapid-fire same-type appends — e.g.
     *  successive "go to:" room-change comments — into a single visible row. */
    public void removeLastEntry() {
        if (entries.isEmpty()) {
            return;
        }
        entries.remove(entries.size() - 1);
        if (currentIndex >= entries.size()) {
            currentIndex = entries.size() - 1;
        }
        if (minVisibleIndex > entries.size()) {
            minVisibleIndex = entries.size();
        }
        rebuild();
    }

    /** Prepends a snapshot of entries (e.g. taken from a previous scene's
     *  {@link #dialogEntries()}) to the start of the entries list so the history view
     *  walks the full transcript across scene transitions.  The cursor + visible-floor
     *  bump by the prepended count so the live view continues to show the same logical
     *  entry it was on before the prepend.  No-op for null or empty input. */
    public void prependEntries(java.util.List<Entry> toPrepend) {
        if (toPrepend == null || toPrepend.isEmpty()) {
            return;
        }
        entries.addAll(0, toPrepend);
        int shift = toPrepend.size();
        if (currentIndex >= 0) {
            currentIndex += shift;
        }
        minVisibleIndex += shift;
        rebuild();
    }

    /** Read-only "is a conversation currently open" property — true between
     *  {@link #startConversation} and {@link #endConversation}.  Bindable so host UIs
     *  can react to it (e.g. hide an action menu while the player is reading dialog). */
    public javafx.beans.property.ReadOnlyBooleanProperty conversationOpenProperty() {
        return conversationOpenProperty.getReadOnlyProperty();
    }

    /** Convenience boolean for {@link #conversationOpenProperty()}. */
    public boolean isConversationOpen() {
        return conversationOpenProperty.get();
    }

    /**
     * Hides every entry before {@code index} from the live dialog block (normal mode) while
     * leaving them in {@link #dialogEntries()} so {@link #isHistoryMode() history mode} still
     * renders them.  Use this at section boundaries (e.g. when the player walks into a new
     * room) to "clear" the visible block without dropping the conversation log.
     *
     * <p>Clamped to {@code [0, entries.size()]}.  Calling with {@code 0} restores the default
     * sliding-window behaviour.  Reset to {@code 0} by {@link #clear()}.</p>
     */
    public void setMinVisibleIndex(int index) {
        this.minVisibleIndex = Math.max(0, Math.min(index, entries.size()));
        rebuild();
    }

    /** @see #setMinVisibleIndex(int) */
    public int minVisibleIndex() {
        return minVisibleIndex;
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

    /**
     * Enables or disables the view's built-in mouse and keyboard navigation.
     *
     * <p>When {@code true} (default) a primary click / Space advances the cursor and a secondary
     * click / Backspace rewinds it. Set to {@code false} when the embedding application drives
     * back/forward through its own state (e.g. a scene-flow engine), so input events fall through to
     * the host instead of being intercepted by the dialog widget.</p>
     */
    public void setInternalNavigationEnabled(boolean enabled) {
        this.internalNavigationEnabled = enabled;
    }

    /** @see #setInternalNavigationEnabled(boolean) */
    public boolean isInternalNavigationEnabled() {
        return internalNavigationEnabled;
    }

    /**
     * Controls whether {@link #isHistoryMode() history mode} renders every entry in the list, or
     * only the entries up to and including the current cursor.
     *
     * <p>Default is {@code false} — history shows the full conversation record. Set to {@code true}
     * for hosts where the cursor reflects "the player's reading position": the expanded history will
     * stop at the current line so it never reveals dialog the player hasn't reached yet.</p>
     */
    public void setHistoryClipsAtCursor(boolean historyClipsAtCursor) {
        this.historyClipsAtCursor = historyClipsAtCursor;
        if (historyModeProperty.get()) {
            rebuild();
        }
    }

    /** @see #setHistoryClipsAtCursor(boolean) */
    public boolean isHistoryClipsAtCursor() {
        return historyClipsAtCursor;
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
            if (!internalNavigationEnabled) {
                return;
            }
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
        return historyModeProperty.get();
    }

    /**
     * Observable for history mode. Fires whenever the view enters or leaves history mode so
     * host applications can react (e.g. hide a HUD overlay while history is visible).
     */
    public ReadOnlyBooleanProperty historyModeProperty() {
        return historyModeProperty.getReadOnlyProperty();
    }

    /**
     * Registers a HUD node that should be hidden while history is open and restored when it
     * closes. The same node is also hidden by a middle-mouse-button "clean view" toggle.
     * Must be called after {@link #bindHistoryToggle} so the node list is stable at toggle time.
     */
    public void addHudNode(Node node) {
        if (node != null && !hudNodes.contains(node)) {
            hudNodes.add(node);
        }
    }

    /**
     * Sets history mode directly (without layout-expand side effects). Prefer
     * {@link #bindHistoryToggle} when the view is embedded in a {@code MAIN_APP_LAYOUT} so the
     * layout expansion also fires. This setter is mainly useful for tests.
     */
    public void setHistoryMode(boolean value) {
        historyModeProperty.set(value);
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
    /**
     * Enables content-driven height for the dialog so a single long entry temporarily expands the
     * dialog block, then contracts again when the next (shorter) entry becomes current.
     *
     * <p>Once called, the dialog's {@code prefHeight}/{@code minHeight}/{@code maxHeight} are
     * bound to {@code clamp(contentHeight, centerRegion.height × normalShare, centerRegion.height
     * × maxShare)} — short messages keep the dialog at its resting share, long paragraphs grow it
     * up to the cap, and the very next rebuild snaps it back when content shrinks. {@code maxShare}
     * guarantees the surrounding layout (e.g. a story area) always retains at least
     * {@code 1 - maxShare} of {@code centerRegion}.</p>
     *
     * <p>Takes precedence over any external height binding installed by a layout renderer
     * (e.g. {@code MainAppLayoutRenderer}'s pinned-share binding) — those constraints are unbound.
     * Coordinates with {@link #bindHistoryToggle}: while history mode is active, the
     * full-centre-height binding wins; on history exit, the auto-fit binding is restored instead
     * of the simple proportional fallback.</p>
     *
     * @param centerRegion the centre region whose height drives the share calculation (typically
     *        the dialog's parent in a {@code MAIN_APP_LAYOUT} centre BorderPane)
     * @param normalShare resting share of {@code centerRegion}'s height, e.g. {@code 0.20}
     * @param maxShare maximum share when expanded for a tall current entry, e.g. {@code 0.60};
     *        must be {@code >= normalShare} and {@code <= 1.0}
     */
    public void enableAutoFitDialogHeight(Region centerRegion, double normalShare, double maxShare) {
        Validation.requireNonNull(centerRegion, "Centre region is required.");
        Validation.requireBetween(normalShare, 0.0, 1.0,
                "Auto-fit normalShare must be between 0.0 and 1.0.");
        Validation.requireBetween(maxShare, 0.0, 1.0,
                "Auto-fit maxShare must be between 0.0 and 1.0.");
        if (maxShare < normalShare) {
            throw new IllegalArgumentException(
                    "Auto-fit maxShare (" + maxShare + ") must be >= normalShare (" + normalShare + ").");
        }
        // Idempotent: skip if already wired with the same parameters. Lets hosts call this
        // manually after MainAppLayoutRenderer auto-wires it without double-binding.
        if (autoFitCenterRegion == centerRegion
                && autoFitNormalShare == normalShare
                && autoFitMaxShare == maxShare
                && normalHeightBinding != null) {
            return;
        }
        this.autoFitCenterRegion = centerRegion;
        this.autoFitNormalShare = normalShare;
        this.autoFitMaxShare = maxShare;
        // Bind to the CURRENT entry's height alone — NOT the total entriesContainer height. The
        // container sums every faded previous entry stacked above the cursor, which previously made
        // the dialog grow to nearly twice the size the active line actually needed (especially in
        // hosts like AltLife that accumulate every step in stepHistory). Tracking just the current
        // entry plus the surrounding chrome (container + dialog chrome) sizes the dialog to fit
        // exactly the active message, letting previous entries scroll off the top.
        //
        // Measurement: we use Node.prefHeight(contentWidth) — the canonical "how tall does this
        // node need to be at this width" — instead of boundsInLocal.height, which lags one layout
        // pulse and historically underestimated by up to ~10% on multi-line wrapped Labels.
        //
        // Chrome: viewportBounds gives the live (dialog height - all ScrollPane overhead) figure,
        // which captures padding + border + any skin-side reservations getInsets() can miss. We
        // then add the entriesContainer's own vertical padding (the gap between the VBox edge
        // and its content).
        //
        // createDoubleBinding because Bindings.min/max return NumberBinding in JavaFX 17.
        DoubleBinding fitBinding = Bindings.createDoubleBinding(
                () -> {
                    double centreH = centerRegion.getHeight();
                    double base = centreH * normalShare;
                    double cap = centreH * maxShare;
                    Node trackedEntry = trackedCurrentEntry;
                    if (trackedEntry == null) {
                        return base;
                    }
                    double containerWidth = entriesContainer.getWidth();
                    double containerHPad = entriesContainer.getInsets().getLeft()
                            + entriesContainer.getInsets().getRight();
                    double contentWidth = containerWidth - containerHPad;
                    if (contentWidth <= 0) {
                        // Pre-first-layout — fall back to the cached bounds height + a generous
                        // chrome estimate so the dialog isn't collapsed on the first frame. The
                        // viewport / width listeners will trigger a recompute as soon as layout
                        // commits real measurements.
                        double cachedBoundsH = currentEntryHeight.get();
                        if (cachedBoundsH <= 0) {
                            return base;
                        }
                        double fallbackPad = entriesContainer.getInsets().getTop()
                                + entriesContainer.getInsets().getBottom()
                                + getInsets().getTop() + getInsets().getBottom();
                        return Math.min(Math.max(base, cachedBoundsH + fallbackPad), cap);
                    }
                    double currentPrefH = trackedEntry.prefHeight(contentWidth);
                    double containerVPad = entriesContainer.getInsets().getTop()
                            + entriesContainer.getInsets().getBottom();
                    // Measure the actual ScrollPane chrome from runtime layout: dialog total minus
                    // viewport. Stable for a given ScrollPane config (= padding + border) and
                    // doesn't undercount the way summing only getInsets() can.
                    double dialogChrome;
                    javafx.geometry.Bounds viewport = getViewportBounds();
                    if (viewport != null && getHeight() > viewport.getHeight()) {
                        dialogChrome = getHeight() - viewport.getHeight();
                    } else {
                        dialogChrome = getInsets().getTop() + getInsets().getBottom();
                    }
                    return Math.min(Math.max(base, currentPrefH + containerVPad + dialogChrome), cap);
                },
                currentEntryHeight,
                centerRegion.heightProperty(),
                entriesContainer.insetsProperty(),
                entriesContainer.widthProperty(),
                viewportBoundsProperty(),
                insetsProperty());
        this.normalHeightBinding = fitBinding;
        if (!historyModeProperty.get()) {
            applyHeightBinding(fitBinding);
        }
        // Pick up whatever the most recent rebuild left in the container so the binding has a real
        // height to clamp against without waiting for the next rebuild.
        refreshAutoFitTracker();
    }

    /**
     * Re-attaches the current-entry height listener after a rebuild. The "current entry" is the
     * last non-divider child of the entriesContainer; in normal mode that's the cursor entry; in
     * history mode it's the cursor entry too (still rendered with the current-entry style). When
     * found, its {@code boundsInLocalProperty} drives {@link #currentEntryHeight}, which the
     * auto-fit binding reads.
     */
    private void refreshAutoFitTracker() {
        if (autoFitCenterRegion == null) {
            return;
        }
        if (trackedCurrentEntry != null && trackedCurrentEntryListener != null) {
            trackedCurrentEntry.boundsInLocalProperty().removeListener(trackedCurrentEntryListener);
            trackedCurrentEntry = null;
        }
        Node target = null;
        for (int i = entriesContainer.getChildren().size() - 1; i >= 0; i--) {
            Node child = entriesContainer.getChildren().get(i);
            // Skip the conversation-start / -end divider rows so we measure a real spoken or plain
            // entry — the divider has a different shape and isn't what the cursor lands on.
            if (!child.getStyleClass().contains(DIVIDER_STYLE_CLASS)) {
                target = child;
                break;
            }
        }
        if (target == null) {
            currentEntryHeight.set(0);
            return;
        }
        trackedCurrentEntry = target;
        trackedCurrentEntryListener = (obs, oldB, newB) ->
                currentEntryHeight.set(newB.getHeight());
        target.boundsInLocalProperty().addListener(trackedCurrentEntryListener);
        currentEntryHeight.set(target.getBoundsInLocal().getHeight());
    }

    /** Unbinds the three height properties and re-binds them all to {@code binding}. */
    private void applyHeightBinding(DoubleBinding binding) {
        prefHeightProperty().unbind();
        minHeightProperty().unbind();
        maxHeightProperty().unbind();
        prefHeightProperty().bind(binding);
        minHeightProperty().bind(binding);
        maxHeightProperty().bind(binding);
    }

    private void toggleHistoryLayout(Node storyNode) {
        boolean entering = !historyModeProperty.get();
        historyModeProperty.set(entering);
        if (!(getParent() instanceof BorderPane centre)) {
            // Not embedded in the expected layout — toggle entry rendering only.
            setHudNodesVisible(!entering);
            rebuild();
            return;
        }
        prefHeightProperty().unbind();
        minHeightProperty().unbind();
        maxHeightProperty().unbind();
        if (entering) {
            // Collapse the story slot so the full centre height is free for the dialog.
            storyNode.setManaged(false);
            storyNode.setVisible(false);
            // Hide HUD overlays — they are meaningless while history fills the screen.
            setHudNodesVisible(false);
            // Bind directly to the centre height so the dialog always fills the full available
            // space in history mode. Earlier revisions used min(content.height, centre.height) to
            // let short histories stay compact, but that created a layout feedback loop: the dialog
            // height drove the ScrollPane width (the vbar appears/disappears), the new width
            // re-wrapped Labels and changed the content's natural height, which fed back through
            // the binding — the dialog visibly oscillated between two heights. Pinning to centre
            // height eliminates the loop and matches the "history covers everything" UX.
            prefHeightProperty().bind(centre.heightProperty());
            minHeightProperty().bind(centre.heightProperty());
            maxHeightProperty().bind(centre.heightProperty());
        } else {
            // Restore the story slot and reinstate the normal height binding. If auto-fit was
            // enabled, restore its content-clamped binding instead of the simple proportional
            // share — otherwise leaving history mode would clobber the auto-fit and pin the
            // dialog at a fixed share again.
            storyNode.setManaged(true);
            storyNode.setVisible(true);
            // Restore HUD overlays now that history mode is closed.
            setHudNodesVisible(true);
            DoubleBinding normalHeight = normalHeightBinding != null
                    ? normalHeightBinding
                    : centre.heightProperty().multiply(savedDialogHeightShare);
            prefHeightProperty().bind(normalHeight);
            minHeightProperty().bind(normalHeight);
            maxHeightProperty().bind(normalHeight);
        }
        rebuild();
    }

    private void setHudNodesVisible(boolean visible) {
        for (Node node : hudNodes) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
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
            if (historyModeProperty.get()) {
                // History mode: by default render ALL entries (including any after the cursor) so
                // the player can scroll through the complete conversation record. When
                // historyClipsAtCursor is set, stop at the cursor — the expanded history then
                // matches "the conversation so far," never revealing dialog the player hasn't
                // reached. The cursor entry is still highlighted as "current".
                int upperBound = historyClipsAtCursor && currentIndex >= 0
                        ? currentIndex + 1
                        : entries.size();
                for (int i = 0; i < upperBound; i++) {
                    entriesContainer.getChildren().add(renderEntry(entries.get(i), i != currentIndex));
                }
            } else if (currentIndex >= 0) {
                // minVisibleIndex acts as a host-controlled floor — entries before it stay in
                // the list (so history mode still walks them) but are hidden from the live
                // view.  Default 0, so existing callers see the original sliding-window
                // behaviour unchanged.
                int floor = Math.max(0, Math.min(minVisibleIndex, currentIndex));
                int firstVisible = Math.max(floor, currentIndex - (maxVisibleEntries - 1));
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
        // Re-bind the auto-fit tracker to the new current-entry node (the last non-divider child
        // of entriesContainer). Cheap no-op when auto-fit is disabled.
        refreshAutoFitTracker();
        // Defer the scroll-to-bottom across TWO JavaFX pulses. The content VBox changes size when
        // entries are added/removed; reading getVmax() on the first deferred tick still sees the old
        // layout — the pulse hasn't committed the new heights yet — so the pin falls short and the
        // newest entry shows only half-visible until the user scrolls by hand. The first runLater
        // lets the pulse process the children change and run layout; the nested runLater fires on
        // the following tick, after the ScrollPane viewport has caught up to the new content
        // height, so vvalue actually reaches the bottom of the newest entry. Cheap to chain —
        // runLater itself only enqueues a Runnable. Inside the inner pulse we either snap (when
        // scrollAnimationDuration is zero) or animate vvalue with an ease-out interpolator over
        // {@link #scrollAnimationDuration} so the reader's eye can follow the dialog dropping down
        // instead of jumping. Any previously-active animation is cancelled before the new one fires
        // so rapid forward clicks don't queue overlapping tweens.
        Platform.runLater(() -> Platform.runLater(this::scrollToBottomDeferred));
    }

    /**
     * Snaps or animates the {@code vvalue} to the bottom of the content (the cursor entry).
     * Cancels any previously-active scroll tween so rapid rebuilds don't stack animations.
     *
     * <p>When content has grown since the previous deferred scroll (the common case — each new
     * entry added by {@link #appendEntry} extends the container), this method pushes {@code vvalue}
     * back to a position that shows the <em>previous</em> bottom of the content before animating
     * to the current bottom. Without that push the {@code vvalue} would already be at
     * {@link #getVmax() vmax} from the previous rebuild and the animation from
     * {@code vmax → vmax} would produce no visible motion — the new entry would just pop into
     * view. The push amount is derived from the height delta between rebuilds so the animated
     * scroll covers exactly the new entry's height, regardless of how tall the entry happens to
     * be.</p>
     */
    private void scrollToBottomDeferred() {
        double target = getVmax();
        if (activeScrollAnimation != null) {
            activeScrollAnimation.stop();
            activeScrollAnimation = null;
        }
        double currentContentHeight = entriesContainer.getBoundsInLocal().getHeight();
        double viewportHeight = getViewportBounds() != null ? getViewportBounds().getHeight() : 0;
        if (scrollAnimationDuration == null || scrollAnimationDuration.lessThanOrEqualTo(Duration.ZERO)) {
            setVvalue(target);
            lastKnownContentHeight = currentContentHeight;
            return;
        }
        double current = getVvalue();
        boolean alreadyAtTarget = Math.abs(target - current) < 1e-6;
        if (alreadyAtTarget && lastKnownContentHeight > 0 && currentContentHeight > lastKnownContentHeight) {
            // Content grew since last rebuild and we're already pinned to the bottom. Push the
            // scroll back to where the previous bottom would now sit so the Timeline has visible
            // distance to cover. scrollable = content - viewport; the previous bottom is
            // `delta` pixels above the new bottom, which maps to (1 - delta/scrollable) in vvalue.
            double scrollable = Math.max(1.0, currentContentHeight - viewportHeight);
            double delta = currentContentHeight - lastKnownContentHeight;
            double startVvalue = Math.max(0.0, target - delta / scrollable);
            setVvalue(startVvalue);
            current = startVvalue;
            alreadyAtTarget = Math.abs(target - current) < 1e-6;
        }
        lastKnownContentHeight = currentContentHeight;
        if (alreadyAtTarget) {
            setVvalue(target);
            return;
        }
        Timeline timeline = new Timeline(new KeyFrame(
                scrollAnimationDuration,
                new KeyValue(vvalueProperty(), target, Interpolator.EASE_OUT)));
        timeline.setOnFinished(event -> {
            if (activeScrollAnimation == timeline) {
                activeScrollAnimation = null;
            }
        });
        activeScrollAnimation = timeline;
        timeline.play();
    }

    /**
     * Sets the duration of the animated scroll-to-bottom that fires after every {@link #rebuild()}.
     * Pass {@link Duration#ZERO} to disable the animation and snap immediately; the default is
     * 500&nbsp;ms with an ease-out curve. Applies to subsequent rebuilds — any animation already in
     * flight runs to completion at its original duration.
     */
    public void setScrollAnimationDuration(Duration scrollAnimationDuration) {
        this.scrollAnimationDuration = scrollAnimationDuration == null ? Duration.ZERO : scrollAnimationDuration;
    }

    /** Returns the current scroll-to-bottom animation duration. */
    public Duration scrollAnimationDuration() {
        return scrollAnimationDuration;
    }

    /**
     * Toggles the hover-fade behaviour. When enabled, the dialog block snaps to
     * {@link #setHoverFadeOpacities(double, double) hover-fade unhovered opacity} whenever the
     * pointer leaves its bounds and back to the hovered opacity when it re-enters.
     *
     * <p>The change is applied instantly (no animation) — running a fade {@link Timeline} on
     * {@code opacityProperty()} alongside the scroll Timeline on {@code vvalueProperty()} produced
     * subtle interference that suppressed the smooth-scroll animation in the rendered output.</p>
     *
     * <p>Switching this off restores the view to fully-opaque {@code opacity = 1.0}; switching it
     * on immediately snaps the view to the appropriate opacity for the current hover state.</p>
     */
    public void setHoverFadeEnabled(boolean hoverFadeEnabled) {
        this.hoverFadeEnabled = hoverFadeEnabled;
        if (!hoverFadeEnabled) {
            setOpacity(1.0);
            return;
        }
        // Read the combined hover state from the tracked sources rather than {@link #isHover()}
        // alone so a companion that's already hovered when fade is switched on doesn't get
        // overridden into an unhovered fade.
        applyHoverFade(!activeHoverSources.isEmpty());
    }

    /** Returns whether the hover fade is currently enabled. */
    public boolean hoverFadeEnabled() {
        return hoverFadeEnabled;
    }

    /**
     * Sets the opacities used by the hover fade. {@code hoveredOpacity} is applied when the
     * pointer is inside the widget; {@code unhoveredOpacity} is applied when it's outside. Both
     * values are clamped to {@code [0, 1]}. Defaults are {@code 1.0} and {@code 0.2} respectively.
     */
    public void setHoverFadeOpacities(double hoveredOpacity, double unhoveredOpacity) {
        this.hoverFadeHoveredOpacity = clampOpacity(hoveredOpacity);
        this.hoverFadeUnhoveredOpacity = clampOpacity(unhoveredOpacity);
        if (hoverFadeEnabled) {
            applyHoverFade(isHover());
        }
    }

    /** Returns the hover opacity (applied while the pointer is inside the widget). */
    public double hoverFadeHoveredOpacity() {
        return hoverFadeHoveredOpacity;
    }

    /** Returns the unhovered opacity (applied while the pointer is outside the widget). */
    public double hoverFadeUnhoveredOpacity() {
        return hoverFadeUnhoveredOpacity;
    }

    /**
     * Snaps this view's opacity to the appropriate hover-state target. No-op when hover fade is
     * disabled — callers should use {@link #setHoverFadeEnabled(boolean)} for that.
     *
     * <p>The original implementation animated the opacity with a {@link Timeline}, but running a
     * Timeline on {@code opacityProperty()} concurrently with the scroll Timeline on
     * {@code vvalueProperty()} appeared to suppress the scroll Timeline in the rendered output —
     * dropping the animation and snapping directly to the hover target avoids any chance of
     * cross-Timeline interference while still delivering the user-visible transparency change.</p>
     */
    private void applyHoverFade(boolean hovering) {
        if (!hoverFadeEnabled) {
            return;
        }
        setOpacity(hovering ? hoverFadeHoveredOpacity : hoverFadeUnhoveredOpacity);
    }

    private static double clampOpacity(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private Node renderEntry(Entry entry, boolean previous) {
        Node node;
        if (entry instanceof PlainEntry plain) {
            node = renderPlain(plain);
        } else if (entry instanceof CommentEntry comment) {
            node = renderComment(comment);
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

    /** Renders a {@link CommentEntry} as a small italic label.  The base
     *  {@link #ENTRY_STYLE_CLASS} keeps it in the same column / wrap behavior as other
     *  rows; the {@link #COMMENT_STYLE_CLASS} layered on top is the hook the stylesheet
     *  uses to drop the font size and italicise the body — see the engine default.css. */
    private static Label renderComment(CommentEntry entry) {
        Label label = new Label(entry.text());
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().addAll(ENTRY_STYLE_CLASS, COMMENT_STYLE_CLASS);
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
