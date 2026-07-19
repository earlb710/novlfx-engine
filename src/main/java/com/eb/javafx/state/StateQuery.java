package com.eb.javafx.state;

import com.eb.javafx.util.SimpleJsonWriter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Engine-generic addressable read-facade over live game state. A dot-path (e.g. {@code mc.energy},
 * {@code current.room.people}) resolves through a registered {@link StateNamespace} to a plain value
 * tree, which callers read as JSON ({@link #query}), as the raw tree ({@link #value}), or as a
 * single comparable value ({@link #plainValue}).
 *
 * <p>The game registers one namespace per top-level key at boot ({@link #register}); every consumer
 * then shares the same resolver — a dialog-choice state gate
 * ({@code DialogChoiceRequirement.Resolver#stateValue}), the test driver's {@code /state} endpoint,
 * and any tooling — so there is a single source of truth and no drift.</p>
 *
 * <p><b>FX-thread only</b> — namespaces read live, mutable game state.</p>
 */
public final class StateQuery {

    private static final Map<String, StateNamespace> NAMESPACES = new ConcurrentHashMap<>();
    private static volatile String hint = "";

    private StateQuery() {
    }

    /** Registers (or replaces) a namespace by its {@link StateNamespace#name()}. */
    public static void register(StateNamespace namespace) {
        if (namespace != null && namespace.name() != null && !namespace.name().isBlank()) {
            NAMESPACES.put(namespace.name(), namespace);
        }
    }

    /** Removes every registered namespace — for test isolation. */
    public static void clear() {
        NAMESPACES.clear();
    }

    /** Sets the hint string shown in the empty-path namespace listing. */
    public static void hint(String value) {
        hint = value == null ? "" : value;
    }

    /**
     * Resolves {@code path} and returns a compact JSON string with an {@code {ok,path,value}}
     * envelope. An empty/blank path returns the namespace listing. A resolver failure yields
     * {@code {"ok":false,"error":...}}.
     */
    public static String query(String path) {
        Object value;
        try {
            value = resolve(path == null ? "" : path.trim());
        } catch (RuntimeException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("ok", false);
            err.put("error", "resolve-failed:" + e);
            return SimpleJsonWriter.write(err);
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("ok", true);
        if (path != null && !path.isBlank()) {
            root.put("path", path.trim());
        }
        root.put("value", value);
        return SimpleJsonWriter.write(root);
    }

    /** Resolves {@code path} to the raw value tree (Map / List / String / Number / Boolean / null). */
    public static Object value(String path) {
        return resolve(path == null ? "" : path.trim());
    }

    /**
     * Resolves {@code path} to a single comparable value — {@code Long} / {@code Boolean} /
     * {@code String} / {@code List<String>}, or {@code null} when the path resolves to nothing
     * usable (missing, an error map, or a compound object). This is what a dialog-choice state gate
     * compares against.
     */
    public static Object plainValue(String path) {
        Object v = value(path);
        if (v instanceof Boolean || v instanceof String) {
            return v;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object e : list) {
                out.add(String.valueOf(e));
            }
            return out;
        }
        return null;   // compound objects (a whole room / npc) and error maps aren't comparable
    }

    private static Object resolve(String path) {
        if (path.isEmpty()) {
            return namespaces();
        }
        String[] parts = path.split("\\.");
        StateNamespace namespace = NAMESPACES.get(parts[0]);
        return namespace == null ? error("unknown-namespace:" + parts[0]) : namespace.resolve(parts);
    }

    private static Map<String, Object> namespaces() {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("namespaces", new ArrayList<>(NAMESPACES.keySet()));
        if (!hint.isBlank()) {
            n.put("hint", hint);
        }
        return n;
    }

    /** A namespace-level error value ({@code {"error": msg}}) — embedded as the resolved value. */
    public static Map<String, Object> error(String message) {
        Map<String, Object> n = new LinkedHashMap<>();
        n.put("error", message);
        return n;
    }
}
