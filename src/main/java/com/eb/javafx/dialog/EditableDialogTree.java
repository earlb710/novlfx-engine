package com.eb.javafx.dialog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable working copy of a {@link DialogTree}. Used by the story editor to stage chain edits
 * (add / replace / remove) and rebuild a validated, immutable {@link DialogTree} on
 * {@link #commit()}.
 *
 * <p>Reverting is just {@code new EditableDialogTree(authored)}.</p>
 */
public final class EditableDialogTree {

    private final LinkedHashMap<String, DialogChain> chains = new LinkedHashMap<>();
    private final LinkedHashMap<String, DialogScene> scenes = new LinkedHashMap<>();
    private String rootChainId;

    public EditableDialogTree(DialogTree source) {
        Objects.requireNonNull(source, "Source tree is required.");
        for (DialogChain chain : source.chains()) {
            chains.put(chain.id(), chain);
        }
        for (DialogScene scene : source.scenes()) {
            scenes.put(scene.id(), scene);
        }
        this.rootChainId = source.rootChainId();
    }

    public String rootChainId() {
        return rootChainId;
    }

    public void setRootChainId(String chainId) {
        String trimmed = StoryStrings.requireKey(chainId, "Root chain id is required.");
        if (!chains.containsKey(trimmed)) {
            throw new IllegalArgumentException("Cannot set root to unknown chain: " + trimmed);
        }
        this.rootChainId = trimmed;
    }

    public List<DialogChain> chains() {
        return List.copyOf(chains.values());
    }

    public List<String> chainIds() {
        return List.copyOf(chains.keySet());
    }

    public DialogChain chain(String chainId) {
        DialogChain chain = chains.get(StoryStrings.requireKey(chainId, "Chain id is required."));
        if (chain == null) {
            throw new IllegalArgumentException("Unknown chain: " + chainId);
        }
        return chain;
    }

    /** Adds a brand-new chain. Throws if {@code chain.id()} already exists. */
    public void addChain(DialogChain chain) {
        Objects.requireNonNull(chain, "Chain is required.");
        if (chains.containsKey(chain.id())) {
            throw new IllegalArgumentException("Chain already exists: " + chain.id());
        }
        chains.put(chain.id(), chain);
    }

    /** Replaces an existing chain. The replacement must use the same id. */
    public void replaceChain(DialogChain chain) {
        Objects.requireNonNull(chain, "Chain is required.");
        if (!chains.containsKey(chain.id())) {
            throw new IllegalArgumentException("No chain to replace: " + chain.id());
        }
        chains.put(chain.id(), chain);
    }

    /** Renames a chain. Updates the {@code rootChainId} when the renamed chain is the root. */
    public void renameChain(String oldId, String newId) {
        String oldKey = StoryStrings.requireKey(oldId, "Old chain id is required.");
        String newKey = StoryStrings.requireKey(newId, "New chain id is required.");
        if (oldKey.equals(newKey)) {
            return;
        }
        if (!chains.containsKey(oldKey)) {
            throw new IllegalArgumentException("Unknown chain: " + oldKey);
        }
        if (chains.containsKey(newKey)) {
            throw new IllegalArgumentException("Chain already exists: " + newKey);
        }
        LinkedHashMap<String, DialogChain> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, DialogChain> entry : chains.entrySet()) {
            if (entry.getKey().equals(oldKey)) {
                DialogChain chain = entry.getValue();
                rebuilt.put(newKey, new DialogChain(newKey, chain.characterRoleId(), chain.sceneId(), chain.sequence(), chain.nodes(), chain.tags(),
                        chain.phase(), chain.conversationWith(), chain.before(), chain.after(),
                        chain.locationUnlocks(), chain.roomUnlocks(), chain.description(),
                        chain.locationTrigger(), chain.requires(), chain.storyline(), chain.step(),
                        chain.phoneUnlocks()));
            } else {
                rebuilt.put(entry.getKey(), entry.getValue());
            }
        }
        chains.clear();
        chains.putAll(rebuilt);
        if (oldKey.equals(rootChainId)) {
            rootChainId = newKey;
        }
    }

    /** Removes a chain. Cannot remove the root. */
    public void removeChain(String chainId) {
        String trimmed = StoryStrings.requireKey(chainId, "Chain id is required.");
        if (trimmed.equals(rootChainId)) {
            throw new IllegalArgumentException("Cannot remove the root chain: " + trimmed);
        }
        if (!chains.containsKey(trimmed)) {
            throw new IllegalArgumentException("Unknown chain: " + trimmed);
        }
        chains.remove(trimmed);
    }

    // ----- scene operations -----

    public List<DialogScene> scenes() {
        return List.copyOf(scenes.values());
    }

    public java.util.Optional<DialogScene> findScene(String sceneId) {
        if (sceneId == null || sceneId.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(scenes.get(sceneId.trim()));
    }

    /** Adds a brand-new scene. Throws if {@code scene.id()} already exists. */
    public void addScene(DialogScene scene) {
        Objects.requireNonNull(scene, "Scene is required.");
        if (scenes.containsKey(scene.id())) {
            throw new IllegalArgumentException("Scene already exists: " + scene.id());
        }
        scenes.put(scene.id(), scene);
    }

    /** Replaces an existing scene. The replacement must use the same id. */
    public void replaceScene(DialogScene scene) {
        Objects.requireNonNull(scene, "Scene is required.");
        if (!scenes.containsKey(scene.id())) {
            throw new IllegalArgumentException("No scene to replace: " + scene.id());
        }
        scenes.put(scene.id(), scene);
    }

    /**
     * Removes a scene. Chains belonging to the removed scene keep their {@code sceneId} value but
     * will fail {@link #commit()} validation; the caller should reassign or clear them first.
     */
    public void removeScene(String sceneId) {
        String trimmed = StoryStrings.requireKey(sceneId, "Scene id is required.");
        if (!scenes.containsKey(trimmed)) {
            throw new IllegalArgumentException("Unknown scene: " + trimmed);
        }
        scenes.remove(trimmed);
    }

    /** Sorted set of every scene id in use. */
    public java.util.SortedSet<String> allSceneIds() {
        return java.util.Collections.unmodifiableSortedSet(new java.util.TreeSet<>(scenes.keySet()));
    }

    /** Builds and validates an immutable {@link DialogTree}. Throws if validation fails. */
    public DialogTree commit() {
        DialogTree.Builder builder = DialogTree.builder(rootChainId);
        for (DialogScene scene : scenes.values()) {
            builder.scene(scene);
        }
        for (DialogChain chain : chains.values()) {
            builder.chain(chain);
        }
        return builder.build();
    }

    public int size() {
        return chains.size();
    }

    /**
     * Sorted set of every tag in use across all chains. Used by the editor's tag combo so authors
     * can pick from existing tags instead of re-typing them.
     */
    public java.util.SortedSet<String> allTags() {
        java.util.TreeSet<String> tags = new java.util.TreeSet<>();
        for (DialogChain chain : chains.values()) {
            tags.addAll(chain.tags());
        }
        return java.util.Collections.unmodifiableSortedSet(tags);
    }

    /**
     * Highest authored {@code sequence} across all chains, or {@link DialogChain#DEFAULT_SEQUENCE}
     * when empty. Used by the editor to place new chains at the end of their group.
     */
    public int maxSequence() {
        int max = DialogChain.DEFAULT_SEQUENCE;
        for (DialogChain chain : chains.values()) {
            if (chain.sequence() > max) {
                max = chain.sequence();
            }
        }
        return max;
    }

    /** Convenience: rewrite the nodes of an existing chain, keeping id/role/sequence/tags. */
    public DialogChain rewriteNodes(String chainId, List<DialogNode> newNodes) {
        DialogChain original = chain(chainId);
        DialogChain replacement = new DialogChain(
                original.id(),
                original.characterRoleId(),
                original.sceneId(),
                original.sequence(),
                newNodes,
                original.tags(),
                original.phase(),
                original.conversationWith(),
                original.before(),
                original.after(),
                original.locationUnlocks(),
                original.roomUnlocks(),
                original.description(),
                original.locationTrigger(),
                original.requires(),
                original.storyline(),
                original.step(),
                original.phoneUnlocks());
        replaceChain(replacement);
        return replacement;
    }

    /** Snapshots all chain ids → display strings for editor list views. */
    public Map<String, String> displaySummary() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (DialogChain chain : chains.values()) {
            String role = chain.bookRole().orElse("—");
            out.put(chain.id(), chain.id() + " · " + role + " · " + chain.nodes().size() + " nodes");
        }
        return Map.copyOf(out);
    }
}
