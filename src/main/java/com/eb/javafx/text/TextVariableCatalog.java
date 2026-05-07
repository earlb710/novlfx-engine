package com.eb.javafx.text;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Reusable catalog of declared application variable names, their value kinds, and an optional lookup handler. */
public final class TextVariableCatalog {
    /** Application variable names start with a letter and then allow letters, digits, underscores (_), or dots (.). */
    private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_.]*");
    private static final TextVariableCatalog EMPTY = new TextVariableCatalog(Map.of(), name -> Optional.empty());

    private final Map<String, TextVariableType> variableTypes;
    private final TextVariableResolver resolver;

    private TextVariableCatalog(Map<String, TextVariableType> variableTypes, TextVariableResolver resolver) {
        LinkedHashMap<String, TextVariableType> validatedTypes = new LinkedHashMap<>();
        Validation.requireNonNull(variableTypes, "Text variable catalog definitions are required.")
                .forEach((name, valueType) -> {
                    String checkedName = validatedVariableName(name);
                    TextVariableType previous = validatedTypes.put(
                            checkedName,
                            Validation.requireNonNull(valueType, "Text variable catalog value type is required."));
                    if (previous != null) {
                        throw new IllegalArgumentException("Duplicate text variable catalog entry: " + checkedName);
                    }
                });
        this.variableTypes = Map.copyOf(validatedTypes);
        this.resolver = Validation.requireNonNull(resolver, "Text variable resolver is required.");
    }

    public static TextVariableCatalog empty() {
        return EMPTY;
    }

    public static TextVariableCatalog of(Collection<VariableDefinition> definitions) {
        LinkedHashMap<String, TextVariableType> variableTypes = new LinkedHashMap<>();
        Validation.requireNonNull(definitions, "Text variable catalog definitions are required.")
                .forEach(definition -> {
                    VariableDefinition checkedDefinition = Validation.requireNonNull(
                            definition,
                            "Text variable catalog definition is required.");
                    TextVariableType previous = variableTypes.put(checkedDefinition.name(), checkedDefinition.valueType());
                    if (previous != null) {
                        throw new IllegalArgumentException("Duplicate text variable catalog entry: " + checkedDefinition.name());
                    }
                });
        return new TextVariableCatalog(variableTypes, name -> Optional.empty());
    }

    public static TextVariableCatalog load(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Text variable catalog JSON path is required.");
        try {
            return fromJson(Files.readString(jsonPath, StandardCharsets.UTF_8), jsonPath.toString());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read text variable catalog JSON: " + jsonPath, exception);
        }
    }

    public static TextVariableCatalog fromJson(String json, String sourceName) {
        Map<String, Object> root = JsonData.rootObject(json, sourceName);
        return of(JsonData.requiredList(root, "variables", "root.variables").stream()
                .map(entry -> parseDefinition(JsonData.requireObject(entry, "root.variables[]")))
                .toList());
    }

    public TextVariableCatalog withResolver(TextVariableResolver resolver) {
        return new TextVariableCatalog(variableTypes, resolver);
    }

    public Set<String> variableNames() {
        return variableTypes.keySet();
    }

    public Map<String, TextVariableType> variableTypes() {
        return variableTypes;
    }

    public boolean isDeclared(String variableName) {
        String checkedVariableName = Validation.requireNonBlank(variableName, "Text variable name is required.");
        return variableTypes.containsKey(checkedVariableName);
    }

    public Optional<TextVariableType> valueType(String variableName) {
        String checkedVariableName = Validation.requireNonBlank(variableName, "Text variable name is required.");
        return Optional.ofNullable(variableTypes.get(checkedVariableName));
    }

    public TextVariableType requireValueType(String variableName) {
        String checkedVariableName = Validation.requireNonBlank(variableName, "Text variable name is required.");
        return valueType(checkedVariableName)
                .orElseThrow(() -> new IllegalArgumentException("Text variable is not declared in catalog: " + checkedVariableName));
    }

    public Optional<String> resolve(String variableName) {
        String checkedVariableName = Validation.requireNonBlank(variableName, "Text variable name is required.");
        requireValueType(checkedVariableName);
        return resolver.resolve(checkedVariableName);
    }

    private static VariableDefinition parseDefinition(Map<String, Object> object) {
        return new VariableDefinition(
                JsonData.requiredString(object, "name", "text variable name"),
                JsonData.enumValue(
                        TextVariableType.class,
                        JsonData.requiredString(object, "valueType", "text variable value type"),
                        "text variable value type"));
    }

    private static String validatedVariableName(String variableName) {
        String checkedVariableName = Validation.requireNonBlank(variableName, "Text variable name is required.").trim();
        if (checkedVariableName.startsWith("$")) {
            throw new IllegalArgumentException("Text variable names must omit the '$' prefix: " + checkedVariableName);
        }
        if (!VARIABLE_NAME_PATTERN.matcher(checkedVariableName).matches()) {
            throw new IllegalArgumentException("Invalid text variable name: " + checkedVariableName);
        }
        return checkedVariableName;
    }

    public record VariableDefinition(String name, TextVariableType valueType) {
        public VariableDefinition {
            name = validatedVariableName(name);
            valueType = Validation.requireNonNull(valueType, "Text variable value type is required.");
        }
    }
}
