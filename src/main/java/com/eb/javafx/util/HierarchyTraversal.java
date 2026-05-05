package com.eb.javafx.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** Generic traversal helpers for flat lists that model parent-child hierarchies. */
public final class HierarchyTraversal {
    private HierarchyTraversal() {
    }

    public static <T, K> List<T> depthFirst(
            List<T> nodes,
            Function<T, K> idExtractor,
            Function<T, K> parentIdExtractor,
            K rootParentId) {
        Validation.requireNonNull(nodes, "Hierarchy nodes are required.");
        Validation.requireNonNull(idExtractor, "Hierarchy id extractor is required.");
        Validation.requireNonNull(parentIdExtractor, "Hierarchy parent id extractor is required.");
        ArrayList<T> ordered = new ArrayList<>();
        appendChildren(ordered, nodes, idExtractor, parentIdExtractor, rootParentId);
        return List.copyOf(ordered);
    }

    public static <T, K> Set<K> descendantIds(
            List<T> nodes,
            Function<T, K> idExtractor,
            Function<T, K> parentIdExtractor,
            K rootId) {
        Validation.requireNonNull(nodes, "Hierarchy nodes are required.");
        Validation.requireNonNull(idExtractor, "Hierarchy id extractor is required.");
        Validation.requireNonNull(parentIdExtractor, "Hierarchy parent id extractor is required.");
        Validation.requireNonNull(rootId, "Hierarchy root id is required.");
        LinkedHashSet<K> descendants = new LinkedHashSet<>();
        ArrayDeque<K> pending = new ArrayDeque<>();
        descendants.add(rootId);
        pending.add(rootId);
        while (!pending.isEmpty()) {
            K parentId = pending.removeFirst();
            for (T node : nodes) {
                K nodeId = idExtractor.apply(node);
                if (Objects.equals(parentId, parentIdExtractor.apply(node)) && descendants.add(nodeId)) {
                    pending.addLast(nodeId);
                }
            }
        }
        return Set.copyOf(descendants);
    }

    private static <T, K> void appendChildren(
            List<T> ordered,
            List<T> nodes,
            Function<T, K> idExtractor,
            Function<T, K> parentIdExtractor,
            K parentId) {
        for (T node : nodes) {
            if (Objects.equals(parentId, parentIdExtractor.apply(node))) {
                ordered.add(node);
                appendChildren(ordered, nodes, idExtractor, parentIdExtractor, idExtractor.apply(node));
            }
        }
    }
}
