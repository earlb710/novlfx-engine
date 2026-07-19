package com.eb.javafx.dialog;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.SimpleJson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads a {@link DialogTree} from a JSON string.
 *
 * <h3>Top-level format</h3>
 * <pre>{@code
 * {
 *   "rootChainId": "intro.wake",
 *   "scenes":  [ <scene>,  ... ],
 *   "chains":  [ <chain>,  ... ]
 * }
 * }</pre>
 *
 * <h3>Scene format</h3>
 * <pre>{@code
 * {
 *   "id":          "ch1.lab.morning",
 *   "title":       "The Lab — Early Morning",
 *   "description": "The recovery room is quiet. Pale light filters through frosted glass.",
 *   "background":  "img/lab-morning.jpg"
 * }
 * }</pre>
 * The {@code background} field is optional; all others are required.
 *
 * <h3>Chain format</h3>
 * <pre>{@code
 * {
 *   "id":       "marsh.intro",
 *   "role":     "marsh",
 *   "sceneId":  "ch1.lab.morning",
 *   "sequence": 10,
 *   "tags":     ["ch1"],
 *   "nodes":    [ <node>, ... ]
 * }
 * }</pre>
 * {@code role}, {@code sceneId}, {@code sequence}, and {@code tags} are optional.
 * {@code sceneId} must reference a scene declared in the same document.
 *
 * {@code role}, {@code sceneId}, {@code sequence}, {@code tags}, the sequencing fields
 * {@code phase}, {@code conversationWith}, {@code before}, {@code after}, the world-map
 * reveal arrays {@code locationUnlock} (building ids) and {@code roomUnlock} (room keys of the
 * form {@code buildingId|roomPath}), and {@code description} (free text appended to the
 * location panel while the chain runs) are all optional. A chain may also carry a
 * {@code locationTrigger} object — {@code {"building": id, "room"?: path, "repeatable"?: bool,
 * "requiredCharacter"?: bookRole, "missingPolicy"?: "skip"|"block", "moveBack"?: bool}} — that
 * auto-fires the chain when the MC enters the building (or the specific room), pulling the required
 * character into the MC's room first (see {@link DialogLocationTrigger}). Each {@code line}/
 * {@code think}/{@code narrate} node also accepts an optional {@code fullStory} code (default
 * {@code "N"}): {@code "N"} always shown, {@code "Y"} shown only when full-story text is on,
 * {@code "S"} a summary line shown only when full-story text is off.  A node may also carry a
 * {@code getItem} object — {@code {"itemId": id, "description"?: text}} — granting the player the
 * catalogued item (see {@code AltLifeItemCatalog}) when the line plays.  Finally, any
 * {@code line}/{@code think}/{@code narrate} node may carry two optional, editorial-only strings
 * ignored by playback: {@code reference} — the book {@code "chapter:paragraph"} the node was based
 * on (a soft pointer, re-linked by hand if the book moves) — and {@code comment}, a free-text
 * authoring note shown in the story editor. Both are blank-tolerant (an empty string is treated as
 * absent). See {@code BOOK_TO_GAME_DIALOG.md} §7.
 *
 * <h3>Node format</h3>
 * <pre>{@code
 * { "type": "line",    "id": "node.1", "speaker": "marsh", "text": "Hello." }
 * { "type": "think",   "id": "node.2", "speaker": "Kendric",   "text": "Careful woman." }
 * { "type": "narrate", "id": "node.3", "speaker": "narrator", "text": "She left the room." }
 * { "type": "choice",  "id": "node.4", "prompt": "What do you say?",
 *   "options": [ { "id": "yes", "label": "Yes" },
 *                { "id": "no",  "label": "No", "nextId": "node.7" } ] }
 * }</pre>
 * <ul>
 *   <li>{@code line} — spoken aloud ({@link DialogNode.Delivery#SPOKEN}). {@code speaker} optional
 *       (null = the MC's inner monologue).</li>
 *   <li>{@code think} — MC inner thought ({@link DialogNode.Delivery#THOUGHT}); {@code speaker}
 *       defaults to {@code "mc"}.</li>
 *   <li>{@code narrate} — third-person narrator ({@link DialogNode.Delivery#NARRATION});
 *       {@code speaker} defaults to {@code "narrator"}.</li>
 *   <li>{@code choice} — {@code prompt} optional; each option may carry an optional {@code nextId}
 *       (jump to a node id within THIS chain), an optional {@code followUp} (branch into ANOTHER
 *       chain by id — wins over {@code nextId}), and an optional {@code response} array of
 *       line/think/narrate nodes played right after selection.</li>
 * </ul>
 *
 * <p>Any {@code line}/{@code think}/{@code narrate} node may also carry movement fields that
 * move the MC as the line plays:</p>
 * <ul>
 *   <li>{@code changeRoom}     — (optional string) room path within the current building to move to
 *       (e.g. {@code "Reception > Lab Floor"}).</li>
 *   <li>{@code changeLocation} — (optional string) building id to move the MC to.</li>
 *   <li>{@code following} — (optional boolean, default {@code false}) when {@code true} the
 *       person the MC is currently speaking with accompanies him to the new room / building.</li>
 *   <li>{@code moveBack}  — (optional boolean, default {@code false}) when {@code true} the MC
 *       (and any following NPC) returns to their original position once the dialog chain
 *       finishes.</li>
 * </ul>
 * <p>At least one of {@code changeRoom} or {@code changeLocation} must be present for the move to
 * apply.  Both may be combined: move to {@code changeLocation} and land in {@code changeRoom}.</p>
 *
 * <p>Any {@code line}/{@code think}/{@code narrate} node (and any choice {@code response} line) may
 * also carry an {@code openScreen} string that pops a game screen open as the line plays:
 * {@code "inventory"} (MC sheet → Inventory tab), {@code "mcDetail"} (MC sheet → Stats tab),
 * {@code "npcDetail"} (the current conversation partner's sheet), or {@code "phone"}.  Unknown
 * values are a parse error.  Applied on the talk-to / fragment playback path (the gameplay hub);
 * a no-op where no hub overlay exists, e.g. the opening cutscene player.</p>
 */
public final class DialogJsonLoader {

    /** Default speaker id assigned to {@code think} nodes that omit an explicit speaker. */
    private static final String DEFAULT_THINK_SPEAKER = "mc";
    /** Default speaker id assigned to {@code narrate} nodes that omit an explicit speaker. */
    private static final String DEFAULT_NARRATE_SPEAKER = "narrator";

    /** Parses {@code json} and returns the resulting {@link DialogTree}. */
    public DialogTree load(String json) {
        return load(json, "dialog-tree");
    }

    /** Parses {@code json} with the given {@code sourceName} in error messages. */
    public DialogTree load(String json, String sourceName) {
        Objects.requireNonNull(json, "json is required.");
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        String rootChainId = JsonData.requiredString(root, "rootChainId", "rootChainId");

        DialogTree.Builder builder = DialogTree.builder(rootChainId);

        // scenes (optional array)
        List<Object> scenesArray = JsonData.optionalList(root, "scenes", "scenes");
        for (int i = 0; i < scenesArray.size(); i++) {
            String path = "scenes[" + i + "]";
            Map<String, Object> obj = JsonData.requireObject(scenesArray.get(i), path);
            builder.scene(parseScene(obj, path));
        }

        // chains (required array)
        List<Object> chainsArray = JsonData.requiredList(root, "chains", "chains");
        for (int i = 0; i < chainsArray.size(); i++) {
            String path = "chains[" + i + "]";
            Map<String, Object> obj = JsonData.requireObject(chainsArray.get(i), path);
            builder.chain(parseChain(obj, path));
        }

        return builder.build();
    }

    private static DialogScene parseScene(Map<String, Object> obj, String path) {
        String id = JsonData.requiredString(obj, "id", path + ".id");
        String title = JsonData.requiredString(obj, "title", path + ".title");
        String description = JsonData.requiredString(obj, "description", path + ".description");
        String background = JsonData.optionalString(obj, "background", path + ".background").orElse(null);
        return new DialogScene(id, title, description, background);
    }

    private static DialogChain parseChain(Map<String, Object> obj, String path) {
        String id = JsonData.requiredString(obj, "id", path + ".id");
        String role = JsonData.optionalString(obj, "role", path + ".role").orElse(null);
        String sceneId = JsonData.optionalString(obj, "sceneId", path + ".sceneId").orElse(null);
        int sequence = optionalInt(obj, "sequence", DialogChain.DEFAULT_SEQUENCE, path + ".sequence");

        String phase = JsonData.optionalString(obj, "phase", path + ".phase").orElse(null);
        String conversationWith = JsonData.optionalString(obj, "conversationWith", path + ".conversationWith").orElse(null);
        String before = JsonData.optionalString(obj, "before", path + ".before").orElse(null);
        String after = JsonData.optionalString(obj, "after", path + ".after").orElse(null);
        String description = JsonData.optionalString(obj, "description", path + ".description").orElse(null);

        DialogChain.Builder builder = DialogChain.builder(id)
                .role(role)
                .scene(sceneId)
                .sequence(sequence)
                .phase(phase)
                .conversationWith(conversationWith)
                .before(before)
                .after(after)
                .description(description);

        // Optional background-music override: the track that plays while this chain runs,
        // overriding the location's music until the conversation ends.
        JsonData.optionalString(obj, "musicKey", path + ".musicKey")
                .filter(s -> !s.isBlank())
                .ifPresent(builder::music);

        // Open-world sequencing: optional chain-level availability gate + storyline grouping.
        DialogChoiceRequirement chainRequires = parseRequirement(obj, path);
        if (chainRequires != null) {
            builder.requires(chainRequires);
        }
        JsonData.optionalString(obj, "storyline", path + ".storyline")
                .filter(s -> !s.isBlank())
                .ifPresent(s -> builder.storyline(s, optionalInt(obj, "step", 0, path + ".step")));

        for (String tag : JsonData.optionalStringList(obj, "tags", path + ".tags")) {
            builder.tag(tag);
        }

        // World-map reveals: buildings ("locationUnlock") and rooms ("roomUnlock", keyed
        // "buildingId|roomPath") this chain unlocks when it plays.
        for (String buildingId : JsonData.optionalStringList(obj, "locationUnlock", path + ".locationUnlock")) {
            builder.locationUnlock(buildingId);
        }
        for (String roomKey : JsonData.optionalStringList(obj, "roomUnlock", path + ".roomUnlock")) {
            builder.roomUnlock(roomKey);
        }
        // Phone-number reveals ("phoneUnlock"): book role ids whose contact number becomes known
        // (dialable in the phone screen) when this chain plays.
        for (String roleId : JsonData.optionalStringList(obj, "phoneUnlock", path + ".phoneUnlock")) {
            builder.phoneUnlock(roleId);
        }

        // Optional location auto-trigger: fires the chain when the MC enters a building (or a
        // specific room), pulling a required character into the MC's room.
        JsonData.optionalObject(obj, "locationTrigger", path + ".locationTrigger").ifPresent(trig -> {
            String building = JsonData.requiredString(trig, "building", path + ".locationTrigger.building");
            String room = JsonData.optionalString(trig, "room", path + ".locationTrigger.room").orElse(null);
            boolean repeatable = JsonData.optionalBoolean(trig, "repeatable", false,
                    path + ".locationTrigger.repeatable");
            String requiredCharacter = JsonData.optionalString(trig, "requiredCharacter",
                    path + ".locationTrigger.requiredCharacter").orElse(null);
            DialogLocationTrigger.MissingPolicy policy = DialogLocationTrigger.MissingPolicy.fromString(
                    JsonData.optionalString(trig, "missingPolicy", path + ".locationTrigger.missingPolicy")
                            .orElse(null));
            boolean moveBack = JsonData.optionalBoolean(trig, "moveBack", false,
                    path + ".locationTrigger.moveBack");
            builder.locationTrigger(new DialogLocationTrigger(
                    building, room, repeatable, requiredCharacter, policy, moveBack));
        });

        List<Object> nodes = JsonData.requiredList(obj, "nodes", path + ".nodes");
        for (int i = 0; i < nodes.size(); i++) {
            String nodePath = path + ".nodes[" + i + "]";
            Map<String, Object> nodeObj = JsonData.requireObject(nodes.get(i), nodePath);
            parseNode(nodeObj, nodePath, builder);
        }

        return builder.build();
    }

    private static void parseNode(Map<String, Object> obj, String path, DialogChain.Builder builder) {
        String type = JsonData.requiredString(obj, "type", path + ".type");
        String nodeId = JsonData.requiredString(obj, "id", path + ".id");
        // Optional per-line story-detail code: "N" always shown, "Y" full-story-only, "S"
        // summary-only (default "N").
        DialogNode.StoryDetail storyDetail = DialogNode.StoryDetail.fromCode(
                JsonData.optionalString(obj, "fullStory", path + ".fullStory").orElse(null));
        // Optional item grant: {"itemId": ..., "description"?: ...} — the player gets the catalogue
        // item when this line plays.
        DialogGetItem getItem = JsonData.optionalObject(obj, "getItem", path + ".getItem")
                .map(g -> new DialogGetItem(
                        JsonData.requiredString(g, "itemId", path + ".getItem.itemId"),
                        JsonData.optionalString(g, "description", path + ".getItem.description").orElse(null)))
                .orElse(null);
        // Optional movement instruction: changeRoom / changeLocation / following
        // move the MC (and optionally his conversation partner) as the line plays.
        DialogMoveAction moveAction = parseMoveAction(obj, path);
        // Optional screen pop-open: openScreen is a game-defined key (e.g. "inventory").
        String openScreen = parseOpenScreen(obj, path);
        // Editorial metadata (ignored by playback): the book "chapter:paragraph" this node was based
        // on, plus a free-text authoring comment. Both optional and blank-tolerant (an empty string
        // is a not-yet-filled field, not an error).
        String reference = optionalText(obj, "reference");
        String comment = optionalText(obj, "comment");
        switch (type) {
            case "line" -> {
                String speaker = JsonData.optionalString(obj, "speaker", path + ".speaker").orElse(null);
                String text = JsonData.requiredString(obj, "text", path + ".text");
                builder.node(new DialogNode.Line(nodeId, speaker, text, DialogNode.Delivery.SPOKEN, storyDetail, getItem, moveAction, reference, comment).withOpenScreen(openScreen));
            }
            case "think" -> {
                // MC inner monologue. Defaults the speaker to the main character when unspecified.
                String speaker = JsonData.optionalString(obj, "speaker", path + ".speaker").orElse(DEFAULT_THINK_SPEAKER);
                String text = JsonData.requiredString(obj, "text", path + ".text");
                builder.node(new DialogNode.Line(nodeId, speaker, text, DialogNode.Delivery.THOUGHT, storyDetail, getItem, moveAction, reference, comment).withOpenScreen(openScreen));
            }
            case "narrate" -> {
                // Third-person narrator voice. Defaults the speaker to the narrator when unspecified.
                String speaker = JsonData.optionalString(obj, "speaker", path + ".speaker").orElse(DEFAULT_NARRATE_SPEAKER);
                String text = JsonData.requiredString(obj, "text", path + ".text");
                builder.node(new DialogNode.Line(nodeId, speaker, text, DialogNode.Delivery.NARRATION, storyDetail, getItem, moveAction, reference, comment).withOpenScreen(openScreen));
            }
            case "choice" -> {
                String prompt = JsonData.optionalString(obj, "prompt", path + ".prompt").orElse(null);
                List<Object> options = JsonData.requiredList(obj, "options", path + ".options");
                List<DialogChoice> choices = new ArrayList<>(options.size());
                for (int i = 0; i < options.size(); i++) {
                    String optPath = path + ".options[" + i + "]";
                    Map<String, Object> optObj = JsonData.requireObject(options.get(i), optPath);
                    String optId    = JsonData.requiredString(optObj, "id",    optPath + ".id");
                    String label    = JsonData.requiredString(optObj, "label", optPath + ".label");
                    String nextId   = JsonData.optionalString(optObj, "nextId", optPath + ".nextId").orElse(null);
                    String followUp = JsonData.optionalString(optObj, "followUp", optPath + ".followUp").orElse(null);
                    List<DialogNode.Line> responseLines = parseResponseLines(optObj, optPath);
                    DialogChoiceRequirement requirement = parseRequirement(optObj, optPath);
                    choices.add(new DialogChoice(optId, label, nextId, responseLines, followUp, requirement));
                }
                builder.choice(nodeId, prompt, choices);
            }
            default -> throw new IllegalArgumentException(
                    "Unknown node type \"" + type + "\" at " + path + ".");
        }
    }

    /** Parses the optional {@code requires} object on a choice option into a
     *  {@link DialogChoiceRequirement}, or {@code null} when absent / empty.  Keys of the form
     *  {@code min<Stat>} / {@code max<Stat>} (e.g. {@code minLove}, {@code maxFear}) become stat
     *  thresholds; {@code requiresItem} (string) or {@code requiresItems} (array) become item
     *  requirements; {@code reason} (string) and {@code hideWhenUnmet} (boolean) control
     *  presentation. */
    private static DialogChoiceRequirement parseRequirement(Map<String, Object> optObj, String optPath) {
        var reqOpt = JsonData.optionalObject(optObj, "requires", optPath + ".requires");
        if (reqOpt.isEmpty()) {
            return null;
        }
        Map<String, Object> req = reqOpt.get();
        java.util.Map<String, Integer> minStats = new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> maxStats = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> e : req.entrySet()) {
            String key = e.getKey();
            if (!(e.getValue() instanceof Number n)) {
                continue;
            }
            if (key.length() > 3 && key.regionMatches(true, 0, "min", 0, 3)) {
                minStats.put(key.substring(3).toLowerCase(java.util.Locale.ROOT), n.intValue());
            } else if (key.length() > 3 && key.regionMatches(true, 0, "max", 0, 3)) {
                maxStats.put(key.substring(3).toLowerCase(java.util.Locale.ROOT), n.intValue());
            }
        }
        java.util.List<String> items = new java.util.ArrayList<>(
                JsonData.optionalStringList(req, "requiresItems", optPath + ".requires.requiresItems"));
        JsonData.optionalString(req, "requiresItem", optPath + ".requires.requiresItem")
                .filter(s -> !s.isBlank()).ifPresent(items::add);

        // Story flags: "flag"/"flags" (all required); "notFlag"/"notFlags" (none may be set).
        java.util.List<String> requiresFlags = new java.util.ArrayList<>(
                JsonData.optionalStringList(req, "flags", optPath + ".requires.flags"));
        JsonData.optionalString(req, "flag", optPath + ".requires.flag")
                .filter(s -> !s.isBlank()).ifPresent(requiresFlags::add);
        java.util.List<String> forbidsFlags = new java.util.ArrayList<>(
                JsonData.optionalStringList(req, "notFlags", optPath + ".requires.notFlags"));
        JsonData.optionalString(req, "notFlag", optPath + ".requires.notFlag")
                .filter(s -> !s.isBlank()).ifPresent(forbidsFlags::add);

        // Prior choices: "choiceMade" (single object) and/or "choicesMade" (array of objects),
        // each { "chain": ..., "node": ..., "option": ... }.
        java.util.List<String[]> requiresChoices = new java.util.ArrayList<>();
        JsonData.optionalObject(req, "choiceMade", optPath + ".requires.choiceMade")
                .map(o -> parseChoiceMade(o, optPath + ".requires.choiceMade"))
                .ifPresent(requiresChoices::add);
        for (Object o : JsonData.optionalList(req, "choicesMade", optPath + ".requires.choicesMade")) {
            Map<String, Object> cm = JsonData.requireObject(o, optPath + ".requires.choicesMade[]");
            requiresChoices.add(parseChoiceMade(cm, optPath + ".requires.choicesMade[]"));
        }

        // State-path predicates: "state": [ { "path": "mc.energy", "min": 50 }, ... ] — each object
        // carries a path plus exactly one op key (min/max/eq/ne/has). See DialogChoiceRequirement.
        java.util.List<DialogChoiceRequirement.StateCondition> requiresState = new java.util.ArrayList<>();
        for (Object o : JsonData.optionalList(req, "state", optPath + ".requires.state")) {
            Map<String, Object> cond = JsonData.requireObject(o, optPath + ".requires.state[]");
            String p = JsonData.requiredString(cond, "path", optPath + ".requires.state[].path");
            DialogChoiceRequirement.StateCondition sc = parseStateCondition(p, cond);
            if (sc != null) {
                requiresState.add(sc);
            }
        }

        String reason = JsonData.optionalString(req, "reason", optPath + ".requires.reason").orElse(null);
        boolean hideWhenUnmet = JsonData.optionalBoolean(req, "hideWhenUnmet", false,
                optPath + ".requires.hideWhenUnmet");
        DialogChoiceRequirement requirement = new DialogChoiceRequirement(
                minStats, maxStats, items, requiresFlags, forbidsFlags, requiresChoices,
                requiresState, reason, hideWhenUnmet);
        return requirement.isEmpty() ? null : requirement;
    }

    /** Parses one state-condition object into a {@link DialogChoiceRequirement.StateCondition}, or
     *  {@code null} when it carries no recognised op key. */
    private static DialogChoiceRequirement.StateCondition parseStateCondition(
            String path, Map<String, Object> cond) {
        if (cond.containsKey("min")) {
            return new DialogChoiceRequirement.StateCondition(
                    path, DialogChoiceRequirement.StateCondition.Op.MIN, cond.get("min"));
        }
        if (cond.containsKey("max")) {
            return new DialogChoiceRequirement.StateCondition(
                    path, DialogChoiceRequirement.StateCondition.Op.MAX, cond.get("max"));
        }
        if (cond.containsKey("eq")) {
            return new DialogChoiceRequirement.StateCondition(
                    path, DialogChoiceRequirement.StateCondition.Op.EQ, cond.get("eq"));
        }
        if (cond.containsKey("ne")) {
            return new DialogChoiceRequirement.StateCondition(
                    path, DialogChoiceRequirement.StateCondition.Op.NE, cond.get("ne"));
        }
        if (cond.containsKey("has")) {
            return new DialogChoiceRequirement.StateCondition(
                    path, DialogChoiceRequirement.StateCondition.Op.HAS, cond.get("has"));
        }
        return null;
    }

    /** Parses a {@code {chain, node, option}} object into a {@code [chain, node, option]} triple. */
    private static String[] parseChoiceMade(Map<String, Object> obj, String path) {
        return new String[]{
                JsonData.requiredString(obj, "chain", path + ".chain"),
                JsonData.requiredString(obj, "node", path + ".node"),
                JsonData.requiredString(obj, "option", path + ".option")};
    }

    /** Parses the optional {@code response} array on a choice option into a list of
     *  {@link DialogNode.Line}s.  Returns an empty list when the field is absent. */
    @SuppressWarnings("unchecked")
    private static List<DialogNode.Line> parseResponseLines(Map<String, Object> optObj,
                                                             String optPath) {
        List<Object> raw = JsonData.optionalList(optObj, "response", optPath + ".response");
        if (raw.isEmpty()) return List.of();
        // Stable, key-safe id stem for response lines that omit an explicit id.  Node ids must be
        // valid keys (letters/digits/.-_, no brackets), so derive from the option id rather than
        // the bracketed JSON path.
        String optionId = JsonData.optionalString(optObj, "id", optPath + ".id").orElse("opt");
        List<DialogNode.Line> out = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            String linePath = optPath + ".response[" + i + "]";
            Map<String, Object> lineObj = JsonData.requireObject(raw.get(i), linePath);
            String type = JsonData.requiredString(lineObj, "type", linePath + ".type");
            String lineId = JsonData.optionalString(lineObj, "id", linePath + ".id")
                    .orElse(optionId + ".response." + i);
            DialogNode.StoryDetail storyDetail = DialogNode.StoryDetail.fromCode(
                    JsonData.optionalString(lineObj, "fullStory", linePath + ".fullStory").orElse(null));
            DialogGetItem getItem = JsonData.optionalObject(lineObj, "getItem", linePath + ".getItem")
                    .map(g -> new DialogGetItem(
                            JsonData.requiredString(g, "itemId", linePath + ".getItem.itemId"),
                            JsonData.optionalString(g, "description", linePath + ".getItem.description").orElse(null)))
                    .orElse(null);
            DialogMoveAction moveAction = parseMoveAction(lineObj, linePath);
            String openScreen = parseOpenScreen(lineObj, linePath);
            String reference = optionalText(lineObj, "reference");
            String comment = optionalText(lineObj, "comment");
            DialogNode.Line line = switch (type) {
                case "line" -> {
                    String speaker = JsonData.optionalString(lineObj, "speaker", linePath + ".speaker").orElse(null);
                    String text    = JsonData.requiredString(lineObj, "text", linePath + ".text");
                    yield new DialogNode.Line(lineId, speaker, text, DialogNode.Delivery.SPOKEN, storyDetail, getItem, moveAction, reference, comment);
                }
                case "think" -> {
                    String speaker = JsonData.optionalString(lineObj, "speaker", linePath + ".speaker").orElse(DEFAULT_THINK_SPEAKER);
                    String text    = JsonData.requiredString(lineObj, "text", linePath + ".text");
                    yield new DialogNode.Line(lineId, speaker, text, DialogNode.Delivery.THOUGHT, storyDetail, getItem, moveAction, reference, comment);
                }
                case "narrate" -> {
                    String speaker = JsonData.optionalString(lineObj, "speaker", linePath + ".speaker").orElse(DEFAULT_NARRATE_SPEAKER);
                    String text    = JsonData.requiredString(lineObj, "text", linePath + ".text");
                    yield new DialogNode.Line(lineId, speaker, text, DialogNode.Delivery.NARRATION, storyDetail, getItem, moveAction, reference, comment);
                }
                default -> throw new IllegalArgumentException(
                        "Unknown response line type \"" + type + "\" at " + linePath + ".");
            };
            out.add(line.withOpenScreen(openScreen));
        }
        return List.copyOf(out);
    }

    /** Reads the optional {@code openScreen} field — an opaque, game-defined screen key
     *  (e.g. {@code "inventory"}) — returning the trimmed token, or {@code null} when absent /
     *  blank. The engine does not validate the key; the game maps it to a screen. */
    private static String parseOpenScreen(Map<String, Object> obj, String path) {
        return JsonData.optionalString(obj, "openScreen", path + ".openScreen")
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElse(null);
    }

    /** Reads optional {@code changeRoom}, {@code changeLocation}, and {@code following} fields
     *  from a node object, returning a {@link DialogMoveAction} when at least one destination
     *  is present, or {@code null} when neither field is set. */
    private static DialogMoveAction parseMoveAction(Map<String, Object> obj, String path) {
        String changeLocation = JsonData.optionalString(obj, "changeLocation", path + ".changeLocation").orElse(null);
        String changeRoom     = JsonData.optionalString(obj, "changeRoom",     path + ".changeRoom").orElse(null);
        boolean hasLocation = changeLocation != null && !changeLocation.isBlank();
        boolean hasRoom     = changeRoom     != null && !changeRoom.isBlank();
        if (!hasLocation && !hasRoom) {
            return null;
        }
        boolean following = JsonData.optionalBoolean(obj, "following", false, path + ".following");
        boolean moveBack  = JsonData.optionalBoolean(obj, "moveBack",  false, path + ".moveBack");
        return new DialogMoveAction(
                hasLocation ? changeLocation.trim() : null,
                hasRoom     ? changeRoom.trim()     : null,
                following,
                moveBack);
    }

    /** Reads an optional, blank-tolerant string field: returns {@code null} for absent, null, or
     *  blank values (so an empty editorial {@code reference}/{@code comment} is a not-yet-filled
     *  field rather than a parse error), otherwise the trimmed text. */
    private static String optionalText(Map<String, Object> obj, String key) {
        Object value = obj.get(key);
        if (!(value instanceof String s) || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    /** Reads an optional integer field, returning {@code defaultValue} when absent. */
    private static int optionalInt(Map<String, Object> obj, String key, int defaultValue, String description) {
        if (!obj.containsKey(key) || obj.get(key) == null) {
            return defaultValue;
        }
        Object value = obj.get(key);
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        throw new IllegalArgumentException("Expected JSON number for " + description + ".");
    }
}
