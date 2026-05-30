package com.eb.javafx.storyline;

/**
 * Top-level categorisation for storyline event trees. Each {@link Storyline} carries one root tree
 * per category; consumers decide how to populate them. Inheritance of requirements down a tree is
 * always category-local — a {@link #LOCATION} root cannot inherit from a {@link #CHARACTER} root.
 *
 * <ul>
 *   <li>{@link #LOCATION} — events bound to a location id (e.g. {@code building/sub-room}).
 *       The tree's {@code categoryKey} is the location id at that level.</li>
 *   <li>{@link #CHARACTER} — events keyed by a character role id (e.g. {@code protagonist},
 *       {@code romance-target}, {@code mentor}).  Useful for relationship arcs.</li>
 *   <li>{@link #TIME} — events keyed by a time bucket (time-of-day, day index, season). Useful for
 *       scheduled or recurring beats.</li>
 *   <li>{@link #GENERIC} — events that don't slot into one of the above categories. The root tree
 *       carries no {@code categoryKey}; children can still be nested freely.</li>
 * </ul>
 */
public enum EventCategory {
    LOCATION,
    CHARACTER,
    TIME,
    GENERIC
}
