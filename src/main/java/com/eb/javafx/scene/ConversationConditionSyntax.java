package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.Comparator;
import java.util.Set;

/** Validates authored conversation condition strings and their fixed variable references. */
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
            int markerIndex = checkedCondition.indexOf('$', index);
            if (markerIndex < 0) {
                return;
            }
            VariableReference reference = variableReferenceAt(checkedCondition, markerIndex, description, checkedVariables);
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
            int markerIndex = checkedCondition.indexOf('$', index);
            if (markerIndex < 0) {
                result.append(checkedCondition.substring(index));
                break;
            }
            result.append(checkedCondition, index, markerIndex);
            VariableReference reference = variableReferenceAt(checkedCondition, markerIndex,
                    "Conversation condition", checkedVariables);
            result.append(checkedVariables.resolve(reference.name()).orElse(reference.sourceText()));
            index = reference.endIndex();
        }
        return result.toString();
    }

    private static VariableReference variableReferenceAt(
            String condition,
            int markerIndex,
            String description,
            ConversationConditionVariables variables) {
        int nameStart = markerIndex + 1;
        if (nameStart >= condition.length()) {
            throw new IllegalArgumentException(description + " has dangling variable marker '$'.");
        }
        if (condition.charAt(nameStart) == '{') {
            int nameEnd = condition.indexOf('}', nameStart + 1);
            if (nameEnd < 0) {
                throw new IllegalArgumentException(description + " has unterminated variable reference: "
                        + condition.substring(markerIndex));
            }
            String variableName = condition.substring(nameStart + 1, nameEnd);
            validateVariableName(variableName, description, variables);
            return new VariableReference(variableName, markerIndex, nameEnd + 1, condition.substring(markerIndex, nameEnd + 1));
        }
        // Match longer unbraced variable names before shorter aliases, e.g. line.speaker before speaker.
        String variableName = variables.declaredVariableNames().stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .filter(name -> condition.startsWith(name, nameStart)
                        && isVariableBoundary(condition, nameStart + name.length()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(description
                        + " references an unknown conversation variable near: "
                        + condition.substring(markerIndex)));
        return new VariableReference(variableName, markerIndex, nameStart + variableName.length(),
                condition.substring(markerIndex, nameStart + variableName.length()));
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

    private record VariableReference(String name, int startIndex, int endIndex, String sourceText) {
    }
}
