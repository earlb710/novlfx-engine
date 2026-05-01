package com.eb.javafx.globalApi;

/**
 * Validated route or screen request that replaces direct engine-specific global API calls.
 */
public final class GlobalRouteRequest {
    private final GlobalRouteAction action;
    private final String routeId;

    GlobalRouteRequest(GlobalRouteAction action, String routeId) {
        this.action = action;
        this.routeId = routeId;
    }

    public GlobalRouteAction action() {
        return action;
    }

    public String routeId() {
        return routeId;
    }
}
