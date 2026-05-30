package com.eb.javafx.text;

import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.gamesupport.GameDateTime;
import com.eb.javafx.util.Validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mutable dialog history accumulator for an active play session.
 *
 * <p>The support methods enforce a single open dialog at a time: start a dated
 * dialog, append formatted content rows, then end it with a closing date stamp.</p>
 */
public final class DialogHistory {
    /**
     * Maximum number of conversation entries retained in memory. Once this limit is reached,
     * the oldest entry is dropped each time a new conversation begins, so the history acts as a
     * sliding window of the most recent 1 000 conversations.
     */
    public static final int MAX_CONVERSATIONS = 1000;

    private final List<DialogHistoryEntry> entries = new ArrayList<>();
    private int openEntryIndex = -1;

    /** Single shared instance used by every {@link com.eb.javafx.ui.DialogEntriesView}
     *  constructed via the no-arg constructor.  Installed once during {@code GameState}
     *  construction so the engine's gameplay-state instance and the widget that
     *  displays + saves history both point at the same backing store.  Without this
     *  pairing, the widget would create its own private history on each construction
     *  and the save-section codec that reads {@code GameState.conversationHistory()}
     *  would persist an empty history while the widget's actual content lived in a
     *  separate, unsaved instance.  See {@link #installShared} / {@link #shared}. */
    private static volatile DialogHistory sharedInstance;

    /**
     * Installs {@code instance} as the engine's shared {@link DialogHistory}.  Called
     * once during {@code GameState}'s constructor so every subsequent
     * {@link com.eb.javafx.ui.DialogEntriesView} built via the no-arg constructor
     * routes its writes through this instance — which is also what the gameplay-state
     * snapshot codec reads / writes.  Result: the conversation rows the player sees
     * accumulate into the SAME object the save system persists, so save/load actually
     * round-trips the dialog history instead of capturing a different empty instance.
     *
     * <p>Passing {@code null} reverts to "no shared instance — lazy fallback applies"
     * (see {@link #shared}).  Subsequent install calls replace the previous shared
     * instance; widgets already constructed against the prior instance keep that
     * reference, so callers should install ONCE at bootstrap and not re-install.</p>
     */
    public static void installShared(DialogHistory instance) {
        // First-wins: only the first non-null install takes effect.  Subsequent
        // installs from secondary GameState constructions (QuickSaveService,
        // SceneRouter fallback factories, etc.) are ignored so the bootstrap-time
        // GameState's history remains the canonical shared instance for the entire
        // session — which is the one widgets capture and the save codec serialises.
        // Passing null is treated as an explicit reset (used by tests in @BeforeEach
        // to give each test a fresh shared instance).
        if (instance == null) {
            sharedInstance = null;
            return;
        }
        if (sharedInstance == null) {
            sharedInstance = instance;
        }
    }

    /**
     * Returns the shared {@link DialogHistory} instance — installed by the engine's
     * {@link com.eb.javafx.state.GameState} constructor via {@link #installShared}.
     * Falls back to a lazily-created singleton when no install has happened (tests,
     * headless tools, or hosts that construct a widget before bootstrap).  The
     * fallback is process-wide so two widgets built before bootstrap still see the
     * same history; the install replaces the fallback if it fired first.
     */
    public static DialogHistory shared() {
        DialogHistory current = sharedInstance;
        if (current != null) {
            return current;
        }
        // Synchronised lazy init for the "constructed before bootstrap" path.
        synchronized (DialogHistory.class) {
            if (sharedInstance == null) {
                sharedInstance = new DialogHistory();
            }
            return sharedInstance;
        }
    }

    public DialogHistoryEntry beginDialog(String dialogId, GameDateTime startedAt) {
        if (openEntryIndex >= 0) {
            throw new IllegalStateException("A dialog history entry is already open.");
        }
        DialogHistoryEntry entry = DialogHistoryEntry.started(dialogId, startedAt);
        entries.add(entry);
        // Trim the oldest conversation once the sliding window is full. openEntryIndex always
        // points at the last element, so the index stays valid after the remove.
        if (entries.size() > MAX_CONVERSATIONS) {
            entries.remove(0);
        }
        openEntryIndex = entries.size() - 1;
        return entry;
    }

