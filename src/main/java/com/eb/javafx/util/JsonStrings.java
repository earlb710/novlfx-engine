package com.eb.javafx.util;

/** Minimal JSON string escaping and parsing helpers for diagnostic JSON files. */
public final class JsonStrings {
    private JsonStrings() {
    }

    public static String quote(String value) {
        return '"' + escape(value) + '"';
    }

    public static String nullableQuote(String value) {
        return value == null ? "null" : quote(value);
    }

    public static String escape(String value) {
        Validation.requireNonNull(value, "JSON string value is required.");
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    public static ParsedString parse(String value, int startIndex) {
        Validation.requireNonNull(value, "JSON source is required.");
        if (startIndex < 0 || startIndex >= value.length() || value.charAt(startIndex) != '"') {
            throw new IllegalArgumentException("JSON string must start with a quote.");
        }
        StringBuilder parsed = new StringBuilder();
        int index = startIndex + 1;
        while (index < value.length()) {
            char character = value.charAt(index);
            if (character == '"') {
                return new ParsedString(parsed.toString(), index + 1);
            }
            if (character == '\\' && index + 1 < value.length()) {
                index++;
                appendEscapedCharacter(parsed, value, index);
                if (value.charAt(index) == 'u' && index + 4 < value.length()) {
                    index += 4;
                }
            } else {
                parsed.append(character);
            }
            index++;
        }
        return new ParsedString(parsed.toString(), index);
    }

    private static void appendEscapedCharacter(StringBuilder parsed, String value, int escapeIndex) {
        char escaped = value.charAt(escapeIndex);
        switch (escaped) {
            case '"' -> parsed.append('"');
            case '\\' -> parsed.append('\\');
            case 'b' -> parsed.append('\b');
            case 'f' -> parsed.append('\f');
            case 'n' -> parsed.append('\n');
            case 'r' -> parsed.append('\r');
            case 't' -> parsed.append('\t');
            case 'u' -> {
                if (escapeIndex + 5 <= value.length()) {
                    parsed.append((char) Integer.parseInt(value.substring(escapeIndex + 1, escapeIndex + 5), 16));
                }
            }
            default -> parsed.append(escaped);
        }
    }

    public static final class ParsedString {
        private final String value;
        private final int endIndex;

        private ParsedString(String value, int endIndex) {
            this.value = value;
            this.endIndex = endIndex;
        }

        public String value() {
            return value;
        }

        public int endIndex() {
            return endIndex;
        }
    }
}
