package com.eb.javafx.routing;

/**
 * Metadata for a JavaFX route representing a screen or label entry point.
 *
 * <p>The descriptor records the stable route ID, content definition used for the
 * title, high-level category, whether the route is fully migrated, and human
 * status text for diagnostics or route inventory screens.</p>
 */
public final class RouteDescriptor {
    private final String id;
    private final String titleDefinition;
    private final RouteCategory category;
    private final boolean migrated;
    private final String status;

    /**
     * Creates route metadata.
     *
     * @param id stable route ID used by {@link SceneRouter#open(String)}
     * @param titleDefinition content-registry key for the route title
     * @param category route grouping for diagnostics and navigation
     * @param migrated whether this route is implemented rather than a placeholder
     * @param status human-readable migration or behavior note
     */
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

    /** Returns whether the route is considered migrated rather than placeholder-only. */
    public boolean migrated() {
        return migrated;
    }

    public String status() {
        return status;
    }
}