    public DialogHistoryEntry beginDialog(String dialogId, GameClock clock) {
        Validation.requireNonNull(clock, "Dialog history clock is required.");
        return beginDialog(dialogId, clock.currentTime());
    }

    public DialogHistoryEntry addMessage(DialogMessage message) {
        ensureOpen();
        DialogHistoryEntry updatedEntry = entries.get(openEntryIndex).withMessage(message);
        entries.set(openEntryIndex, updatedEntry);
        return updatedEntry;
    }

    public DialogHistoryEntry addMessage(DialogSpeaker speaker, String message) {
        return addMessage(DialogMessage.speakerMessage(speaker, message));
    }

    public Optional<DialogMessage> removeLastMessage() {
        ensureOpen();
        DialogHistoryEntry entry = entries.get(openEntryIndex);
        if (entry.messages().isEmpty()) {
            return Optional.empty();
        }
        DialogMessage removed = entry.messages().get(entry.messages().size() - 1);
        entries.set(openEntryIndex, entry.withoutLastMessage());
        return Optional.of(removed);
    }

    public DialogHistoryEntry endDialog(GameDateTime endedAt) {
        ensureOpen();
        DialogHistoryEntry updatedEntry = entries.get(openEntryIndex).ended(endedAt);
        entries.set(openEntryIndex, updatedEntry);
        openEntryIndex = -1;
        return updatedEntry;
    }

    public DialogHistoryEntry endDialog(GameClock clock) {
        Validation.requireNonNull(clock, "Dialog history clock is required.");
        return endDialog(clock.currentTime());
    }

    public List<DialogHistoryEntry> entries() {
        return List.copyOf(entries);
    }

    public Optional<DialogHistoryEntry> openDialog() {
        return openEntryIndex < 0 ? Optional.empty() : Optional.of(entries.get(openEntryIndex));
    }

    private void ensureOpen() {
        if (openEntryIndex < 0) {
            throw new IllegalStateException("No dialog history entry is open.");
        }
    }

    /**
     * Bulk-restores the history's state from a snapshot.  Used by the save/load path —
     * see {@code DialogHistorySnapshotJson} — to put the history back to its
     * write-time contents after loading a save.  Clears the existing entries first so
     * the result is the supplied list verbatim, not appended.
     *
     * <p>{@code openEntryIndex} is the cursor into {@code entries} that
     * {@link #addMessage} / {@link #endDialog} target.  Pass {@code -1} when no dialog
     * is currently open (the common case at save time); pass the index of an entry
     * with {@code endedAt == null} to resume a mid-conversation save.</p>
     */
    public void restoreEntries(List<DialogHistoryEntry> restoredEntries, int restoredOpenEntryIndex) {
        Validation.requireNonNull(restoredEntries, "Restored dialog history entries are required.");
        if (restoredOpenEntryIndex >= restoredEntries.size()) {
            throw new IllegalArgumentException(
                    "Restored open entry index " + restoredOpenEntryIndex
                            + " is past the end of the restored entries (size = "
                            + restoredEntries.size() + ").");
        }
        if (restoredOpenEntryIndex >= 0
                && restoredEntries.get(restoredOpenEntryIndex).endedAt() != null) {
            throw new IllegalArgumentException(
                    "Restored open entry at index " + restoredOpenEntryIndex
                            + " is already ended — open index must point at an open entry or be -1.");
        }
        entries.clear();
        for (DialogHistoryEntry entry : restoredEntries) {
            entries.add(Validation.requireNonNull(entry, "Restored dialog history entry must be non-null."));
        }
        openEntryIndex = restoredOpenEntryIndex < 0 ? -1 : restoredOpenEntryIndex;
    }

    /** Returns the index of the currently-open dialog entry, or {@code -1} when no
     *  dialog is open.  Exposed so the save snapshot codec can persist the cursor. */
    public int openEntryIndex() {
        return openEntryIndex;
    }

    /** Resets the history to its empty baseline — no entries, no open dialog.  Used
     *  by save/load as the pre-load reset (see {@code GameState.restore} reset pass)
     *  so a loaded slot doesn't merge into the current in-memory history. */
    public void clear() {
        entries.clear();
        openEntryIndex = -1;
    }
}
