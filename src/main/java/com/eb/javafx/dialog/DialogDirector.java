package com.eb.javafx.dialog;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Plays one {@link DialogChain} at a time against a {@link StoryLine}.
 *
 * <p>Playback model — matches the user-facing contract "dialog stops when chain is finished, then
 * input from player is required":</p>
 * <ol>
 *   <li>{@link #startChain(DialogChain)} loads a chain. The first node is the current node.</li>
 *   <li>The caller reads {@link #currentNode()} and renders it.</li>
 *   <li>{@link #advance()} moves to the next node when the current node is a {@link DialogNode.Line}.
 *       Calling advance on a {@link DialogNode.Choice} node throws — the caller must select an option.</li>
 *   <li>{@link #selectChoice(String)} records the option, advances past the choice node.</li>
 *   <li>When the cursor passes the last node the chain is marked completed on the {@link StoryLine}
 *       and {@link #isChainFinished()} returns true. The director becomes idle until another chain
 *       is started.</li>
 * </ol>
 */
public final class DialogDirector {

    private final StoryLine storyLine;
    private DialogChain activeChain;
    private int cursor;
    private boolean chainFinished = true;
    /** Response lines queued from the most recently selected choice option.  Served by
     *  {@link #currentNode()} and drained one-by-one by {@link #advance()} before the
     *  director moves on to the next authored chain node. */
    private final Deque<DialogNode.Line> pendingResponses = new ArrayDeque<>();

    /** Summary mode — the default reading: spoken/normal lines plus the short {@code S} summaries,
     *  hiding the long full-story ({@code Y}) paragraphs.  This is the "game version". */
    public static final Predicate<DialogNode.Line> SUMMARY_MODE =
            line -> line.storyDetail() != DialogNode.StoryDetail.FULL;

    /** Full-story mode — spoken/normal lines plus the long full-story ({@code Y}) paragraphs,
     *  hiding the {@code S} summaries that stand in for them. */
    public static final Predicate<DialogNode.Line> FULL_STORY_MODE =
            line -> line.storyDetail() != DialogNode.StoryDetail.SUMMARY;

    /** Decides whether a {@link DialogNode.Line} is played; lines that fail it are skipped during
     *  advancement.  Defaults to {@link #SUMMARY_MODE} (no filter ⇒ the concise game version);
     *  gameplay swaps to {@link #FULL_STORY_MODE} when the player enables full story text (see
     *  {@code AltLifeStoryDetail}).  Showing a full paragraph and its summary together is never a
     *  valid state, so there is deliberately no "show everything" default. */
    private Predicate<DialogNode.Line> lineFilter = SUMMARY_MODE;

    public DialogDirector(StoryLine storyLine) {
        this.storyLine = Objects.requireNonNull(storyLine, "StoryLine is required.");
    }

    /**
     * Installs a line visibility filter.  When set, the director auto-skips any
     * {@link DialogNode.Line} the filter rejects as it advances — so a chain authored with both
     * full-story ({@code Y}) and summary ({@code S}) lines plays only the set the current mode
     * wants, with no work required from the caller.  A {@code null} filter restores "show all".
     */
    public void setLineFilter(Predicate<DialogNode.Line> lineFilter) {
        this.lineFilter = lineFilter == null ? SUMMARY_MODE : lineFilter;
        // Re-apply immediately in case the director is already parked on a now-hidden line.
        skipFilteredLines();
    }

    /** Advances the cursor over any leading {@link DialogNode.Line} the current filter rejects,
     *  completing the chain if the skipping runs off the end. */
    private void skipFilteredLines() {
        while (!chainFinished && activeChain != null
                && activeChain.node(cursor) instanceof DialogNode.Line line
                && !lineFilter.test(line)) {
            cursor++;
            if (cursor >= activeChain.size()) {
                storyLine.markChainCompleted(activeChain);
                chainFinished = true;
            }
        }
    }

    public StoryLine storyLine() {
        return storyLine;
    }

    /** Begin playing the supplied chain. The previous chain (if any) is replaced without completing it. */
    public void startChain(DialogChain chain) {
        this.activeChain = Objects.requireNonNull(chain, "Chain is required.");
        this.cursor = 0;
        this.chainFinished = false;
        pendingResponses.clear();
        skipFilteredLines();
    }

    /**
     * Restarts playback inside {@code chain} at the supplied {@code cursor}. Used by back/forward
     * snapshot navigation to restore the director to a prior position without replaying lines.
     */
    public void restartAt(DialogChain chain, int cursor) {
        Objects.requireNonNull(chain, "Chain is required.");
        if (cursor < 0 || cursor > chain.size()) {
            throw new IllegalArgumentException(
                    "Cursor out of range [0," + chain.size() + "]: " + cursor);
        }
        this.activeChain = chain;
        this.cursor = cursor;
        this.chainFinished = cursor >= chain.size();
        pendingResponses.clear();
    }

    /** Current cursor index. Equals {@code activeChain.size()} when the chain has finished. */
    public int cursor() {
        return cursor;
    }

    public Optional<DialogChain> activeChain() {
        return Optional.ofNullable(chainFinished ? null : activeChain);
    }

    /** Id of the chain the director is positioned in (even when finished). Empty before start. */
    public Optional<String> activeChainId() {
        return Optional.ofNullable(activeChain).map(DialogChain::id);
    }

    public boolean isIdle() {
        return chainFinished && pendingResponses.isEmpty();
    }

    public boolean isChainFinished() {
        return chainFinished && pendingResponses.isEmpty();
    }

    /** True when the director is waiting for a choice selection — never true while
     *  response lines from a previous choice are still playing. */
    public boolean isAwaitingChoice() {
        return !chainFinished && pendingResponses.isEmpty() && activeChain != null
                && activeChain.node(cursor) instanceof DialogNode.Choice;
    }

    /** True when response lines from the most recently selected option are still playing. */
    public boolean isPlayingResponse() {
        return !pendingResponses.isEmpty();
    }

    /** Current node.  Serves pending response lines first; throws when the director is idle. */
    public DialogNode currentNode() {
        if (!pendingResponses.isEmpty()) {
            return pendingResponses.peek();
        }
        if (chainFinished || activeChain == null) {
            throw new NoSuchElementException("Director has no active chain.");
        }
        return activeChain.node(cursor);
    }

    /** Advances past a {@link DialogNode.Line}.  Drains pending response lines one by one
     *  before moving the chain cursor.  Throws when sitting on a choice or when idle. */
    public void advance() {
        // Drain response lines first.
        if (!pendingResponses.isEmpty()) {
            pendingResponses.poll();
            return;
        }
        if (chainFinished || activeChain == null) {
            throw new IllegalStateException("Director has no active chain.");
        }
        DialogNode node = activeChain.node(cursor);
        if (node instanceof DialogNode.Choice) {
            throw new IllegalStateException(
                    "Active node is a choice; call selectChoice(id) instead. Node: " + node.id());
        }
        moveForward();
    }

    /** Records the selection, queues any response lines from the chosen option, advances past
     *  the choice node, and returns the chosen option id. */
    public String selectChoice(String choiceId) {
        if (chainFinished || activeChain == null) {
            throw new IllegalStateException("Director has no active chain.");
        }
        DialogNode node = activeChain.node(cursor);
        if (!(node instanceof DialogNode.Choice choice)) {
            throw new IllegalStateException(
                    "Active node is not a choice node. Got: " + node.id());
        }
        DialogChoice option = choice.option(choiceId);
        storyLine.recordChoice(activeChain.id(), choice.id(), option.id());

        // Queue response lines (respecting the current line filter).
        pendingResponses.clear();
        for (DialogNode.Line responseLine : option.responseLines()) {
            if (lineFilter.test(responseLine)) {
                pendingResponses.add(responseLine);
            }
        }

        String branchTarget = option.nextId();
        if (branchTarget != null) {
            int target = activeChain.indexOf(branchTarget);
            if (target < 0) {
                throw new IllegalStateException("Choice option " + option.id() + " on node " + choice.id()
                        + " branches to unknown node id: " + branchTarget);
            }
            moveTo(target);
        } else {
            moveForward();
        }
        return option.id();
    }

    private void moveForward() {
        moveTo(cursor + 1);
    }

    /** Positions the cursor at {@code index}, completing the chain when it lands past the last node.
     *  Then skips any filtered-out lines so the director never rests on a hidden line. */
    private void moveTo(int index) {
        cursor = index;
        if (cursor >= activeChain.size()) {
            storyLine.markChainCompleted(activeChain);
            chainFinished = true;
        }
        skipFilteredLines();
    }
}
