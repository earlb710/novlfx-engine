package com.eb.javafx.gamesupport;

import com.eb.javafx.util.Validation;

import java.util.List;
import java.util.Objects;

/**
 * Generic room/location metadata without authored map content or movement rules.
 * Location paths are map-aware when {@code mapId} is supplied, so a local
 * location ID such as {@code lobby} can be reused under different parents.
 */
public final class LocationDescriptor implements IdentifiedDefinition {
    private final String mapId;
    private final String id;
    private final String title;
    private final String routeId;
    private final String parentLocationId;
    private final List<String> tags;
    private final List<String> actionIds;
    private final String path;
    private final String parentPath;

    public LocationDescriptor(
            String id,
            String title,
            String routeId,
            String parentLocationId,
            List<String> tags,
            List<String> actionIds) {
        this(null, parentLocationId, id, title, routeId, tags, actionIds);
    }

    public LocationDescriptor(
            String mapId,
            String parentLocationId,
            String id,
            String title,
            String routeId,
            List<String> tags,
            List<String> actionIds) {
        this.mapId = normalizedOptionalId(mapId, "Location map ID must not be blank.");
        this.id = requirePathSegment(id, "Location ID must not be blank.");
        this.title = Validation.requireNonBlank(title, "Location title must not be blank.");
        this.routeId = Validation.requireNonBlank(routeId, "Location route ID must not be blank.");
        this.parentLocationId = normalizeParentLocationId(parentLocationId);
        this.tags = copyNonBlank(tags, "Location tag must not be blank.");
        this.actionIds = copyNonBlank(actionIds, "Location action ID must not be blank.");
        this.parentPath = resolveParentPath(this.mapId, this.parentLocationId);
        this.path = this.mapId == null
                ? this.id
                : this.parentPath == null ? rootPath(this.mapId, this.id) : this.parentPath + "." + this.id;
    }

    /**
     * Returns the local location ID. Use {@link #path()} for registry lookups
     * when locations are map-aware or nested.
     */
    @Override
    public String id() {
        return id;
    }

    public String mapId() {
        return mapId;
    }

    public String title() {
        return title;
    }

    public String routeId() {
        return routeId;
    }

    public String parentLocationId() {
        return parentLocationId;
    }

    public List<String> tags() {
        return tags;
    }

    public List<String> actionIds() {
        return actionIds;
    }

    /** Returns the unique map-aware path used by registries, movement, occupancy, and saves. */
    public String path() {
        return path;
    }

    /** Returns the resolved parent path, or {@code null} for root locations. */
    public String parentPath() {
        return parentPath;
    }

    private static String normalizedOptionalId(String value, String message) {
        if (value == null) {
            return null;
        }
        return requirePathSegment(value, message);
    }

    private static String requirePathSegment(String value, String message) {
        String checkedValue = Validation.requireNonBlank(value, message);
        if (checkedValue.contains(".")) {
            throw new IllegalArgumentException(message.replace("must not be blank", "must not contain dots"));
        }
        return checkedValue;
    }

    private static String normalizeParentLocationId(String parentLocationId) {
        if (parentLocationId == null) {
            return null;
        }
        String checkedParentId = Validation.requireNonBlank(parentLocationId, "Parent location ID must not be blank.");
        validatePath(checkedParentId, "Parent location ID path must not contain blank segments.");
        return checkedParentId;
    }

    private static void validatePath(String path, String message) {
        if (path.startsWith(".") || path.endsWith(".") || path.contains("..")) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String resolveParentPath(String mapId, String parentLocationId) {
        if (parentLocationId == null) {
            return null;
        }
        if (mapId == null || parentLocationId.equals(mapId) || parentLocationId.startsWith(mapId + ".")) {
            return parentLocationId;
        }
        return mapId + "." + parentLocationId;
    }

    private static String rootPath(String mapId, String id) {
        return mapId == null ? id : mapId + "." + id;
    }

    private static List<String> copyNonBlank(List<String> values, String message) {
        List<String> copiedValues = List.copyOf(Validation.requireNonNull(values, message));
        copiedValues.forEach(value -> Validation.requireNonBlank(value, message));
        return copiedValues;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LocationDescriptor that)) {
            return false;
        }
        return Objects.equals(mapId, that.mapId)
                && Objects.equals(id, that.id)
                && Objects.equals(title, that.title)
                && Objects.equals(routeId, that.routeId)
                && Objects.equals(parentLocationId, that.parentLocationId)
                && Objects.equals(tags, that.tags)
                && Objects.equals(actionIds, that.actionIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapId, id, title, routeId, parentLocationId, tags, actionIds);
    }

    @Override
    public String toString() {
        return "LocationDescriptor[mapId=%s, id=%s, title=%s, routeId=%s, parentLocationId=%s, tags=%s, actionIds=%s]"
                .formatted(mapId, id, title, routeId, parentLocationId, tags, actionIds);
    }
}
