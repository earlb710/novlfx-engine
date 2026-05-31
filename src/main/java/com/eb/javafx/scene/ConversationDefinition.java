package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;
import com.eb.javafx.util.UtilString;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Data-only authored conversation document.
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

    /** One ordered conversation block with author-facing description text and dialogue lines. */
    public record ConversationBlock(String id, String description, List<ConversationLine> lines) {
        public ConversationBlock {
            id = Validation.requireNonBlank(id, "Conversation id is required.");
            description = Validation.requireNonBlank(description, "Conversation description is required.");
            lines = List.copyOf(Validation.requireNonEmpty(lines, "Conversation lines are required."));
        }
    }

    /** Authored dialogue presentation modes and their JSON token values. */
    public enum LineType {
        SHOUT("shout"),
        SAY("say"),
        WHISPER("whisper"),
        THINK("think"),
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
                // Internal thought — italic, preserving the author's casing.
                case THINK -> "<i>" + UtilString.escapeXML(checkedText) + "</i>";
            };
        }

        @Override
        public String toString() {
            return jsonValue;
        }
    }

    /** One speaker/listener line with a presentation type and one or more candidate variants. */
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

    /** Immutable variant class with constructor-specific condition variable validation. */
    public static final class ConversationVariant {
        private final String text;
        private final String value;
        private final double weight;
        private final List<String> conditions;
        private final String tooltipText;

        public ConversationVariant(String text, double weight, List<String> conditions) {
            this(text, "", weight, conditions, "");
        }

        public ConversationVariant(String text, String value, double weight, List<String> conditions, String tooltipText) {
            this(text, value, weight, conditions, tooltipText, ConversationConditionVariables.fixed());
        }

        public ConversationVariant(
                String text,
                String value,
                double weight,
                List<String> conditions,
                String tooltipText,
                ConversationConditionVariables conditionVariables) {
            this.text = Validation.requireNonNull(text, "Conversation variant text is required.");
            this.value = Validation.requireNonNull(value, "Conversation variant value cannot be null.");
            this.weight = Validation.requirePositive(weight, "Conversation variant weight must be positive.");
            this.conditions = validatedConditions(conditions, conditionVariables);
            this.tooltipText = Validation.requireNonNull(tooltipText, "Conversation variant tooltip text cannot be null.");
        }

        public String text() {
            return text;
        }

        public String value() {
            return value;
        }

        public double weight() {
            return weight;
        }

        public List<String> conditions() {
            return conditions;
        }

        public String tooltipText() {
            return tooltipText;
        }

        private static List<String> validatedConditions(
                List<String> conditions,
                ConversationConditionVariables conditionVariables) {
            List<String> checkedConditions = List.copyOf(Validation.requireNonNull(conditions, "Conversation variant conditions are required."));
            ConversationConditionVariables checkedConditionVariables = Validation.requireNonNull(conditionVariables,
                    "Conversation condition variables are required.");
            for (int index = 0; index < checkedConditions.size(); index++) {
                ConversationConditionSyntax.validateCondition(
                        checkedConditions.get(index),
                        "Conversation variant condition " + (index + 1),
                        checkedConditionVariables);
            }
            return checkedConditions;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ConversationVariant other)) {
                return false;
            }
            return Double.compare(weight, other.weight) == 0
                    && text.equals(other.text)
                    && value.equals(other.value)
                    && conditions.equals(other.conditions)
                    && tooltipText.equals(other.tooltipText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, value, weight, conditions, tooltipText);
        }

        @Override
        public String toString() {
            return "ConversationVariant[text=" + text
                    + ", value=" + value
                    + ", weight=" + weight
                    + ", conditions=" + conditions
                    + ", tooltipText=" + tooltipText
                    + "]";
        }
    }
}
