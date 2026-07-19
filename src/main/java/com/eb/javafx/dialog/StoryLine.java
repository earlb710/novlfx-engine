package com.eb.javafx.dialog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Mutable runtime state tracking where the MC is in the story with each character.
 *
 * <p>Records which {@link DialogChain chains} have finished, the choices recorded inside them,
 * which characters have been talked to, and arbitrary story flags / triggers. {@link
 * DialogRequirement} implementations read this state to decide whether a chain is eligible.</p>
 *
 * <p>Per-character progress is derived from chain completion: the number of completed chains
 * tagged with each {@code characterRoleId} is the character's current story stage.</p>
 */
public final class StoryLine {

    private final LinkedHashSet<String> completedChains = new LinkedHashSet<>();
    private final List<String> completionOrder = new ArrayList<>();
    private final LinkedHashMap<String, String> recordedChoices = new LinkedHashMap<>();
    private final LinkedHashSet<String> talkedToCharacters = new LinkedHashSet<>();
    private final LinkedHashSet<String> flags = new LinkedHashSet<>();

    public boolean hasCompletedChain(String chainId) {
        return completedChains.contains(StoryStrings.requireKey(chainId, "Chain id is required."));
    }

    public boolean hasTalkedTo(String characterRoleId) {
        return talkedToCharacters.contains(
                StoryStrings.requireKey(characterRoleId, "Character role id is required."));
    }

    public boolean isFlagSet(String flagKey) {
        return flags.contains(StoryStrings.requireKey(flagKey, "Flag key is required."));
    }

    public Optional<String> recordedChoice(String chainId, String nodeId) {
        String key = choiceKey(
                StoryStrings.requireKey(chainId, "Chain id is required."),
                StoryStrings.requireKey(nodeId, "Node id is required."));
        return Optional.ofNullable(recordedChoices.get(key));
    }

    public Set<String> completedChains() {
        return Set.copyOf(completedChains);
    }

    public List<String> completionOrder() {
        return List.copyOf(completionOrder);
    }

    public Set<String> flags() {
        return Set.copyOf(flags);
    }

    public Set<String> talkedToCharacters() {
        return Set.copyOf(talkedToCharacters);
    }

    public Map<String, String> recordedChoices() {
        return Map.copyOf(recordedChoices);
    }

    public void markChainCompleted(DialogChain chain) {
        Objects.requireNonNull(chain, "Chain is required.");
        if (completedChains.add(chain.id())) {
            completionOrder.add(chain.id());
        }
        chain.bookRole().ifPresent(talkedToCharacters::add);
    }

    public void recordChoice(String chainId, String nodeId, String choiceId) {
        String key = choiceKey(
                StoryStrings.requireKey(chainId, "Chain id is required."),
                StoryStrings.requireKey(nodeId, "Node id is required."));
        recordedChoices.put(key, StoryStrings.requireKey(choiceId, "Choice id is required."));
    }

    public void setFlag(String flagKey) {
        flags.add(StoryStrings.requireKey(flagKey, "Flag key is required."));
    }

    public void clearFlag(String flagKey) {
        flags.remove(StoryStrings.requireKey(flagKey, "Flag key is required."));
    }

    public void recordTrigger(String triggerId) {
        setFlag(triggerId);
    }

    /** Returns all non-completed chains. Eligibility ordering is handled by the engine's storyline system. */
    public List<DialogChain> eligibleChains(DialogTree tree) {
        Objects.requireNonNull(tree, "Tree is required.");
        List<DialogChain> result = new ArrayList<>();
        for (DialogChain chain : tree.chains()) {
            if (!hasCompletedChain(chain.id())) {
                result.add(chain);
            }
        }
        return List.copyOf(result);
    }

    /** First eligible chain, preferring one that matches the {@code activeCharacter} when set. */
    public Optional<DialogChain> nextChain(DialogTree tree, StoryContext context) {
        List<DialogChain> eligible = eligibleChains(tree);
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        if (context.activeCharacter().isPresent()) {
            String activeRole = context.activeCharacter().get();
            for (DialogChain chain : eligible) {
                if (activeRole.equals(chain.characterRoleId())) {
                    return Optional.of(chain);
                }
            }
        }
        return Optional.of(eligible.get(0));
    }

    /** Count of completed chains that belong to the supplied book role — the character's story stage. */
    public int progressFor(String characterRoleId, DialogTree tree) {
        Objects.requireNonNull(tree, "Tree is required.");
        String role = StoryStrings.requireKey(characterRoleId, "Character role id is required.");
        int count = 0;
        for (String chainId : completionOrder) {
            DialogChain chain = tree.chain(chainId).orElse(null);
            if (chain != null && role.equals(chain.characterRoleId())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns an immutable snapshot of every mutable field on this story line. Use {@link
     * #restore(Snapshot)} to load it back. Saves, save/load, and back/forward navigation use this
     * round-trip to preserve recorded choices and chain completions.
     */
    public Snapshot snapshot() {
        return new Snapshot(
                List.copyOf(completionOrder),
                Map.copyOf(recordedChoices),
                Set.copyOf(talkedToCharacters),
                Set.copyOf(flags));
    }

    /**
     * Replaces all state with the values in {@code snapshot}. Equivalent to {@link #reset()} +
     * replaying the snapshot's contents.
     */
    public void restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot is required.");
        reset();
        for (String chainId : snapshot.completionOrder()) {
            completedChains.add(chainId);
            completionOrder.add(chainId);
        }
        recordedChoices.putAll(snapshot.recordedChoices());
        talkedToCharacters.addAll(snapshot.talkedToCharacters());
        flags.addAll(snapshot.flags());
    }

    /** Immutable view of {@link StoryLine} state. Round-trips through {@link #snapshot()} / {@link #restore}. */
    public record Snapshot(
            List<String> completionOrder,
            Map<String, String> recordedChoices,
            Set<String> talkedToCharacters,
            Set<String> flags) {
        public Snapshot {
            completionOrder = List.copyOf(Objects.requireNonNull(completionOrder, "completionOrder is required."));
            recordedChoices = Map.copyOf(Objects.requireNonNull(recordedChoices, "recordedChoices are required."));
            talkedToCharacters = Set.copyOf(Objects.requireNonNull(talkedToCharacters, "talkedToCharacters are required."));
            flags = Set.copyOf(Objects.requireNonNull(flags, "flags are required."));
        }

        public static Snapshot empty() {
            return new Snapshot(List.of(), Map.of(), Set.of(), Set.of());
        }
    }

    /** Resets all state. Intended for tests and new-game flows. */
    public void reset() {
        completedChains.clear();
        completionOrder.clear();
        recordedChoices.clear();
        talkedToCharacters.clear();
        flags.clear();
    }

    private static String choiceKey(String chainId, String nodeId) {
        return chainId + "#" + nodeId;
    }
}
