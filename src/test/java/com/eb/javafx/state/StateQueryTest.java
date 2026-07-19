package com.eb.javafx.state;

import com.eb.javafx.util.SimpleJson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StateQueryTest {

    @BeforeEach
    void setUp() {
        StateQuery.clear();
        StateQuery.hint("try t.num");
        StateQuery.register(new StateNamespace() {
            @Override public String name() {
                return "t";
            }
            @Override public Object resolve(String[] path) {
                String sub = path.length > 1 ? path[1] : null;
                if (sub == null) {
                    return "root";
                }
                switch (sub) {
                    case "num":  return 5;
                    case "flag": return true;
                    case "list": return List.of("x", "y");
                    case "obj": {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("k", "v");
                        return m;
                    }
                    case "bad":  return StateQuery.error("nope");
                    default:     return StateQuery.error("unknown-field:t." + sub);
                }
            }
        });
    }

    @AfterEach
    void tearDown() {
        StateQuery.clear();
    }

    @Test
    void plainValueNarrowsByType() {
        assertEquals(5L, StateQuery.plainValue("t.num"));          // Number → Long
        assertEquals(true, StateQuery.plainValue("t.flag"));
        assertEquals(List.of("x", "y"), StateQuery.plainValue("t.list"));
        assertEquals("root", StateQuery.plainValue("t"));
        assertNull(StateQuery.plainValue("t.obj"));                // compound object → null
        assertNull(StateQuery.plainValue("t.bad"));               // error map → null
        assertNull(StateQuery.plainValue("nope.x"));              // unknown namespace → null
    }

    @Test
    void queryWrapsValueInEnvelope() {
        Object parsed = SimpleJson.parse(StateQuery.query("t.num"), "q");
        Map<?, ?> root = (Map<?, ?>) parsed;
        assertEquals(Boolean.TRUE, root.get("ok"));
        assertEquals("t.num", root.get("path"));
        assertEquals(5L, ((Number) root.get("value")).longValue());
    }

    @Test
    void unknownNamespaceResolvesToErrorMapAsValue() {
        Object parsed = SimpleJson.parse(StateQuery.query("nope.x"), "q");
        Map<?, ?> root = (Map<?, ?>) parsed;
        assertEquals(Boolean.TRUE, root.get("ok"));
        Map<?, ?> value = (Map<?, ?>) root.get("value");
        assertEquals("unknown-namespace:nope", value.get("error"));
    }

    @Test
    void emptyPathListsRegisteredNamespaces() {
        Object parsed = SimpleJson.parse(StateQuery.query(""), "q");
        Map<?, ?> root = (Map<?, ?>) parsed;
        Map<?, ?> value = (Map<?, ?>) root.get("value");
        assertTrue(((List<?>) value.get("namespaces")).contains("t"));
        assertEquals("try t.num", value.get("hint"));
    }
}
