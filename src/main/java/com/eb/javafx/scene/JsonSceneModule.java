package com.eb.javafx.scene;

import com.eb.javafx.util.Validation;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

/** Scene module that registers data-only scene definitions from app-owned JSON. */
public final class JsonSceneModule implements SceneModule {
    private final List<SceneDefinition> scenes;

    public JsonSceneModule(Path jsonPath) {
        this(SceneDefinitionJson.load(jsonPath));
    }

    public JsonSceneModule(URL jsonUrl) {
        this(SceneDefinitionJson.load(jsonUrl));
    }

    public JsonSceneModule(List<SceneDefinition> scenes) {
        this.scenes = List.copyOf(Validation.requireNonNull(scenes, "Scene definitions are required."));
    }

    @Override
    public void registerScenes(SceneRegistry sceneRegistry) {
        scenes.forEach(sceneRegistry::register);
    }

    @Override
    public void validateScenes(SceneRegistry sceneRegistry) {
        scenes.forEach(scene -> sceneRegistry.requireScene(scene.id()));
    }

    public List<SceneDefinition> scenes() {
        return scenes;
    }
}
