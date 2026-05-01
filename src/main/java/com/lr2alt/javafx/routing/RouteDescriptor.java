package com.lr2alt.javafx.routing;

/**
 * Metadata for a JavaFX route replacing a Ren'Py screen or label entry point.
 */
public final class RouteDescriptor {
    private final String id;
    private final String titleDefinition;
    private final RouteCategory category;
    private final boolean migrated;
    private final String status;

    public RouteDescriptor(
            String id,
            String titleDefinition,
            RouteCategory category,
            boolean migrated,
            String status) {
        this.id = id;
        this.titleDefinition = titleDefinition;
        this.category = category;
        this.migrated = migrated;
        this.status = status;
    }

    public String id() {
        return id;
    }

    public String titleDefinition() {
        return titleDefinition;
    }

    public RouteCategory category() {
        return category;
    }

    public boolean migrated() {
        return migrated;
    }

    public String status() {
        return status;
    }
}
