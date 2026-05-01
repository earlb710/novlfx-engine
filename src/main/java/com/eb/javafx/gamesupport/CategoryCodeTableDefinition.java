package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.SimpleJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JSON-backed set of project-defined category code tables for a single authored language.
 */
public final class CategoryCodeTableDefinition {
    private final String language;
    private final DefinitionRegistry<CodeTableDefinition> tables = new DefinitionRegistry<>("Code table");

    private CategoryCodeTableDefinition(String language, List<CodeTableDefinition> tables) {
        this.language = Validation.requireNonBlank(language, "Category code table language must not be blank.");
        Validation.requireNonEmpty(tables, "Category code table file must contain at least one table.")
                .forEach(this.tables::register);
    }

    public static CategoryCodeTableDefinition of(String language, List<CodeTableDefinition> tables) {
        return new CategoryCodeTableDefinition(language, tables);
    }

    /**
     * Loads category code tables from a UTF-8 JSON file.
     *
     * <p>The root object must contain a non-blank {@code language} field and a {@code tables} array. Titles are
     * interpreted as authored text for that language so applications can provide separate files per translation.</p>
     *
     * @param jsonPath path to the authored JSON file
     * @return parsed category code tables
     */
    public static CategoryCodeTableDefinition load(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Category code table JSON path is required.");
        try {
            return fromJson(Files.readString(jsonPath, StandardCharsets.UTF_8), jsonPath.toString());
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read category code table JSON: " + jsonPath, exception);
        }
    }

    static CategoryCodeTableDefinition fromJson(String json, String sourceName) {
        Object root = SimpleJson.parse(json, sourceName);
        Map<String, Object> rootObject = requireObject(root, "root");
        String language = requireString(rootObject, "language", "root.language");
        List<CodeTableDefinition> tables = requireArray(rootObject, "tables", "root.tables").stream()
                .map(CategoryCodeTableDefinition::toCodeTable)
                .toList();
        return new CategoryCodeTableDefinition(language, tables);
    }

    public String language() {
        return language;
    }

    public List<CodeTableDefinition> tables() {
        return tables.definitions();
    }

    public Optional<CodeTableDefinition> table(String tableId) {
        return tables.definition(tableId);
    }

    public boolean containsTable(String tableId) {
        return tables.contains(tableId);
    }

    public CategoryCodeTableDefinition addTable(CodeTableDefinition table) {
        List<CodeTableDefinition> updatedTables = new ArrayList<>(tables());
        updatedTables.add(Validation.requireNonNull(table, "Code table definition is required."));
        return new CategoryCodeTableDefinition(language, updatedTables);
    }

    public CategoryCodeTableDefinition removeTable(String tableId) {
        requireExistingTable(tableId);
        return new CategoryCodeTableDefinition(
                language,
                tables().stream()
                        .filter(table -> !table.id().equals(tableId))
                        .toList());
    }

    public CategoryCodeTableDefinition editTable(CodeTableDefinition table) {
        CodeTableDefinition checkedTable = Validation.requireNonNull(table, "Code table definition is required.");
        requireExistingTable(checkedTable.id());
        return new CategoryCodeTableDefinition(
                language,
                tables().stream()
                        .map(existingTable -> existingTable.id().equals(checkedTable.id()) ? checkedTable : existingTable)
                        .toList());
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
                .append("  \"language\": ").append(JsonStrings.quote(language)).append(",\n")
                .append("  \"tables\": [\n");
        List<CodeTableDefinition> orderedTables = tables();
        for (int tableIndex = 0; tableIndex < orderedTables.size(); tableIndex++) {
            CodeTableDefinition table = orderedTables.get(tableIndex);
            json.append("    {\n")
                    .append("      \"id\": ").append(JsonStrings.quote(table.id())).append(",\n")
                    .append("      \"title\": ").append(JsonStrings.quote(table.title())).append(",\n")
                    .append("      \"codes\": [\n");
            List<CodeDefinition> codes = table.codes();
            for (int codeIndex = 0; codeIndex < codes.size(); codeIndex++) {
                CodeDefinition code = codes.get(codeIndex);
                json.append("        {\n")
                        .append("          \"id\": ").append(JsonStrings.quote(code.id())).append(",\n")
                        .append("          \"title\": ").append(JsonStrings.quote(code.title())).append(",\n")
                        .append("          \"sortOrder\": ").append(code.sortOrder()).append(",\n")
                        .append("          \"tags\": ").append(toJsonArray(code.tags())).append('\n')
                        .append("        }");
                if (codeIndex + 1 < codes.size()) {
                    json.append(',');
                }
                json.append('\n');
            }
            json.append("      ]\n")
                    .append("    }");
            if (tableIndex + 1 < orderedTables.size()) {
                json.append(',');
            }
            json.append('\n');
        }
        json.append("  ]\n")
                .append("}\n");
        return json.toString();
    }

