package com.eb.javafx.scene;

/**
 * Registers reusable or application-provided scene definitions during bootstrap.
 *
 * <p>Implementations add validated scene definitions to the startup registry, allowing engine-provided demos
 * and application-authored content to share the same bootstrap extension point.</p>
 */
public interface SceneModule {
    void registerScenes(SceneRegistry sceneRegistry);

    default void validateScenes(SceneRegistry sceneRegistry) {
        // Registry-level validation catches reusable scene graph issues.
    }
}
