package com.eb.javafx.scene;

import com.eb.javafx.text.TextVariableCatalog;
import com.eb.javafx.text.TextVariableResolver;
import com.eb.javafx.text.TextVariableType;
import com.eb.javafx.util.Validation;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Declares application-owned conversation condition variables and their optional lookup handler. */
public final class ConversationConditionVariables {
    private static final ConversationConditionVariables FIXED = new ConversationConditionVariables(TextVariableCatalog.empty());

    private final TextVariableCatalog applicationVariableCatalog;

    private ConversationConditionVariables(TextVariableCatalog applicationVariableCatalog) {
        TextVariableCatalog checkedCatalog = Validation.requireNonNull(
                applicationVariableCatalog,
                "Conversation application variable catalog is required.");
        checkedCatalog.variableNames().forEach(variableName -> {
            if (ConversationConditionSyntax.variableNames().contains(variableName)) {
                throw new IllegalArgumentException("Conversation application variable conflicts with a fixed variable name: "
                        + variableName);
            }
        });
        this.applicationVariableCatalog = checkedCatalog;
    }

    public static ConversationConditionVariables fixed() {
        return FIXED;
    }

    public static ConversationConditionVariables declaring(Collection<String> applicationVariableNames) {
        return catalog(TextVariableCatalog.of(Validation.requireNonNull(
                        applicationVariableNames,
                        "Conversation application variable names are required.")
                .stream()
                .map(name -> new TextVariableCatalog.VariableDefinition(name, TextVariableType.STRING))
                .toList()));
    }

    public static ConversationConditionVariables withResolver(
            Collection<String> applicationVariableNames,
            TextVariableResolver resolver) {
        return catalog(TextVariableCatalog.of(Validation.requireNonNull(
                        applicationVariableNames,
                        "Conversation application variable names are required.")
                .stream()
                .map(name -> new TextVariableCatalog.VariableDefinition(name, TextVariableType.STRING))
                .toList()).withResolver(resolver));
    }

    public static ConversationConditionVariables catalog(TextVariableCatalog applicationVariableCatalog) {
        return new ConversationConditionVariables(applicationVariableCatalog);
    }

    public Set<String> applicationVariableNames() {
        return applicationVariableCatalog.variableNames();
    }

    public TextVariableCatalog applicationVariableCatalog() {
        return applicationVariableCatalog;
    }

    public Set<String> declaredVariableNames() {
        return java.util.stream.Stream.concat(
                        ConversationConditionSyntax.variableNames().stream(),
                        applicationVariableCatalog.variableNames().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isDeclared(String variableName) {
        String checkedVariableName = Validation.requireNonBlank(variableName, "Conversation condition variable name is required.");
        return ConversationConditionSyntax.variableNames().contains(checkedVariableName)
                || applicationVariableCatalog.isDeclared(checkedVariableName);
    }

    public Optional<String> resolve(String variableName) {
        String checkedVariableName = Validation.requireNonBlank(variableName, "Conversation condition variable name is required.");
        return applicationVariableCatalog.isDeclared(checkedVariableName)
                ? applicationVariableCatalog.resolve(checkedVariableName)
                : Optional.empty();
    }
}
