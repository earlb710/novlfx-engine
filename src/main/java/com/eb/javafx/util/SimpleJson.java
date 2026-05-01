package com.eb.javafx.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser for small engine-owned configuration documents.
 *
 * <p>This parser supports objects, arrays, strings, booleans, null, and
 * integer numbers. It is intentionally small so reusable engine data classes
 * can parse authored JSON without adding a dependency.</p>
 */
public final class SimpleJson {
    private SimpleJson() {
    }

    public static Object parse(String json, String sourceName) {
        return new Parser(json, sourceName).parse();
    }

    private static final class Parser {
        private final String json;
        private final String sourceName;
        private int index;

        private Parser(String json, String sourceName) {
            this.json = Validation.requireNonNull(json, "JSON input is required.");
            this.sourceName = sourceName == null || sourceName.isBlank() ? "JSON" : sourceName;
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
                    if (json.startsWith("true", index)) {
                        index += 4;
                        yield Boolean.TRUE;
                    }
                    if (json.startsWith("false", index)) {
                        index += 5;
                        yield Boolean.FALSE;
                    }
                    if (json.startsWith("null", index)) {
                        index += 4;
                        yield null;
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
