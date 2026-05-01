package com.lr2alt.javafx.renpy;

/**
 * Validated route or screen request that replaces direct {@code renpy.*} calls.
 */
public final class RenpyRouteRequest {
    private final RenpyRouteAction action;
    private final String routeId;

    RenpyRouteRequest(RenpyRouteAction action, String routeId) {
        this.action = action;
        this.routeId = routeId;
    }

    public RenpyRouteAction action() {
        return action;
    }

    public String routeId() {
        return routeId;
    }
}
