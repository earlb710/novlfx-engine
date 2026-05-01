package com.eb.javafx.gamesupport;

import java.util.List;

/** Generic authored-code entry used for reusable lists such as slots, roles, goals, or postures. */
public final class CodeDefinition implements IdentifiedDefinition {
    private final String id;
    private final String title;
    private final int sortOrder;
    private final List<String> tags;

    /**
     * Creates an immutable generic code entry.
     *
     * @param id stable non-blank code ID used by rules, authored data, and persistence
     * @param title non-blank player-facing or diagnostic title
     * @param sortOrder ordering value within the owning table
     * @param tags immutable copied metadata tags for grouping or validation
     */
    public CodeDefinition(String id, String title, int sortOrder, List<String> tags) {
        this.id = requireText(id, "id");
        this.title = requireText(title, "title");
        this.sortOrder = sortOrder;
        this.tags = List.copyOf(tags);
    }

    @Override
    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public List<String> tags() {
        return tags;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }
}
