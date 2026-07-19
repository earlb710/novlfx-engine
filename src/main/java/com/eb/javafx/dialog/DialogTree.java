package com.eb.javafx.dialog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Authored registry of all {@link DialogChain dialog chains} and {@link DialogScene scenes},
 * together with the {@code rootChainId} — the chain that plays when the game starts.
 *
 * <p>Validates the graph at construction: chain ids must be unique, the root chain must be
 * registered, and any chain that declares a {@link DialogChain#sceneId()} must reference a scene
 * registered in this tree. Eligibility ordering and unlocking are handled by the engine's
 * storyline system, not by the tree itself.</p>
 */
public final class DialogTree {

    private final Map<String, DialogChain> chainsById;
    private final Map<String, DialogScene> scenesById;
    private final String rootChainId;

    private DialogTree(Map<String, DialogChain> chainsById, Map<String, DialogScene> scenesById, String rootChainId) {
        this.chainsById = Map.copyOf(chainsById);
        this.scenesById = Map.copyOf(scenesById);
        this.rootChainId = rootChainId;
    }

    public DialogChain root() {
        return chainsById.get(rootChainId);
    }

    public String rootChainId() {
        return rootChainId;
    }

    public Optional<DialogChain> chain(String chainId) {
        if (chainId == null || chainId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(chainsById.get(chainId.trim()));
    }

    public DialogChain requireChain(String chainId) {
        return chain(chainId).orElseThrow(() ->
                new IllegalArgumentException("Unknown dialog chain id: " + chainId));
    }

    public List<DialogChain> chains() {
        return List.copyOf(chainsById.values());
    }

    public int size() {
        return chainsById.size();
    }

    /** All chains tagged with the supplied book role id. */
    public List<DialogChain> chainsFor(String characterRoleId) {
        String role = StoryStrings.requireKey(characterRoleId, "Character role id is required.");
        List<DialogChain> result = new ArrayList<>();
        for (DialogChain chain : chainsById.values()) {
            if (role.equals(chain.characterRoleId())) {
                result.add(chain);
            }
        }
        return List.copyOf(result);
    }

    // ----- scene queries -----

    /** All scenes registered in this tree, in author order. */
    public List<DialogScene> scenes() {
        return List.copyOf(scenesById.values());
    }

    /** Look up a scene by id. */
    public Optional<DialogScene> findScene(String sceneId) {
        if (sceneId == null || sceneId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(scenesById.get(sceneId.trim()));
    }

    /** All chains whose {@link DialogChain#sceneId()} matches {@code sceneId}. */
    public List<DialogChain> chainsInScene(String sceneId) {
        String key = StoryStrings.requireKey(sceneId, "Scene id is required.");
        List<DialogChain> result = new ArrayList<>();
        for (DialogChain chain : chainsById.values()) {
            if (key.equals(chain.sceneId())) {
                result.add(chain);
            }
        }
        return List.copyOf(result);
    }

    /** All chains that have no scene parent ({@link DialogChain#sceneId()} is null). */
    public List<DialogChain> unscenedChains() {
        List<DialogChain> result = new ArrayList<>();
        for (DialogChain chain : chainsById.values()) {
            if (chain.sceneId() == null) {
                result.add(chain);
            }
        }
        return List.copyOf(result);
    }

    public static Builder builder(String rootChainId) {
        return new Builder(rootChainId);
    }

    /** Mutable builder for {@link DialogTree}. */
    public static final class Builder {
        private final String rootChainId;
        private final LinkedHashMap<String, DialogChain> chainsById = new LinkedHashMap<>();
        private final LinkedHashMap<String, DialogScene> scenesById = new LinkedHashMap<>();

        private Builder(String rootChainId) {
            this.rootChainId = StoryStrings.requireKey(rootChainId, "Root chain id is required.");
        }

        public Builder chain(DialogChain chain) {
            Objects.requireNonNull(chain, "Chain is required.");
            if (chainsById.putIfAbsent(chain.id(), chain) != null) {
                throw new IllegalArgumentException("Duplicate chain id: " + chain.id());
            }
            return this;
        }

        public Builder chains(DialogChain... entries) {
            for (DialogChain chain : entries) {
                chain(chain);
            }
            return this;
        }

        public Builder scene(DialogScene scene) {
            Objects.requireNonNull(scene, "Scene is required.");
            if (scenesById.putIfAbsent(scene.id(), scene) != null) {
                throw new IllegalArgumentException("Duplicate scene id: " + scene.id());
            }
            return this;
        }

        public DialogTree build() {
            if (!chainsById.containsKey(rootChainId)) {
                throw new IllegalArgumentException("Root chain " + rootChainId + " is not registered.");
            }
            // Validate that every chain's sceneId references a registered scene.
            for (DialogChain chain : chainsById.values()) {
                if (chain.sceneId() != null && !scenesById.containsKey(chain.sceneId())) {
                    throw new IllegalArgumentException(
                            "Chain \"" + chain.id() + "\" references unknown scene \"" + chain.sceneId() + "\".");
                }
            }
            return new DialogTree(chainsById, scenesById, rootChainId);
        }
    }
}
