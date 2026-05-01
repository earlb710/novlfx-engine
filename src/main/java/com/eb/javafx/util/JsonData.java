package com.eb.javafx.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Shared typed accessors for small JSON documents parsed with {@link SimpleJson}. */
public final class JsonData {
    private JsonData() {
    }

    public static Map<String, Object> rootObject(String json, String sourceName) {
        return requireObject(SimpleJson.parse(json, sourceName), "root");
    }

    public static Map<String, Object> requireObject(Object value, String description) {
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
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

    public static Optional<Map<String, Object>> optionalObject(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key) || object.get(key) == null) {
            return Optional.empty();
        }
        return Optional.of(requireObject(object.get(key), description));
    }

    public static List<Object> optionalList(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key) || object.get(key) == null) {
            return List.of();
        }
        return requireList(object.get(key), description);
    }

    public static List<Object> requireList(Object value, String description) {
        if (value instanceof List<?> list) {
            return List.copyOf(list);
        }
        throw new IllegalArgumentException("Expected JSON array for " + description + ".");
    }

    public static String requiredString(Map<String, Object> object, String key, String description) {
        return Validation.requireNonBlank(optionalString(object, key, description)
                .orElseThrow(() -> new IllegalArgumentException("Missing JSON string for " + description + ".")),
                description + " must not be blank.");
    }

    public static Optional<String> optionalString(Map<String, Object> object, String key, String description) {
        if (!object.containsKey(key) || object.get(key) == null) {
            return Optional.empty();
        }
        Object value = object.get(key);
        if (value instanceof String stringValue) {
            return Optional.of(Validation.requireNonBlank(stringValue, description + " must not be blank."));
        }
        throw new IllegalArgumentException("Expected JSON string for " + description + ".");
    }

    public static int requiredInt(Map<String, Object> object, String key, String description) {
        Object value = object.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Expected JSON number for " + description + ".");
    }

    public static double optionalDouble(Map<String, Object> object, String key, double defaultValue, String description) {
        if (!object.containsKey(key) || object.get(key) == null) {
            return defaultValue;
        }
        Object value = object.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        throw new IllegalArgumentException("Expected JSON number for " + description + ".");
    }

    public static List<String> stringList(Object value, String description) {
        List<String> strings = new ArrayList<>();
        int index = 0;
        for (Object entry : requireList(value, description)) {
            if (!(entry instanceof String stringEntry) || stringEntry.isBlank()) {
                throw new IllegalArgumentException("Expected non-blank JSON string for " + description + "[" + index + "].");
            }
            strings.add(stringEntry);
            index++;
        }
        return List.copyOf(strings);
    }

    public static Map<String, String> stringMap(Object value, String description) {
        LinkedHashMap<String, String> strings = new LinkedHashMap<>();
        requireObject(value, description).forEach((key, entryValue) -> {
            if (!(entryValue instanceof String stringValue) || stringValue.isBlank()) {
                throw new IllegalArgumentException("Expected non-blank JSON string for " + description + "." + key + ".");
            }
            strings.put(key, stringValue);
        });
        return Map.copyOf(strings);
    }

    public static <E extends Enum<E>> E enumValue(Class<E> enumType, String value, String description) {
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown " + description + ": " + value, exception);
        }
    }
}
