package com.eb.javafx.dialog;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One beat in a {@link DialogChain}. A node is either a spoken {@link Line} or a player {@link
 * Choice} prompt. Choice nodes pause playback until the player selects one of the options.
 */
public sealed interface DialogNode permits DialogNode.Line, DialogNode.Choice {

    String id();

    /**
     * How a {@link Line} is delivered — drives speaker resolution and (future) text styling.
     * <ul>
     *   <li>{@link #SPOKEN} — said aloud. The default for plain {@code line} nodes.</li>
     *   <li>{@link #THOUGHT} — the main character's internal monologue ({@code think} nodes).
     *       Still attributed to the MC, but flagged so the UI can italicise it.</li>
     *   <li>{@link #NARRATION} — third-person narrator voice ({@code narrate} nodes), distinct
     *       from the MC's own inner voice.</li>
     * </ul>
     */
    enum Delivery { SPOKEN, THOUGHT, NARRATION }

    /**
     * How much of the story a line belongs to, controlling whether it shows under the player's
     * "full story" detail preference.  Persisted in JSON as the single-letter {@code fullStory}
     * code.
     * <ul>
     *   <li>{@link #NORMAL} ({@code "N"}) — important dialog, always shown in both modes.</li>
     *   <li>{@link #FULL} ({@code "Y"}) — extra flavour, shown only when full-story text is on.</li>
     *   <li>{@link #SUMMARY} ({@code "S"}) — a condensed line, shown only when full-story text is
     *       off (the shorter "game version").</li>
     * </ul>
     */
    enum StoryDetail {
        NORMAL("N"), FULL("Y"), SUMMARY("S");

        private final String code;

        StoryDetail(String code) {
            this.code = code;
        }

        /** The single-letter JSON code ({@code "N"} / {@code "Y"} / {@code "S"}). */
        public String code() {
            return code;
        }

        /**
         * Parses a code into a {@link StoryDetail}, tolerating case, blanks (→ {@link #NORMAL}),
         * and the legacy boolean form ({@code true}→{@link #FULL}, {@code false}→{@link #NORMAL}).
         */
        public static StoryDetail fromCode(String value) {
            if (value == null || value.isBlank()) {
                return NORMAL;
            }
            String v = value.trim();
            for (StoryDetail d : values()) {
                if (d.code.equalsIgnoreCase(v)) {
                    return d;
                }
            }
            if ("true".equalsIgnoreCase(v)) {
                return FULL;
            }
            if ("false".equalsIgnoreCase(v)) {
                return NORMAL;
            }
            throw new IllegalArgumentException("Unknown fullStory code: " + value + " (expected Y/N/S).");
        }
    }

