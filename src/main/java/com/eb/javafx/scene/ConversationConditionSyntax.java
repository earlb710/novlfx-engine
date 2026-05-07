package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.Comparator;
import java.util.Set;

/** Validates authored conversation condition strings and their declared variable references. */
public final class ConversationConditionSyntax {
    private static final Set<String> VARIABLE_NAMES = Set.of(
            "conversation.id",
            "conversation.name",
            "conversation.language",
            "line.speaker",
            "line.listener",
            "line.type",
            "variant.text",
            "variant.value",
            "variant.weight",
            "variant.tooltipText",
            "choice.text",
            "choice.value",
            "choice.tooltipText",
            "speaker",
            "listener",
            "lineType",
            "text",
            "value",
            "weight",
            "tooltipText");

    /** Static-only helper; callers should use the validation and replacement methods directly. */
    private ConversationConditionSyntax() {
    }

    public static Set<String> variableNames() {
        return VARIABLE_NAMES;
    }

    public static void validateCondition(String condition, String description) {
        validateCondition(condition, description, ConversationConditionVariables.fixed());
    }

    public static void validateCondition(
            String condition,
            String description,
            ConversationConditionVariables variables) {
        String checkedCondition = Validation.requireNonBlank(condition, description + " is required.");
        ConversationConditionVariables checkedVariables = Validation.requireNonNull(variables,
                "Conversation condition variables are required.");
        int index = 0;
        while (index < checkedCondition.length()) {
            int dollarSignIndex = checkedCondition.indexOf('$', index);
            if (dollarSignIndex < 0) {
                return;
            }
            VariableReference reference = variableReferenceAt(checkedCondition, dollarSignIndex, description, checkedVariables);
            index = reference.endIndex();
        }
    }

    public static String replaceVariables(String condition, ConversationConditionVariables variables) {
        String checkedCondition = Validation.requireNonBlank(condition, "Conversation condition is required.");
        ConversationConditionVariables checkedVariables = Validation.requireNonNull(variables,
                "Conversation condition variables are required.");
        StringBuilder result = new StringBuilder();
        int index = 0;
        while (index < checkedCondition.length()) {
            int dollarSignIndex = checkedCondition.indexOf('$', index);
            if (dollarSignIndex < 0) {
                result.append(checkedCondition.substring(index));
                break;
            }
            result.append(checkedCondition, index, dollarSignIndex);
            VariableReference reference = variableReferenceAt(checkedCondition, dollarSignIndex,
                    "Conversation condition", checkedVariables);
            result.append(checkedVariables.resolve(reference.name()).orElse(reference.sourceText()));
            index = reference.endIndex();
        }
        return result.toString();
    }

    private static VariableReference variableReferenceAt(
            String condition,
            int dollarSignIndex,
            String description,
            ConversationConditionVariables variables) {
        int nameStart = dollarSignIndex + 1;
        if (nameStart >= condition.length()) {
            throw new IllegalArgumentException(description + " has dangling variable marker '$'.");
        }
        if (condition.charAt(nameStart) == '{') {
            int nameEnd = condition.indexOf('}', nameStart + 1);
            if (nameEnd < 0) {
                throw new IllegalArgumentException(description + " has unterminated variable reference: "
                        + condition.substring(dollarSignIndex));
            }
            String variableName = condition.substring(nameStart + 1, nameEnd);
            validateVariableName(variableName, description, variables);
            return new VariableReference(variableName, nameEnd + 1, condition.substring(dollarSignIndex, nameEnd + 1));
        }
        // Match longer unbraced variable names before shorter aliases, e.g. line.speaker before speaker.
        String variableName = variables.declaredVariableNames().stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .filter(name -> condition.startsWith(name, nameStart)
                        && isVariableBoundary(condition, nameStart + name.length()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(description
                        + " references an unknown conversation variable near: "
                        + condition.substring(dollarSignIndex)));
        return new VariableReference(variableName, nameStart + variableName.length(),
                condition.substring(dollarSignIndex, nameStart + variableName.length()));
    }

    private static boolean isVariableBoundary(String condition, int index) {
        if (index >= condition.length()) {
            return true;
        }
        char next = condition.charAt(index);
        return !Character.isLetterOrDigit(next) && next != '_' && next != '.';
    }

    private static void validateVariableName(
            String variableName,
            String description,
            ConversationConditionVariables variables) {
        if (!variables.isDeclared(variableName)) {
            throw new IllegalArgumentException(description + " references unknown conversation variable: " + variableName);
        }
    }

    private record VariableReference(String name, int endIndex, String sourceText) {
    }
}
