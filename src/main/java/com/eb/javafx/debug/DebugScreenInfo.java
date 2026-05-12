package com.eb.javafx.debug;

/**
 * Immutable metadata describing the active scene for debug-mode inspection.
 *
 * <p>All fields default to an empty string when unknown so the debug overlay can render a stable shape
 * regardless of which fields the screen author chose to populate.</p>
 */
public record DebugScreenInfo(String routeId, String screenClass, String jsonFilePath) {
    public DebugScreenInfo {
        routeId = normalize(routeId);
        screenClass = normalize(screenClass);
        jsonFilePath = normalize(jsonFilePath);
    }

    public static DebugScreenInfo empty() {
        return new DebugScreenInfo("", "", "");
    }

    public static DebugScreenInfo forRoute(String routeId) {
        return new DebugScreenInfo(routeId, "", "");
    }

    public DebugScreenInfo withScreenClass(String screenClass) {
        return new DebugScreenInfo(routeId, screenClass, jsonFilePath);
    }

    public DebugScreenInfo withJsonFilePath(String jsonFilePath) {
        return new DebugScreenInfo(routeId, screenClass, jsonFilePath);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
