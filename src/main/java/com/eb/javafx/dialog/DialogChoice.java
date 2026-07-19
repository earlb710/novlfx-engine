package com.eb.javafx.dialog;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One option presented at a {@link DialogNode.Choice} node. Selecting it advances the chain and
 * records the {@code (chainId, nodeId, choiceId)} triple on the {@link StoryLine}, where later
 * requirements can query it.
 *
 * <p>{@code nextId} is an optional branch target: when present, selecting this option jumps the
 * director to the node with that id inside the same chain, instead of falling through to the next
 * node in author order. {@code null} means "continue linearly".</p>
 *
 * <p>{@code responseLines} is an optional list of {@link DialogNode.Line} nodes that play
 * immediately after this option is selected, before the director moves on.  When the list is
 * empty no response plays — only the chosen option's label is shown as a comment in the
 * dialog scroll.</p>
 *
 * <p>{@code followUpChainId} is an optional cross-chain branch target: when present, selecting
 * this option swaps the active conversation to the named chain in the runtime tree (continuing
 * from its first node) instead of staying in the current chain.  This is the data-driven
 * equivalent of the legacy {@code DIALOG_CHOICE_FOLLOWUPS} routing map and is the way to fork
 * into a whole different scripted beat (its own scene, unlocks, items).  A {@code followUp}
 * takes precedence over {@code nextId} when both are set.</p>
 *
 * <h3>JSON format</h3>
 * <pre>{@code
 * {
 *   "id":    "agree",
 *   "label": "I understand.",
 *   "nextId": "node.7",                 // optional: jump within THIS chain
 *   "followUp": "marsh.meet.accept",    // optional: branch into ANOTHER chain (wins over nextId)
 *   "requires": { "minLove": 30, "reason": "She doesn't trust you yet." }, // optional: gate (see DialogChoiceRequirement)
 *   "response": [
 *     { "type": "line", "speaker": "marsh", "text": "Good. Then we can begin." }
 *   ]
 * }
 * }</pre>
 * Omit all three for a plain option that records the pick and continues in author order.
 */
public record DialogChoice(String id, String label, String nextId,
                           List<DialogNode.Line> responseLines, String followUpChainId,
                           DialogChoiceRequirement requirement) {
    public DialogChoice {
        id    = StoryStrings.requireKey(id, "Choice id is required.");
        label = StoryStrings.requireText(label, "Choice label is required.");
        nextId = (nextId == null || nextId.isBlank())
                ? null : StoryStrings.requireKey(nextId, "Choice nextId must be a valid key.");
        responseLines = responseLines == null ? List.of() : List.copyOf(responseLines);
        followUpChainId = (followUpChainId == null || followUpChainId.isBlank())
                ? null : StoryStrings.requireKey(followUpChainId, "Choice followUp must be a valid key.");
        // requirement nullable — null / empty means the option is always available
    }

    /** Backward-compatible 5-arg form (no availability requirement). */
    public DialogChoice(String id, String label, String nextId,
            List<DialogNode.Line> responseLines, String followUpChainId) {
        this(id, label, nextId, responseLines, followUpChainId, null);
    }

    /** Backward-compatible 4-arg form (no follow-up chain, no requirement). */
    public DialogChoice(String id, String label, String nextId, List<DialogNode.Line> responseLines) {
        this(id, label, nextId, responseLines, null, null);
    }

    /** Backward-compatible 3-arg form (no response lines, no follow-up, no requirement). */
    public DialogChoice(String id, String label, String nextId) {
        this(id, label, nextId, List.of(), null, null);
    }

    public static DialogChoice of(String id, String label) {
        return new DialogChoice(id, label, null, List.of(), null, null);
    }

    public static DialogChoice of(String id, String label, String nextId) {
        return new DialogChoice(id, label, nextId, List.of(), null, null);
    }

    public boolean hasId(String candidate) {
        return id.equals(Objects.requireNonNull(candidate, "Choice id is required."));
    }

    /** True when this option has at least one response line to play after selection. */
    public boolean hasResponse() {
        return !responseLines.isEmpty();
    }

    /** Branch target node id, if this option jumps rather than continuing linearly. */
    public Optional<String> branchTarget() {
        return Optional.ofNullable(nextId);
    }

    /** Cross-chain branch target, if this option forks into a different chain. */
    public Optional<String> followUp() {
        return Optional.ofNullable(followUpChainId);
    }

    /** Availability gate, if this option is conditionally offered. */
    public Optional<DialogChoiceRequirement> requirementOpt() {
        return (requirement == null || requirement.isEmpty())
                ? Optional.empty() : Optional.of(requirement);
    }
}
