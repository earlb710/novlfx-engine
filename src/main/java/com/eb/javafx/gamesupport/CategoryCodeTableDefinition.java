package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

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
        Object root = new JsonParser(json, sourceName).parse();
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

    private static final class JsonParser {
        private final String json;
        private final String sourceName;
        private int index;

        private JsonParser(String json, String sourceName) {
            this.json = Validation.requireNonNull(json, "Category code table JSON is required.");
            this.sourceName = sourceName == null || sourceName.isBlank() ? "category code table JSON" : sourceName;
        }

        private Object parse() {
            Object value = parseValue();
            skipWhitespace();
            if (index != json.length()) {
                throw error("Unexpected content after JSON document.");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= json.length()) {
                throw error("Unexpected end of JSON document.");
            }
            char character = json.charAt(index);
            return switch (character) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case '-' -> parseNumber();
                default -> {
                    if (Character.isDigit(character)) {
                        yield parseNumber();
                    }
                    throw error("Unsupported JSON value.");
                }
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return object;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> values = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return values;
            }
            while (true) {
                values.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return values;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder parsed = new StringBuilder();
            while (index < json.length()) {
                char character = json.charAt(index++);
                if (character == '"') {
                    return parsed.toString();
                }
                if (character == '\\') {
                    appendEscapedCharacter(parsed);
                } else {
                    if (character < 0x20) {
                        throw error("Control character is not allowed in a JSON string.");
                    }
                    parsed.append(character);
                }
            }
            throw error("Unterminated JSON string.");
        }

        private void appendEscapedCharacter(StringBuilder parsed) {
            if (index >= json.length()) {
                throw error("Unterminated JSON escape.");
            }
            char escaped = json.charAt(index++);
            switch (escaped) {
                case '"' -> parsed.append('"');
                case '\\' -> parsed.append('\\');
                case '/' -> parsed.append('/');
                case 'b' -> parsed.append('\b');
                case 'f' -> parsed.append('\f');
                case 'n' -> parsed.append('\n');
                case 'r' -> parsed.append('\r');
                case 't' -> parsed.append('\t');
                case 'u' -> parsed.append(parseUnicodeEscape());
                default -> throw error("Unsupported JSON escape.");
            }
        }

        private char parseUnicodeEscape() {
            if (index + 4 > json.length()) {
                throw error("Incomplete JSON unicode escape.");
            }
            try {
                char value = (char) Integer.parseInt(json.substring(index, index + 4), 16);
                index += 4;
                return value;
            } catch (NumberFormatException exception) {
                throw error("Invalid JSON unicode escape.");
            }
        }

        private int parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            if (index >= json.length() || !Character.isDigit(json.charAt(index))) {
                throw error("Invalid JSON integer.");
            }
            while (index < json.length() && Character.isDigit(json.charAt(index))) {
                index++;
            }
            if (index < json.length() && (json.charAt(index) == '.' || json.charAt(index) == 'e' || json.charAt(index) == 'E')) {
                throw error("Only JSON integers are supported here.");
            }
            try {
                return Integer.parseInt(json.substring(start, index));
            } catch (NumberFormatException exception) {
                throw error("JSON integer is out of range.");
            }
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= json.length() || json.charAt(index) != expected) {
                throw error("Expected '" + expected + "'.");
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < json.length() && json.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " Source: " + sourceName + " at index " + index + ".");
        }
    }
}
