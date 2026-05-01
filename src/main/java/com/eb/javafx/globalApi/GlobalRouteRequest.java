package com.eb.javafx.globalApi;

/**
 * Validated route or screen request that replaces direct engine-specific global API calls.
 *
 * <p>Instances are package-private to create so {@link GlobalApiAdapter} can
 * validate route IDs against the scene router before exposing requests to migrated code.</p>
 */
public final class GlobalRouteRequest {
    private final GlobalRouteAction action;
    private final String routeId;

    /** Creates a request after adapter-level route validation has succeeded. */
    GlobalRouteRequest(GlobalRouteAction action, String routeId) {
        this.action = action;
        this.routeId = routeId;
    }

    /** Returns the requested navigation or screen action. */
    public GlobalRouteAction action() {
        return action;
    }

    /** Returns the validated route ID targeted by the action. */
    public String routeId() {
        return routeId;
    }
}
