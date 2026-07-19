package com.eb.javafx.util;

import java.util.List;
import java.util.Map;

/**
 * Minimal JSON serializer — the inverse of {@link SimpleJson}. Writes a nested tree of
 * {@code Map} / {@code List} / {@code Iterable} / {@code String} / {@code Number} /
 * {@code Boolean} / {@code null} to a compact JSON string, escaping strings via
 * {@link JsonStrings}.
 *
 * <p>Engine-owned and dependency-free — the counterpart for code that builds small JSON documents
 * (e.g. the addressable state query and the localhost test driver) without pulling in an external
 * JSON library. Object key order follows the map's iteration order, so pass a
 * {@link java.util.LinkedHashMap} when field order matters.</p>
 */
public final class SimpleJsonWriter {
    private SimpleJsonWriter() {
    }

    /** Serializes {@code value} to a compact JSON string. */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            sb.append(JsonStrings.quote(s));
        } else if (value instanceof Boolean b) {
            sb.append(b.booleanValue() ? "true" : "false");
        } else if (value instanceof Number n) {
            sb.append(numberText(n));
        } else if (value instanceof Map<?, ?> map) {
            writeObject(sb, map);
        } else if (value instanceof Iterable<?> iterable) {
            writeArray(sb, iterable);
        } else {
            sb.append(JsonStrings.quote(String.valueOf(value)));
        }
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(JsonStrings.quote(String.valueOf(entry.getKey()))).append(':');
            writeValue(sb, entry.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, Iterable<?> list) {
        sb.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            writeValue(sb, item);
        }
        sb.append(']');
    }

    private static String numberText(Number n) {
        if (n instanceof Double || n instanceof Float) {
            double d = n.doubleValue();
            if (!Double.isInfinite(d) && !Double.isNaN(d) && d == Math.rint(d)) {
                return Long.toString((long) d);
            }
            return Double.toString(d);
        }
        return n.toString();
    }

    /** Convenience {@code List} builder for output trees. */
    public static List<Object> array() {
        return new java.util.ArrayList<>();
    }

    /** Convenience insertion-ordered {@code Map} builder for output trees. */
    public static Map<String, Object> object() {
        return new java.util.LinkedHashMap<>();
    }
}
