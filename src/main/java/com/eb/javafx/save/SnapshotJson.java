package com.eb.javafx.save;

import com.eb.javafx.util.JsonData;
import com.eb.javafx.util.JsonStrings;
import com.eb.javafx.util.Validation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class SnapshotJson {
    private SnapshotJson() {
    }

    static String object(Field... fields) {
        return java.util.Arrays.stream(fields)
                .map(field -> "  " + JsonStrings.quote(field.name()) + ": " + field.json())
                .collect(Collectors.joining(",\n", "{\n", "\n}"));
    }

    static Field field(String name, String json) {
        return new Field(name, json);
    }

    record Field(String name, String json) {
        Field {
            Validation.requireNonBlank(name, "Snapshot JSON field name is required.");
            Validation.requireNonNull(json, "Snapshot JSON field value is required.");
        }
    }

    static String stringArray(Collection<String> values) {
        return values.stream()
                .map(JsonStrings::quote)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    static String stringMap(Map<String, String> values) {
        return values.entrySet().stream()
                .map(entry -> JsonStrings.quote(entry.getKey()) + ": " + JsonStrings.quote(entry.getValue()))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    static String integerMap(Map<String, Integer> values) {
        return values.entrySet().stream()
                .map(entry -> JsonStrings.quote(entry.getKey()) + ": " + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    static Set<String> stringSet(Object value, String field) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String item : JsonData.stringList(value, field)) {
            result.add(item);
        }
        return Set.copyOf(result);
    }

    static Map<String, String> stringMap(Object value, String field) {
        return JsonData.stringMap(value, field);
    }

    static Map<String, Integer> integerMap(Object value, String field) {
        Map<String, Object> object = JsonData.requireObject(value, field);
        Map<String, Integer> result = new LinkedHashMap<>();
        object.forEach((key, entryValue) -> {
            Validation.requireNonBlank(key, field + " key is required.");
            if (!(entryValue instanceof Number number)) {
                throw new IllegalArgumentException("Expected JSON number for " + field + "." + key + ".");
            }
            result.put(key, number.intValue());
        });
        return Map.copyOf(result);
    }
}
