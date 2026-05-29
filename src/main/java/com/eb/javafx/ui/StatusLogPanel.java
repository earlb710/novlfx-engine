package com.eb.javafx.ui;

import com.eb.javafx.events.GameEvent;
import com.eb.javafx.events.GameEventBus;
import com.eb.javafx.gamesupport.GameDateTime;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Generic vertical scrolling log surface for status changes — typically rendered alongside
 * the main dialog block to give the player a passive feed of stat / relationship /
 * inventory / achievement updates without interrupting the spoken-dialog flow.
 *
 * <p>The widget is a thin {@link ScrollPane} wrapping a {@link VBox} of {@link StatusLogEntry}
 * rows. Newest entries appear at the bottom (chronological order); the view auto-scrolls
 * to the bottom whenever the entries grow so a fresh status change is always visible.
 * The viewport reads as transparent by default — host stylesheets paint the chrome.</p>
 *
 * <h2>Adding entries</h2>
 *
 * <p>Hosts emit entries imperatively:</p>
 * <pre>
 *   StatusLogPanel statusLog = new StatusLogPanel();
 *   statusLog.append(StatusLogEntry.of("MC", "Stamina -1"));
 *   statusLog.append(StatusLogEntry.of("Anna", "Love +2", "relationship"));
 * </pre>
 *
 * <p>For event-driven systems, {@link #subscribeToEvents(GameEventBus, String, Function)}
 * wires the panel to a {@link GameEventBus} channel and converts incoming events into
 * entries via a translator. The returned {@link Runnable} unsubscribes.</p>
 *
 * <h2>Configuration</h2>
 *
 * <ul>
 *   <li>{@link #setMaxEntries(int)} — cap on retained entries. Default {@value #DEFAULT_MAX_ENTRIES}.
 *       Older entries are evicted from the front when the cap is exceeded.</li>
 *   <li>{@link #setTimestampVisible(boolean)} — show the {@code (d&lt;n&gt; &lt;slot&gt;)} prefix.
 *       Default {@code true}.</li>
 *   <li>{@link #setSubjectColumnWidth(double)} — fixed-width column for subject labels so
 *       entries line up. Default {@value #DEFAULT_SUBJECT_COLUMN_WIDTH} px.</li>
 * </ul>
 *
 * <h2>Styling</h2>
 *
 * <p>The default stylesheet ({@code default.css} in this package) ships rules for the
 * style classes exposed via {@code STYLE_CLASS} constants. Hosts can override any of
 * them in their own theme stylesheet to retune colours, fonts, and spacing.</p>
 */
public final class StatusLogPanel extends ScrollPane {
    /** Style class applied to the outer ScrollPane chrome. */
    public static final String STYLE_CLASS = "status-log-panel";
    /** Style class on the inner VBox that holds the entry rows. */
    public static final String ENTRIES_CONTAINER_STYLE_CLASS = "status-log-entries";
    /** Style class on each entry row ({@code HBox}). */
    public static final String ENTRY_STYLE_CLASS = "status-log-entry";
    /** Style class on the optional timestamp prefix label. */
    public static final String TIMESTAMP_STYLE_CLASS = "status-log-entry-timestamp";
    /** Style class on the subject label (left column). */
    public static final String SUBJECT_STYLE_CLASS = "status-log-entry-subject";
    /** Style class on the message body label (centre column). */
    public static final String MESSAGE_STYLE_CLASS = "status-log-entry-message";
    /** Style class on the optional category badge label (right column). */
    public static final String CATEGORY_STYLE_CLASS = "status-log-entry-category";

    /** Default cap on retained entries before older rows are evicted from the front. */
    public static final int DEFAULT_MAX_ENTRIES = 200;
    /** Default fixed width (in pixels) of the subject column so entries line up. */
    public static final double DEFAULT_SUBJECT_COLUMN_WIDTH = 64.0;

    private final VBox entriesContainer = new VBox(2);
    private final Deque<StatusLogEntry> entries = new ArrayDeque<>();
    /** Overrides {@link #renderEntry}'s default message-Label for entries appended via
     *  {@link #appendWithCustomMessage(StatusLogEntry, Node)}.  Identity-keyed so two
     *  equal-content StatusLogEntry records can each carry their own custom message
     *  Node — we don't want one custom row to silently steal another's content. */
    private final Map<StatusLogEntry, Node> customMessageContent = new IdentityHashMap<>();
    private final ReadOnlyIntegerWrapper entryCountProperty =
            new ReadOnlyIntegerWrapper(this, "entryCount", 0);

    private int maxEntries = DEFAULT_MAX_ENTRIES;
    private boolean timestampVisible = true;
    private double subjectColumnWidth = DEFAULT_SUBJECT_COLUMN_WIDTH;

    public StatusLogPanel() {
        getStyleClass().add(STYLE_CLASS);
        entriesContainer.getStyleClass().add(ENTRIES_CONTAINER_STYLE_CLASS);
        // Newest at the bottom — same chat-style pin used by DialogEntriesView. Combined
        // with fitToHeight + USE_PREF_SIZE min height below, BOTTOM_LEFT positions the
        // first entry against the panel's bottom edge until enough rows accumulate to
        // start overflowing.
        entriesContainer.setAlignment(Pos.BOTTOM_LEFT);
        // minWidth = 0 lets the column squeeze horizontally with the host's layout
        // share. minHeight is left at the VBox default (USE_PREF_SIZE = sum of children)
        // so the ScrollPane's AS_NEEDED vbar policy engages when the row stack exceeds
        // the viewport — without this, setFitToHeight(true) would force the VBox to the
        // viewport height regardless of how many children overflow it (the same trap that
        // bit DialogEntriesView before the fix).
        entriesContainer.setMinWidth(0);
        entriesContainer.setPickOnBounds(true);
        setContent(entriesContainer);
        setFitToWidth(true);
        // Stretch the content to the viewport so BOTTOM_LEFT pins a single row against
        // the bottom edge even when the panel is taller than the row count.  Tall content
        // still overflows correctly because we left entriesContainer.minHeight at
        // USE_PREF_SIZE above.
        setFitToHeight(true);
        setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setMinSize(0, 0);
        setPickOnBounds(true);
        // Swallow mouse-click events so the panel doesn't pass them to whatever sits
        // behind it (a layout-wide click filter on the gameplay root, say, that would
        // advance dialog).  Wheel scrolling on the panel is left alone — the ScrollPane's
        // native handler picks it up.
        addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        // Auto-pin to bottom whenever the entries container actually grows.  rebuild()
        // doesn't schedule a deferred scroll because the typical caller appends one entry
        // at a time and the height listener fires once the layout commits the new row —
        // simpler than the DialogEntriesView two-pulse dance, and equivalent in effect.
        entriesContainer.heightProperty().addListener((obs, oldH, newH) -> {
            if (newH.doubleValue() > oldH.doubleValue() && getVmax() > 0) {
                setVvalue(getVmax());
            }
        });
    }

    /**
     * Appends an entry to the log. Evicts the oldest entry when the cap (see
     * {@link #setMaxEntries(int)}) is exceeded so the buffer stays bounded for
     * long-running sessions.
     *
     * <p>No-op when {@code entry} is null.</p>
     */
    public void append(StatusLogEntry entry) {
        if (entry == null) {
            return;
        }
        entries.addLast(entry);
        while (entries.size() > maxEntries) {
            entries.pollFirst();
        }
        rebuild();
        entryCountProperty.set(entries.size());
    }

    /** Convenience: append a {@link StatusLogEntry#of(String, String)} entry. */
    public void append(String subject, String message) {
        append(StatusLogEntry.of(subject, message));
    }

    /** Convenience: append a {@link StatusLogEntry#of(String, String, String)} entry. */
    public void append(String subject, String message, String category) {
        append(StatusLogEntry.of(subject, message, category));
    }

    /** Convenience: append with a fully specified timestamp. */
    public void append(String subject, String message, String category, GameDateTime occurredAt) {
        append(StatusLogEntry.of(subject, message, category, occurredAt));
    }

    /**
     * Appends an entry whose message column is rendered by a caller-supplied {@link Node}
     * instead of a plain text Label.  Use this when the message needs richer content than
     * a single string — e.g. inline stat icons followed by deltas, an avatar thumbnail,
     * or coloured/badged segments.
     *
     * <p>The {@code entry.message()} string is retained as the textual fallback so
     * {@link #entries()} snapshots, history exports, and screen-reader paths still see a
     * meaningful representation of the row.  Hosts should set it to a plain-text
     * description of the same content (e.g. {@code "love+1, fear+5"} for an icon row
     * showing the same deltas).</p>
     *
     * <p>The supplied {@code messageContent} Node may be any JavaFX node — typically an
     * {@code HBox} of icons + labels.  It's placed in the message column slot of the row,
     * stretching horizontally with the same {@link Priority#ALWAYS Hgrow} treatment the
     * default Label receives.  The node is attached on the next rebuild and never
     * detached/rebuilt internally — callers MUST NOT reuse the same Node across multiple
     * appendWithCustomMessage calls (a Node can only have one parent in a scene graph).
     * Build a fresh Node per call.</p>
     */
    public void appendWithCustomMessage(StatusLogEntry entry, Node messageContent) {
        if (entry == null || messageContent == null) {
            return;
        }
        entries.addLast(entry);
        customMessageContent.put(entry, messageContent);
        while (entries.size() > maxEntries) {
            StatusLogEntry evicted = entries.pollFirst();
            if (evicted != null) {
                customMessageContent.remove(evicted);
            }
        }
        rebuild();
        entryCountProperty.set(entries.size());
    }

    /** Removes every entry and resets the panel to an empty state. */
    public void clear() {
        entries.clear();
        customMessageContent.clear();
        rebuild();
        entryCountProperty.set(0);
    }

    /** Returns an immutable snapshot of the current entries, oldest first. */
    public List<StatusLogEntry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /** Read-only property reporting the current entry count.  Useful for bindings. */
    public ReadOnlyIntegerProperty entryCountProperty() {
        return entryCountProperty.getReadOnlyProperty();
    }

    /** Current entry count. */
    public int entryCount() {
        return entryCountProperty.get();
    }

    /**
     * Sets the maximum number of entries retained.  When exceeded, the oldest entries are
     * dropped from the front so the live view stays bounded.  Must be positive.
     */
    public void setMaxEntries(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive.");
        }
        this.maxEntries = maxEntries;
        boolean changed = false;
        while (entries.size() > maxEntries) {
            entries.pollFirst();
            changed = true;
        }
        if (changed) {
            rebuild();
            entryCountProperty.set(entries.size());
        }
    }

    public int maxEntries() {
        return maxEntries;
    }

    /** Shows or hides the {@code (d&lt;n&gt; &lt;slot&gt;)} timestamp prefix on each entry. */
    public void setTimestampVisible(boolean visible) {
        if (this.timestampVisible == visible) {
            return;
        }
        this.timestampVisible = visible;
        rebuild();
    }

    public boolean isTimestampVisible() {
        return timestampVisible;
    }

    /**
     * Sets the fixed width of the subject column so subject labels line up regardless of
     * length.  Pass a non-positive value to skip the column entirely (subject text then
     * sits inline with the message).
     */
    public void setSubjectColumnWidth(double subjectColumnWidth) {
        this.subjectColumnWidth = Math.max(0.0, subjectColumnWidth);
        rebuild();
    }

    public double subjectColumnWidth() {
        return subjectColumnWidth;
    }

    /**
     * Wires this panel to a {@link GameEventBus} channel: whenever {@code bus} publishes
     * an event whose type matches {@code eventType}, {@code translator} converts it into
     * an entry that is appended to the log.  A null translator result drops the event
     * (useful for filtering).  The returned {@link Runnable} unsubscribes.
     *
     * <p>Hosts that already publish stat / inventory / relationship changes through a
     * {@link GameEventBus} can wire the log in one line:</p>
     *
     * <pre>
     *   statusLog.subscribeToEvents(eventBus, "stat.changed",
     *       event -&gt; StatusLogEntry.of(
     *               event.payload().get("subject"),
     *               event.payload().get("description"),
     *               "stat",
     *               event.occurredAt() == null ? null : clock.now()));
     * </pre>
     */
    public Runnable subscribeToEvents(GameEventBus bus, String eventType,
                                      Function<GameEvent, StatusLogEntry> translator) {
        if (bus == null) {
            throw new IllegalArgumentException("Event bus is required.");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Event type is required.");
        }
        if (translator == null) {
            throw new IllegalArgumentException("Translator is required.");
        }
        return bus.subscribe(eventType, event -> {
            StatusLogEntry entry = translator.apply(event);
            if (entry != null) {
                append(entry);
            }
        });
    }

    // ---------------------------------------------------------------------------------
    //  Rendering
    // ---------------------------------------------------------------------------------

    private void rebuild() {
        entriesContainer.getChildren().clear();
        for (StatusLogEntry entry : entries) {
            entriesContainer.getChildren().add(renderEntry(entry));
        }
        // Defer the scroll-to-bottom across TWO JavaFX pulses.  Reading getVmax() right
        // away still sees the old layout — the pulse hasn't committed the new children's
        // heights yet — so the pin falls short and the newest row shows only half-visible
        // until the user scrolls by hand.  The first runLater lets the pulse process the
        // children change and run layout; the nested runLater fires on the following tick,
        // after the ScrollPane viewport has caught up to the new content height, so
        // setVvalue(vmax) actually reaches the bottom of the newest entry.
        //
        // The heightProperty listener installed in the constructor is a redundant safety
        // net for the (rare) case where the deferred pin lands before the final layout
        // commits — same belt-and-braces approach DialogEntriesView uses.
        Platform.runLater(() -> Platform.runLater(this::scrollToBottomDeferred));
    }

    private void scrollToBottomDeferred() {
        double target = getVmax();
        if (target > 0) {
            setVvalue(target);
        }
    }

    private Node renderEntry(StatusLogEntry entry) {
        HBox row = new HBox(6);
        row.getStyleClass().add(ENTRY_STYLE_CLASS);
        row.setAlignment(Pos.TOP_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);

        if (timestampVisible && entry.occurredAt() != null) {
            Label timestampLabel = new Label(formatTimestamp(entry.occurredAt()));
            timestampLabel.getStyleClass().add(TIMESTAMP_STYLE_CLASS);
            timestampLabel.setMinWidth(Region.USE_PREF_SIZE);
            row.getChildren().add(timestampLabel);
        }

        if (subjectColumnWidth > 0 && !entry.subject().isEmpty()) {
            Label subjectLabel = new Label(entry.subject());
            subjectLabel.getStyleClass().add(SUBJECT_STYLE_CLASS);
            subjectLabel.setMinWidth(subjectColumnWidth);
            subjectLabel.setPrefWidth(subjectColumnWidth);
            subjectLabel.setMaxWidth(subjectColumnWidth);
            row.getChildren().add(subjectLabel);
        } else if (!entry.subject().isEmpty()) {
            Label subjectLabel = new Label(entry.subject());
            subjectLabel.getStyleClass().add(SUBJECT_STYLE_CLASS);
            subjectLabel.setMinWidth(Region.USE_PREF_SIZE);
            row.getChildren().add(subjectLabel);
        }

        Node messageContent = customMessageContent.get(entry);
        if (messageContent != null) {
            // Tag the host-provided node with the message style class so theme rules
            // targeting .status-log-entry-message still apply (a caller can opt out
            // by removing the class afterward).  HBox.setHgrow lets it stretch into
            // the row's remaining horizontal space the same way a default Label does.
            if (messageContent instanceof javafx.scene.Parent
                    && !messageContent.getStyleClass().contains(MESSAGE_STYLE_CLASS)) {
                messageContent.getStyleClass().add(MESSAGE_STYLE_CLASS);
            }
            HBox.setHgrow(messageContent, Priority.ALWAYS);
            row.getChildren().add(messageContent);
        } else {
            Label messageLabel = new Label(entry.message());
            messageLabel.getStyleClass().add(MESSAGE_STYLE_CLASS);
            messageLabel.setWrapText(true);
            messageLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(messageLabel, Priority.ALWAYS);
            row.getChildren().add(messageLabel);
        }

        // Category is intentionally NOT rendered as a visible badge — it stays on the
        // StatusLogEntry record so persistence / filtering / CSS hooks can still key off
        // it, but the per-row visual tag is dropped because hosts found the small italic
        // suffix on every row noisy ("stat", "day-divider", etc. trailing every entry).

        return row;
    }

    private static String formatTimestamp(GameDateTime timestamp) {
        String slotLabel = prettyTimeSlot(timestamp.timeSlotId());
        // Skip the "default" placeholder slot — most engines ship that as a fall-back
        // when the host hasn't wired a real time-of-day code table yet, and it doesn't
        // add information to the log row.  Falls through to a bare day stamp.
        if (slotLabel.isEmpty() || "Default".equalsIgnoreCase(slotLabel)) {
            return "(Day " + timestamp.day() + ")";
        }
        return "(Day " + timestamp.day() + " " + slotLabel + ")";
    }

    /** Converts a raw time-slot id ({@code "early-morning"}, {@code "afternoon"},
     *  {@code "night"}, etc.) into a display label by capitalising each word and
     *  replacing hyphens / underscores with spaces: {@code "early-morning"} →
     *  {@code "Early Morning"}, {@code "after_noon"} → {@code "After Noon"}.  Returns
     *  empty string for null / blank input.  Generic — the renderer can't assume which
     *  time-of-day vocabulary the host uses, so we apply the same word-boundary rule
     *  every game wants for log readability ("Morning", "Evening", "Late Night"). */
    /** Pretty-formats a time-slot id like {@code "late-night"} into {@code "Late Night"}.
     *  Exposed so hosts can format their own log lines / divider rows using the same
     *  capitalisation rule the panel applies to per-entry timestamps. */
    public static String prettyTimeSlot(String slotId) {
        if (slotId == null || slotId.isBlank()) {
            return "";
        }
        String[] words = slotId.split("[-_]");
        StringBuilder sb = new StringBuilder(slotId.length());
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }
}
