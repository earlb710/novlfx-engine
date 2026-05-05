package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;
import com.eb.javafx.util.UtilString;

import java.util.List;
import java.util.Locale;

/**
 * Data-only authored conversation document using the LR2Alt exported JSON shape.
 *
 * <p>The schema mirrors authored conversation files with a top-level {@code name}, {@code language}, and ordered
 * {@code conversations}; each conversation has typed dialogue {@code lines}, and each line has a speaker plus one
 * or more text variants that can also serve as choices.</p>
 */
public final class ConversationDefinition {
    private final String name;
    private final String language;
    private final List<ConversationBlock> conversations;

    public ConversationDefinition(String name, String language, List<ConversationBlock> conversations) {
        this.name = Validation.requireNonBlank(name, "Conversation name is required.");
        this.language = Validation.requireNonBlank(language, "Conversation language is required.");
        this.conversations = List.copyOf(Validation.requireNonEmpty(conversations, "Conversation document requires conversations."));
    }

    public String name() {
        return name;
    }

    public String language() {
        return language;
    }

    public List<ConversationBlock> conversations() {
        return conversations;
    }

    public record ConversationBlock(String id, String description, List<ConversationLine> lines) {
        public ConversationBlock {
            id = Validation.requireNonBlank(id, "Conversation id is required.");
            description = Validation.requireNonBlank(description, "Conversation description is required.");
            lines = List.copyOf(Validation.requireNonEmpty(lines, "Conversation lines are required."));
        }
    }

    public enum LineType {
        SHOUT("shout"),
        SAY("say"),
        WHISPER("whisper"),
        CHOICE("choice");

        private final String jsonValue;

        LineType(String jsonValue) {
            this.jsonValue = jsonValue;
        }

        public String jsonValue() {
            return jsonValue;
        }

        public static LineType fromJson(String value) {
            String checkedValue = Validation.requireNonBlank(value, "Conversation line type is required.").trim();
            for (LineType type : values()) {
                if (type.jsonValue.equals(checkedValue)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown conversation line type: " + value);
        }

        public String formatText(String text) {
            String checkedText = Validation.requireNonNull(text, "Conversation line text is required.");
            return switch (this) {
                case SHOUT -> "<b>" + UtilString.escapeXML(checkedText.toUpperCase(Locale.ROOT)) + "</b>";
                case SAY, CHOICE -> UtilString.escapeXML(checkedText);
                case WHISPER -> "<i>" + UtilString.escapeXML(checkedText.toLowerCase(Locale.ROOT)) + "</i>";
            };
        }

        @Override
        public String toString() {
            return jsonValue;
        }
    }

    public record ConversationLine(String speaker, String listener, LineType type, List<ConversationVariant> variants) {
        public ConversationLine(String speaker, String listener, List<ConversationVariant> variants) {
            this(speaker, listener, LineType.SAY, variants);
        }

        public ConversationLine {
            speaker = Validation.requireNonBlank(speaker, "Conversation line speaker is required.");
            listener = Validation.requireNonNull(listener, "Conversation line listener is required.");
            type = Validation.requireNonNull(type, "Conversation line type is required.");
            variants = List.copyOf(Validation.requireNonEmpty(variants, "Conversation line variants are required."));
        }
    }

    public record ConversationVariant(String text, String value, double weight, List<String> conditions) {
        public ConversationVariant(String text, double weight, List<String> conditions) {
            this(text, "", weight, conditions);
        }

        public ConversationVariant {
            text = Validation.requireNonNull(text, "Conversation variant text is required.");
            value = Validation.requireNonNull(value, "Conversation variant value is required.");
            weight = Validation.requirePositive(weight, "Conversation variant weight must be positive.");
            conditions = List.copyOf(Validation.requireNonNull(conditions, "Conversation variant conditions are required."));
        }
    }
}
