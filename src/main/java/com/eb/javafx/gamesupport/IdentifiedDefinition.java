package com.eb.javafx.gamesupport;

/** Reusable contract for static section 2 definitions keyed by a stable ID. */
public interface IdentifiedDefinition {
    /** Returns the non-blank stable ID used by registries and authored content references. */
    String id();
}