    /**
     * A line attributed to a {@code speakerId} that resolves through
     * {@code AltLifeDialogSpeakers}, carrying a
     * {@link Delivery} that says whether it is spoken aloud, an inner thought, or narration, a
     * {@link StoryDetail} controlling whether it shows under the full-story preference, and an
     * optional {@code openScreen} key (a free-form, game-defined screen identifier such as
     * {@code "inventory"}) popped open as the line plays. The engine treats the key as opaque; the
     * game maps it to an actual screen (see {@code AltLifeMainAppLayout#applyDialogOpenScreen}).
     */
    record Line(String id, String speakerId, String text, Delivery delivery, StoryDetail storyDetail,
            DialogGetItem getItem, DialogMoveAction moveAction, String reference, String comment,
            String openScreen)
            implements DialogNode {
        public Line {
            id = StoryStrings.requireKey(id, "Dialog node id is required.");
            speakerId = speakerId == null ? null : StoryStrings.requireKey(speakerId, "Speaker id must be a valid key.");
            text = StoryStrings.requireText(text, "Dialog line text is required.");
            delivery = delivery == null ? Delivery.SPOKEN : delivery;
            storyDetail = storyDetail == null ? StoryDetail.NORMAL : storyDetail;
            // moveAction nullable — most lines don't move the MC
            // Editorial metadata, ignored by playback: {@code reference} is the book
            // "chapter:paragraph" this line was based on (re-linked by hand if the book moves);
            // {@code comment} is a free-text authoring note. Blanks normalise to null.
            reference = (reference == null || reference.isBlank()) ? null : reference.trim();
            comment = (comment == null || comment.isBlank()) ? null : comment.trim();
            // openScreen is an opaque game-screen key; keep the author's token, blanks → null.
            openScreen = (openScreen == null || openScreen.isBlank()) ? null : openScreen.trim();
        }

        /** Backward-compatible 9-arg form (no openScreen) — the prior canonical shape. */
        public Line(String id, String speakerId, String text, Delivery delivery, StoryDetail storyDetail,
                DialogGetItem getItem, DialogMoveAction moveAction, String reference, String comment) {
            this(id, speakerId, text, delivery, storyDetail, getItem, moveAction, reference, comment, null);
        }

        /** Backward-compatible 7-arg form (no editorial reference / comment). */
        public Line(String id, String speakerId, String text, Delivery delivery, StoryDetail storyDetail,
                DialogGetItem getItem, DialogMoveAction moveAction) {
            this(id, speakerId, text, delivery, storyDetail, getItem, moveAction, null, null);
        }

        /** Backward-compatible 6-arg form (no move action). */
        public Line(String id, String speakerId, String text, Delivery delivery, StoryDetail storyDetail,
                DialogGetItem getItem) {
            this(id, speakerId, text, delivery, storyDetail, getItem, null);
        }

        /** Backward-compatible 5-arg form (no item grant, no move action). */
        public Line(String id, String speakerId, String text, Delivery delivery, StoryDetail storyDetail) {
            this(id, speakerId, text, delivery, storyDetail, null, null);
        }

        /** Backward-compatible 4-arg form (normal story detail). */
        public Line(String id, String speakerId, String text, Delivery delivery) {
            this(id, speakerId, text, delivery, StoryDetail.NORMAL, null, null);
        }

        /** Spoken line (backward-compatible 3-arg form). */
        public Line(String id, String speakerId, String text) {
            this(id, speakerId, text, Delivery.SPOKEN, StoryDetail.NORMAL, null, null);
        }

        /** Item granted when this line plays, if any (see {@link DialogGetItem}). */
        public Optional<DialogGetItem> grantedItem() {
            return Optional.ofNullable(getItem);
        }

        /** Movement instruction carried by this line wrapped in Optional, if any (see {@link DialogMoveAction}).
         *  Use {@link #moveAction()} for the raw nullable value. */
        public Optional<DialogMoveAction> optionalMoveAction() {
            return Optional.ofNullable(moveAction);
        }

        /** MC inner monologue with no explicit speaker — rendered as the main character. */
        public static Line narration(String id, String text) {
            return new Line(id, null, text, Delivery.SPOKEN, StoryDetail.NORMAL);
        }

        /** MC inner thought attributed to {@code speakerId} (typically the MC). */
        public static Line thought(String id, String speakerId, String text) {
            return new Line(id, speakerId, text, Delivery.THOUGHT, StoryDetail.NORMAL);
        }

        /** Third-person narrator line. */
        public static Line narrator(String id, String speakerId, String text) {
            return new Line(id, speakerId, text, Delivery.NARRATION, StoryDetail.NORMAL);
        }

        /** Returns a copy of this line with the {@link StoryDetail} set as given. */
        public Line withStoryDetail(StoryDetail detail) {
            return new Line(id, speakerId, text, delivery, detail, getItem, moveAction, reference, comment, openScreen);
        }

        /** Returns a copy of this line with the given {@link DialogMoveAction} attached. */
        public Line withMoveAction(DialogMoveAction action) {
            return new Line(id, speakerId, text, delivery, storyDetail, getItem, action, reference, comment, openScreen);
        }

        /** Returns a copy with the editorial {@code reference} (book "chapter:paragraph") set. */
        public Line withReference(String newReference) {
            return new Line(id, speakerId, text, delivery, storyDetail, getItem, moveAction, newReference, comment, openScreen);
        }

        /** Returns a copy with the editorial {@code comment} set. */
        public Line withComment(String newComment) {
            return new Line(id, speakerId, text, delivery, storyDetail, getItem, moveAction, reference, newComment, openScreen);
        }

        /** Returns a copy of this line with the given {@link DialogGetItem} grant attached. */
        public Line withGetItem(DialogGetItem item) {
            return new Line(id, speakerId, text, delivery, storyDetail, item, moveAction, reference, comment, openScreen);
        }

        /** Returns a copy with the given {@code openScreen} key attached ({@code null}/blank → returns this). */
        public Line withOpenScreen(String screen) {
            return (screen == null || screen.isBlank()) ? this
                    : new Line(id, speakerId, text, delivery, storyDetail, getItem, moveAction, reference, comment, screen);
        }

        /** The game-defined key of the screen this line pops open as it plays, if any. */
        public Optional<String> screenToOpen() {
            return Optional.ofNullable(openScreen);
        }

        /** The book "chapter:paragraph" reference this line was based on, if recorded. */
        public Optional<String> referenceText() {
            return Optional.ofNullable(reference);
        }

        /** The editorial comment on this line, if any. */
        public Optional<String> commentText() {
            return Optional.ofNullable(comment);
        }

        public boolean isThought() {
            return delivery == Delivery.THOUGHT;
        }

        public boolean isNarration() {
            return delivery == Delivery.NARRATION;
        }
    }

    /**
     * A player choice prompt. {@code prompt} is the text shown above the options (may be empty
     * to render just the buttons). Selecting an option records the choice on the {@link StoryLine}
     * and advances past this node.
     */
    record Choice(String id, String prompt, List<DialogChoice> options) implements DialogNode {
        public Choice {
            id = StoryStrings.requireKey(id, "Dialog node id is required.");
            prompt = prompt == null ? "" : prompt.trim();
            options = List.copyOf(Objects.requireNonNull(options, "Choice options are required."));
            if (options.isEmpty()) {
                throw new IllegalArgumentException("Choice node " + id + " must define at least one option.");
            }
            long distinct = options.stream().map(DialogChoice::id).distinct().count();
            if (distinct != options.size()) {
                throw new IllegalArgumentException("Choice node " + id + " has duplicate option ids.");
            }
        }

        public DialogChoice option(String choiceId) {
            String required = StoryStrings.requireKey(choiceId, "Choice id is required.");
            return options.stream()
                    .filter(option -> option.hasId(required))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown choice option " + choiceId + " on node " + id));
        }
    }
}
