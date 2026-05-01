package com.eb.javafx.bootstrap;

/**
 * Explicit startup phases replacing Ren'Py init priority and script load order.
 *
 * <p>The order mirrors docs/JAVAFX_PLAN.md section 1.1: core services must exist
 * before content registries, content must exist before game rules, routes must be
 * registered before the UI opens them, and mutable runtime state is created last so
 * it can depend on all earlier static data.</p>
 */
public enum BootstrapPhase {
    CORE_SERVICES,
    STATIC_CONTENT_REGISTRIES,
    GAME_RULES,
    UI_ROUTES_AND_CONTROLLERS,
    RUNTIME_STATE
}