    public void save(Path jsonPath) {
        Validation.requireNonNull(jsonPath, "Category code table JSON path is required.");
        try {
            Files.writeString(jsonPath, toJson(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to write category code table JSON: " + jsonPath, exception);
        }
    }

    private static CodeTableDefinition toCodeTable(Object value) {
        Map<String, Object> tableObject = requireObject(value, "table");
        String tableId = requireString(tableObject, "id", "table.id");
        String title = requireString(tableObject, "title", "table.title");
        List<CodeDefinition> codes = requireArray(tableObject, "codes", "table.codes").stream()
                .map(CategoryCodeTableDefinition::toCode)
                .toList();
        return new CodeTableDefinition(tableId, title, codes);
    }

    private static CodeDefinition toCode(Object value) {
        Map<String, Object> codeObject = requireObject(value, "code");
        String codeId = requireString(codeObject, "id", "code.id");
        String title = requireString(codeObject, "title", "code.title");
        int sortOrder = requireInt(codeObject, "sortOrder", "code.sortOrder");
        List<String> tags = requireOptionalArray(codeObject, "tags", "code.tags").stream()
                .map(tag -> requireStringValue(tag, "code.tags[]"))
                .toList();
        return new CodeDefinition(codeId, title, sortOrder, tags);
    }

    private static Map<String, Object> requireObject(Object value, String description) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> {
                if (!(key instanceof String stringKey)) {
                    throw new IllegalArgumentException("JSON object key must be a string in " + description + ".");
                }
                result.put(stringKey, mapValue);
            });
            return result;
        }
        throw new IllegalArgumentException("Expected JSON object for " + description + ".");
    }

    private static String requireString(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key)) {
            throw new IllegalArgumentException("Missing JSON string field: " + description);
        }
        return Validation.requireNonBlank(requireStringValue(object.get(key), description), description + " must not be blank.");
    }

    private static String requireStringValue(Object value, String description) {
        if (value instanceof String stringValue) {
            return stringValue;
        }
        throw new IllegalArgumentException("Expected JSON string for " + description + ".");
    }

    private static int requireInt(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key)) {
            throw new IllegalArgumentException("Missing JSON integer field: " + description);
        }
        Object value = object.get(key);
        if (value instanceof Integer integerValue) {
            return integerValue;
        }
        throw new IllegalArgumentException("Expected JSON integer for " + description + ".");
    }

    private static List<Object> requireArray(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key)) {
            throw new IllegalArgumentException("Missing JSON array field: " + description);
        }
        Object value = object.get(key);
        if (value instanceof List<?> list) {
            return List.copyOf(list);
        }
        throw new IllegalArgumentException("Expected JSON array for " + description + ".");
    }

    private static List<Object> requireOptionalArray(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key)) {
            return List.of();
        }
        Object value = object.get(key);
        if (value instanceof List<?> list) {
            return List.copyOf(list);
        }
        throw new IllegalArgumentException("Expected JSON array for " + description + ".");
    }

    private static String toJsonArray(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                json.append(", ");
            }
            json.append(JsonStrings.quote(values.get(index)));
        }
        json.append(']');
        return json.toString();
    }

    private void requireExistingTable(String tableId) {
        if (!containsTable(tableId)) {
            throw new IllegalArgumentException("Unknown code table: " + tableId);
        }
    }

}
