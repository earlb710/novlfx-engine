package com.eb.javafx.scene;

/** Registers reusable or application-provided scene definitions during bootstrap. */
public interface SceneModule {
    void registerScenes(SceneRegistry sceneRegistry);

    default void validateScenes(SceneRegistry sceneRegistry) {
        // Registry-level validation catches reusable scene graph issues.
    }
}
