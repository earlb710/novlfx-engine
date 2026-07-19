package com.eb.javafx.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SimpleJsonWriterTest {

    @Test
    void writesPrimitives() {
        assertEquals("null", SimpleJsonWriter.write(null));
        assertEquals("true", SimpleJsonWriter.write(true));
        assertEquals("false", SimpleJsonWriter.write(false));
        assertEquals("42", SimpleJsonWriter.write(42));
        assertEquals("42", SimpleJsonWriter.write(42L));
        assertEquals("3.5", SimpleJsonWriter.write(3.5));
        assertEquals("3", SimpleJsonWriter.write(3.0));   // whole double collapses to a long
        assertEquals("\"hi\"", SimpleJsonWriter.write("hi"));
    }

    @Test
    void escapesStrings() {
        assertEquals("\"a\\\"b\"", SimpleJsonWriter.write("a\"b"));
        assertEquals("\"line\\nbreak\"", SimpleJsonWriter.write("line\nbreak"));
        assertEquals("\"back\\\\slash\"", SimpleJsonWriter.write("back\\slash"));
    }

    @Test
    void writesArraysAndObjectsInOrder() {
        assertEquals("[1,2,3]", SimpleJsonWriter.write(List.of(1, 2, 3)));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("b", 1);
        m.put("a", "x");
        assertEquals("{\"b\":1,\"a\":\"x\"}", SimpleJsonWriter.write(m));
    }

    @Test
    void roundTripsThroughSimpleJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("name", "a\"b");
        m.put("nums", List.of(1, 2, 3));
        m.put("nested", Map.of("k", "v"));
        m.put("missing", null);

        Object parsed = SimpleJson.parse(SimpleJsonWriter.write(m), "round-trip");
        assertTrue(parsed instanceof Map<?, ?>);
        Map<?, ?> pm = (Map<?, ?>) parsed;
        assertEquals(Boolean.TRUE, pm.get("ok"));
        assertEquals("a\"b", pm.get("name"));
        assertEquals(3, ((List<?>) pm.get("nums")).size());
        assertEquals("v", ((Map<?, ?>) pm.get("nested")).get("k"));
        assertTrue(pm.containsKey("missing"));
        assertEquals(null, pm.get("missing"));
    }

    @Test
    void writesIterableAsArray() {
        List<Object> live = new ArrayList<>();
        live.add("x");
        live.add("y");
        assertEquals("[\"x\",\"y\"]", SimpleJsonWriter.write(live));
    }
}
