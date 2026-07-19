package com.eb.javafx.state;

/**
 * SPI for one addressable state namespace read by {@link StateQuery}.
 *
 * <p>The game registers one namespace per top-level key (e.g. {@code "mc"}, {@code "time"},
 * {@code "current"}). {@link #resolve} receives the full dot-split path — {@code path[0]} is always
 * this namespace's {@link #name()} — and returns a plain value tree the engine can serialize and
 * compare: {@code Map<String,Object>} / {@code List<?>} / {@code String} / {@code Number} /
 * {@code Boolean} / {@code null}. Signal a lookup error by returning {@link StateQuery#error(String)}
 * (an {@code {"error": msg}} map), which the query surfaces as the resolved value.</p>
 *
 * <p>Called on the JavaFX application thread (namespaces read live game state).</p>
 */
public interface StateNamespace {

    /** The top-level path key this namespace answers, e.g. {@code "mc"}. */
    String name();

    /**
     * Resolves the dot-split {@code path} ({@code path[0] == name()}) to a plain value tree, or a
     * {@link StateQuery#error(String)} map when the sub-path is unknown.
     */
    Object resolve(String[] path);
}
