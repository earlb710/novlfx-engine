package com.eb.javafx.dialog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One sequence of {@link DialogNode dialog beats} that play together as a single unit.
 * Playback stops at the end of a chain — eligibility is determined by the engine's
 * {@link com.eb.javafx.storyline.StorylineEvent} requirement system, not the chain itself.
 *
 * <p>{@code characterRoleId} is the book-role identifier (see {@link
 * com.altlife.javafx.gamecontent.person.AltLifeBookRoles}) when this chain belongs to a specific
 * book character. Chains owned by the narrator or general story flow leave it {@code null}.</p>
 */
public record DialogChain(
        String id,
        String characterRoleId,
        String sceneId,
        int sequence,
        List<DialogNode> nodes,
        Set<String> tags,
        String phase,
        String conversationWith,
        String before,
        String after,
        List<String> locationUnlocks,
        List<String> roomUnlocks,
        String description,
        DialogLocationTrigger locationTrigger,
        DialogChoiceRequirement requires,
        String storyline,
        int step,
        List<String> phoneUnlocks) {

    /** Default sequence used when an author has not assigned one. Sorts above seq 1+ chains. */
    public static final int DEFAULT_SEQUENCE = 0;

    /** Matches a {@code .ch<digits>} segment in a chain id, e.g. {@code marsh.ch1.meet} → {@code 1}. */
    private static final Pattern CHAPTER_PATTERN = Pattern.compile("(?:^|\\.)ch(\\d+)(?:\\.|$)");

    public DialogChain {
        id = StoryStrings.requireKey(id, "Chain id is required.");
        characterRoleId = characterRoleId == null ? null : StoryStrings.requireKey(characterRoleId,
                "Character role id must be a valid key.");
        sceneId = sceneId == null ? null : StoryStrings.requireKey(sceneId, "Scene id must be a valid key.");
        nodes = List.copyOf(Objects.requireNonNull(nodes, "Nodes are required."));
        tags = Set.copyOf(Objects.requireNonNull(tags, "Tags are required."));
        phase = blankToNull(phase);
        conversationWith = conversationWith == null ? null
                : StoryStrings.requireKey(conversationWith, "conversationWith must be a valid key.");
        before = blankToNull(before);
        after = blankToNull(after);
        locationUnlocks = normalizeKeys(locationUnlocks, "locationUnlock");
        roomUnlocks = normalizeKeys(roomUnlocks, "roomUnlock");
        description = blankToNull(description);
        // requires nullable (no gate); storyline optional grouping; step orders within a storyline.
        storyline = blankToNull(storyline);
        step = Math.max(0, step);
        phoneUnlocks = normalizeKeys(phoneUnlocks, "phoneUnlock");
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Chain " + id + " must have at least one node.");
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (DialogNode node : nodes) {
            if (!ids.add(node.id())) {
                throw new IllegalArgumentException("Chain " + id + " has duplicate node id: " + node.id());
            }
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** Trims, drops blanks, and de-duplicates an unlock list into an immutable list (never null). */
    private static List<String> normalizeKeys(List<String> values, String label) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.trim());
            }
        }
        return List.copyOf(out);
    }

    /** Backward-compatible 10-arg constructor (no unlock lists / requires / storyline). */
    public DialogChain(String id, String characterRoleId, String sceneId, int sequence,
            List<DialogNode> nodes, Set<String> tags, String phase, String conversationWith,
            String before, String after) {
        this(id, characterRoleId, sceneId, sequence, nodes, tags, phase, conversationWith,
                before, after, List.of(), List.of(), null, null, null, null, 0, List.of());
    }

    /** Backward-compatible 14-arg constructor (no requires / storyline / step). */
    public DialogChain(String id, String characterRoleId, String sceneId, int sequence,
            List<DialogNode> nodes, Set<String> tags, String phase, String conversationWith,
            String before, String after, List<String> locationUnlocks, List<String> roomUnlocks,
            String description, DialogLocationTrigger locationTrigger) {
        this(id, characterRoleId, sceneId, sequence, nodes, tags, phase, conversationWith,
                before, after, locationUnlocks, roomUnlocks, description, locationTrigger, null, null, 0,
                List.of());
    }

    /** Chain-level availability gate, if declared (see {@link DialogChoiceRequirement}). */
    public Optional<DialogChoiceRequirement> requiresOpt() {
        return (requires == null || requires.isEmpty()) ? Optional.empty() : Optional.of(requires);
    }

    /** Storyline grouping id this chain belongs to (e.g. {@code "lena"}), if any. */
    public Optional<String> storylineOpt() {
        return Optional.ofNullable(storyline);
    }

    /** Free-text description appended to the location panel while this chain is running
     *  (empty when none was authored). */
    public Optional<String> descriptionText() {
        return Optional.ofNullable(description);
    }

    /** Location auto-trigger for this chain, if declared (see {@link DialogLocationTrigger}). */
    public Optional<DialogLocationTrigger> locationTriggerOpt() {
        return Optional.ofNullable(locationTrigger);
    }

    public Optional<String> bookRole() {
        return Optional.ofNullable(characterRoleId);
    }

    /** Story phase this chain plays in (setup / conversation / aftermath / pov-shift / …). */
    public Optional<String> phaseId() {
        return Optional.ofNullable(phase);
    }

    /** Book role this chain's conversation is held with, if any. */
    public Optional<String> conversationPartner() {
        return Optional.ofNullable(conversationWith);
    }

    /** Chain that precedes this one in author sequence, if declared. */
    public Optional<String> beforeChainId() {
        return Optional.ofNullable(before);
    }

    /** Chain that follows this one in author sequence, if declared. */
    public Optional<String> afterChainId() {
        return Optional.ofNullable(after);
    }

    /** Index of the node with {@code nodeId} in this chain, or {@code -1} when absent. */
    public int indexOf(String nodeId) {
        if (nodeId == null) {
            return -1;
        }
        String key = nodeId.trim();
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).id().equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /** The scene this chain belongs to, or empty if the chain has no scene parent. */
    public Optional<String> scene() {
        return Optional.ofNullable(sceneId);
    }

    /**
     * Returns the chapter number embedded in this chain's id, e.g. {@code marsh.ch1.meet} → {@code 1}.
     * Empty when the id has no {@code ch<digits>} segment.
     */
    public Optional<Integer> chapter() {
        return chapterOf(id);
    }

    /** Parses the {@code ch<digits>} segment out of any chain id. */
    public static Optional<Integer> chapterOf(String chainId) {
        if (chainId == null || chainId.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = CHAPTER_PATTERN.matcher(chainId);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public DialogNode node(int index) {
        return nodes.get(index);
    }

    public int size() {
        return nodes.size();
    }

    public boolean hasTag(String tag) {
        return tags.contains(Objects.requireNonNull(tag, "Tag is required."));
    }

    /** Background-music key this chain should play while it runs (overriding the location track),
     *  carried as a {@code music:<key>} tag. Empty when the chain declares no music. */
    public Optional<String> musicKeyOpt() {
        for (String tag : tags) {
            if (tag.startsWith("music:") && tag.length() > "music:".length()) {
                return Optional.of(tag.substring("music:".length()));
            }
        }
        return Optional.empty();
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** Mutable builder. Chains are constructed once and remain immutable thereafter. */
    public static final class Builder {
        private final String id;
        private String characterRoleId;
        private String sceneId;
        private int sequence = DEFAULT_SEQUENCE;
        private final List<DialogNode> nodes = new ArrayList<>();
        private final LinkedHashSet<String> tags = new LinkedHashSet<>();
        private String phase;
        private String conversationWith;
        private String before;
        private String after;
        private final List<String> locationUnlocks = new ArrayList<>();
        private final List<String> roomUnlocks = new ArrayList<>();
        private final List<String> phoneUnlocks = new ArrayList<>();
        private String description;
        private DialogLocationTrigger locationTrigger;
        private DialogChoiceRequirement requires;
        private String storyline;
        private int step = 0;

        private Builder(String id) {
            this.id = StoryStrings.requireKey(id, "Chain id is required.");
        }

        public Builder role(String characterRoleId) {
            this.characterRoleId = characterRoleId;
            return this;
        }

        public Builder scene(String sceneId) {
            this.sceneId = sceneId;
            return this;
        }

        public Builder sequence(int sequence) {
            this.sequence = sequence;
            return this;
        }

        public Builder tag(String tag) {
            tags.add(StoryStrings.requireKey(tag, "Tag must be a valid key."));
            return this;
        }

        /** Declares the background-music key this chain plays while it runs (overrides the location
         *  track). Stored as a {@code music:<key>} tag; see {@link DialogChain#musicKeyOpt()}. */
        public Builder music(String musicKey) {
            if (musicKey != null && !musicKey.isBlank()) {
                tag("music:" + musicKey.trim());
            }
            return this;
        }

        public Builder phase(String phase) {
            this.phase = phase;
            return this;
        }

        public Builder conversationWith(String conversationWith) {
            this.conversationWith = conversationWith;
            return this;
        }

        /** Building ids ({@code AltLifeMapLayout} {@code BUILDING_*}) this chain
         *  reveals on the world map when it plays. */
        public Builder locationUnlock(String... buildingIds) {
            if (buildingIds != null) {
                for (String id : buildingIds) {
                    locationUnlocks.add(id);
                }
            }
            return this;
        }

        /** Room keys ({@code buildingId|roomPath}) this chain marks discovered when it plays. */
        public Builder roomUnlock(String... roomKeys) {
            if (roomKeys != null) {
                for (String key : roomKeys) {
                    roomUnlocks.add(key);
                }
            }
            return this;
        }

        /** Book role ids whose phone number is revealed (the contact becomes dialable in the phone
         *  screen) when this chain plays. Applied by {@code AltLifeDialogUnlocks}. */
        public Builder phoneUnlock(String... roleIds) {
            if (roleIds != null) {
                for (String roleId : roleIds) {
                    phoneUnlocks.add(roleId);
                }
            }
            return this;
        }

        /** Free-text description appended to the location panel while this chain runs. */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /** Location auto-trigger for this chain (see {@link DialogLocationTrigger}). */
        public Builder locationTrigger(DialogLocationTrigger locationTrigger) {
            this.locationTrigger = locationTrigger;
            return this;
        }

        public Builder before(String before) {
            this.before = before;
            return this;
        }

        public Builder after(String after) {
            this.after = after;
            return this;
        }

        /** Chain-level availability gate (see {@link DialogChoiceRequirement}). */
        public Builder requires(DialogChoiceRequirement requires) {
            this.requires = requires;
            return this;
        }

        /** Storyline grouping + ordering for open-world sequencing. */
        public Builder storyline(String storyline, int step) {
            this.storyline = storyline;
            this.step = step;
            return this;
        }

        /** Adds a spoken line. The {@code nodeId} is auto-derived as {@code <chainId>.<index>}. */
        public Builder line(String speakerId, String text) {
            return line(autoNodeId(), speakerId, text);
        }

        public Builder line(String nodeId, String speakerId, String text) {
            nodes.add(new DialogNode.Line(nodeId, speakerId, text));
            return this;
        }

        /** Appends a pre-built node verbatim (used when merging chains). */
        public Builder node(DialogNode node) {
            nodes.add(java.util.Objects.requireNonNull(node, "node"));
            return this;
        }

        /** Adds a line with an explicit {@link DialogNode.Delivery}. */
        public Builder line(String nodeId, String speakerId, String text, DialogNode.Delivery delivery) {
            nodes.add(new DialogNode.Line(nodeId, speakerId, text, delivery));
            return this;
        }

        public Builder narration(String text) {
            return narration(autoNodeId(), text);
        }

        public Builder narration(String nodeId, String text) {
            nodes.add(DialogNode.Line.narration(nodeId, text));
            return this;
        }

        /** Adds an MC inner-thought line ({@code think} node). */
        public Builder think(String nodeId, String speakerId, String text) {
            nodes.add(DialogNode.Line.thought(nodeId, speakerId, text));
            return this;
        }

        /** Adds a third-person narrator line ({@code narrate} node). */
        public Builder narrate(String nodeId, String speakerId, String text) {
            nodes.add(DialogNode.Line.narrator(nodeId, speakerId, text));
            return this;
        }

        public Builder choice(String prompt, DialogChoice... options) {
            return choice(autoNodeId(), prompt, List.of(options));
        }

        public Builder choice(String nodeId, String prompt, DialogChoice... options) {
            return choice(nodeId, prompt, List.of(options));
        }

        public Builder choice(String nodeId, String prompt, List<DialogChoice> options) {
            nodes.add(new DialogNode.Choice(nodeId, prompt, options));
            return this;
        }

        public DialogChain build() {
            return new DialogChain(id, characterRoleId, sceneId, sequence, nodes, tags,
                    phase, conversationWith, before, after, locationUnlocks, roomUnlocks, description,
                    locationTrigger, requires, storyline, step, phoneUnlocks);
        }

        private String autoNodeId() {
            return id + "." + (nodes.size() + 1);
        }
    }
}
