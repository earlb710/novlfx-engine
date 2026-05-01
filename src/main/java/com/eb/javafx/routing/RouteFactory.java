package com.eb.javafx.routing;

import javafx.scene.Scene;

/**
 * Builds a JavaFX scene for a route using the shared route context.
 *
 * <p>Factories keep screen/controller construction out of {@link SceneRouter}
 * so routes can be contributed by reusable engine modules or app/game modules.</p>
 */
@FunctionalInterface
public interface RouteFactory {
    /** Creates the scene for a route when that route is opened. */
    Scene createScene(RouteContext context);
}
