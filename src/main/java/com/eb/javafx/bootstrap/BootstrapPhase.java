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
    /** Core services are constructed, loaded, and ready for later phases. */
    CORE_SERVICES,
    /** Static content/display registries contain base and module-provided definitions. */
    STATIC_CONTENT_REGISTRIES,
    /** Required content, display definitions, and game-support rules have been validated. */
    GAME_RULES,
    /** JavaFX routes, route metadata, and global API adapters are registered. */
    UI_ROUTES_AND_CONTROLLERS,
    /** Mutable per-save runtime state has been created from validated static data. */
    RUNTIME_STATE
}
