package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.util.Comparator;
import java.util.List;
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
    // Match longer unbraced variable names before shorter aliases, e.g. line.speaker before speaker.
    private static final List<String> VARIABLE_NAMES_BY_LENGTH = VARIABLE_NAMES.stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();

    private ConversationConditionSyntax() {
    }

    public static Set<String> variableNames() {
        return VARIABLE_NAMES;
    }

    public static void validateCondition(String condition, String description) {
        String checkedCondition = Validation.requireNonBlank(condition, description + " is required.");
        int index = 0;
        while (index < checkedCondition.length()) {
            int markerIndex = checkedCondition.indexOf('$', index);
            if (markerIndex < 0) {
                return;
            }
            index = validateVariableAt(checkedCondition, markerIndex, description);
        }
    }

    private static int validateVariableAt(String condition, int markerIndex, String description) {
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
            validateVariableName(variableName, description);
            return nameEnd + 1;
        }
        String variableName = VARIABLE_NAMES_BY_LENGTH.stream()
                .filter(name -> condition.startsWith(name, nameStart)
                        && isVariableBoundary(condition, nameStart + name.length()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(description
                        + " references an unknown conversation variable near: "
                        + condition.substring(markerIndex)));
        return nameStart + variableName.length();
    }

    private static boolean isVariableBoundary(String condition, int index) {
        if (index >= condition.length()) {
            return true;
        }
        char next = condition.charAt(index);
        return !Character.isLetterOrDigit(next) && next != '_' && next != '.' && next != '-';
    }

    private static void validateVariableName(String variableName, String description) {
        if (!VARIABLE_NAMES.contains(variableName)) {
            throw new IllegalArgumentException(description + " references unknown conversation variable: " + variableName);
        }
    }
}
