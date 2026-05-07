package com.eb.javafx.scene;

import com.eb.javafx.text.TextVariableResolver;
import com.eb.javafx.util.Validation;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Declares application-owned conversation condition variables and their optional lookup handler. */
public final class ConversationConditionVariables {
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_.]*");
    private static final ConversationConditionVariables FIXED = new ConversationConditionVariables(Set.of(), name -> Optional.empty());

    private final Set<String> applicationVariableNames;
    private final TextVariableResolver resolver;

    private ConversationConditionVariables(Collection<String> applicationVariableNames, TextVariableResolver resolver) {
        this.applicationVariableNames = Set.copyOf(Validation.requireNonNull(applicationVariableNames,
                        "Conversation application variable names are required.")
                .stream()
                .map(ConversationConditionVariables::validatedVariableName)
                .collect(Collectors.toSet()));
        this.resolver = Validation.requireNonNull(resolver, "Conversation condition variable resolver is required.");
    }

    public static ConversationConditionVariables fixed() {
        return FIXED;
    }

    public static ConversationConditionVariables declaring(Collection<String> applicationVariableNames) {
        return new ConversationConditionVariables(applicationVariableNames, name -> Optional.empty());
    }

    public static ConversationConditionVariables withResolver(
            Collection<String> applicationVariableNames,
            TextVariableResolver resolver) {
        return new ConversationConditionVariables(applicationVariableNames, resolver);
    }

    public Set<String> applicationVariableNames() {
        return applicationVariableNames;
    }

    public Set<String> declaredVariableNames() {
        return java.util.stream.Stream.concat(
                        ConversationConditionSyntax.variableNames().stream(),
                        applicationVariableNames.stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isDeclared(String variableName) {
        String checkedVariableName = Validation.requireNonBlank(variableName, "Conversation condition variable name is required.");
        return ConversationConditionSyntax.variableNames().contains(checkedVariableName)
                || applicationVariableNames.contains(checkedVariableName);
    }

    public Optional<String> resolve(String variableName) {
        String checkedVariableName = Validation.requireNonBlank(variableName, "Conversation condition variable name is required.");
        return applicationVariableNames.contains(checkedVariableName)
                ? resolver.resolve(checkedVariableName)
                : Optional.empty();
    }

    private static String validatedVariableName(String variableName) {
        String checkedVariableName = Validation.requireNonBlank(variableName,
                "Conversation application variable name is required.").trim();
        if (checkedVariableName.startsWith("$")) {
            throw new IllegalArgumentException("Conversation application variable names must omit the '$' prefix: "
                    + checkedVariableName);
        }
        if (!VARIABLE_NAME_PATTERN.matcher(checkedVariableName).matches()) {
            throw new IllegalArgumentException("Invalid conversation application variable name: " + checkedVariableName);
        }
        return checkedVariableName;
    }
}
