package com.eb.javafx.routing;

/**
 * Contributes one or more route descriptors and factories to a router.
 *
 * <p>Route modules mirror static content modules: they register deterministic
 * startup definitions without constructing mutable runtime state or JavaFX scenes
 * eagerly.</p>
 */
@FunctionalInterface
public interface RouteModule {
    /** Registers this module's routes with the supplied router. */
    void registerRoutes(SceneRouter router);
}
