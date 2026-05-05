package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.List;

/**
 * Data-only authored conversation document using the LR2Alt exported JSON shape.
 *
 * <p>The schema mirrors LR2Alt files under {@code docs/conversations}: top-level {@code schemaVersion},
 * {@code language}, and ordered {@code conversations}; each conversation has dialogue {@code lines}, and each
 * line has a speaker plus one or more text variants.</p>
 */
public final class ConversationDefinition {
    private final int schemaVersion;
    private final String language;
    private final List<ConversationBlock> conversations;

    public ConversationDefinition(int schemaVersion, String language, List<ConversationBlock> conversations) {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("Conversation schemaVersion must be positive.");
        }
        this.schemaVersion = schemaVersion;
        this.language = Validation.requireNonBlank(language, "Conversation language is required.");
        this.conversations = List.copyOf(Validation.requireNonEmpty(conversations, "Conversation document requires conversations."));
    }

    public int schemaVersion() {
        return schemaVersion;
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

    public record ConversationLine(String speaker, List<ConversationVariant> variants) {
        public ConversationLine {
            speaker = Validation.requireNonBlank(speaker, "Conversation line speaker is required.");
            variants = List.copyOf(Validation.requireNonEmpty(variants, "Conversation line variants are required."));
        }
    }

    public record ConversationVariant(String text) {
        public ConversationVariant {
            text = Validation.requireNonNull(text, "Conversation variant text is required.");
        }
    }
}
