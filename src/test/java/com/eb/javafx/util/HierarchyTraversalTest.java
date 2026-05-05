package com.eb.javafx.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HierarchyTraversalTest {
    @Test
    void ordersFlatHierarchyDepthFirstFromParentId() {
        List<Node> nodes = List.of(
                new Node("root", null),
                new Node("sibling", null),
                new Node("child", "root"),
                new Node("grandchild", "child"));

        List<String> orderedIds = HierarchyTraversal.depthFirst(nodes, Node::id, Node::parentId, null).stream()
                .map(Node::id)
                .toList();

        assertEquals(List.of("root", "child", "grandchild", "sibling"), orderedIds);
    }

    @Test
    void findsDescendantIdsIncludingRoot() {
        List<Node> nodes = List.of(
                new Node("root", null),
                new Node("child", "root"),
                new Node("grandchild", "child"),
                new Node("sibling", null));

        assertEquals(Set.of("root", "child", "grandchild"),
                HierarchyTraversal.descendantIds(nodes, Node::id, Node::parentId, "root"));
    }

    private record Node(String id, String parentId) {
    }
}
