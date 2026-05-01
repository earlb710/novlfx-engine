package com.eb.javafx.scene;

import java.util.List;
import java.util.Map;

/** Registers reusable no-content demo scenes used by diagnostic dialogue/choice routes. */
public final class EnginePlaceholderSceneModule implements SceneModule {
    public static final String DEMO_DIALOGUE_SCENE = "engine.demo.dialogue";
    public static final String DEMO_CHOICE_SCENE = "engine.demo.choice";

    @Override
    public void registerScenes(SceneRegistry sceneRegistry) {
        sceneRegistry.register(new SceneDefinition(
                DEMO_DIALOGUE_SCENE,
                List.of(),
                List.of(
                        SceneStep.narration("intro", "scene.demo.dialogue.line"),
                        SceneStep.transition("to-choice", SceneTransition.jump(DEMO_CHOICE_SCENE))),
                Map.of("scope", "engine-placeholder")));
        sceneRegistry.register(new SceneDefinition(
                DEMO_CHOICE_SCENE,
                List.of(),
                List.of(SceneStep.choice("choice", List.of(
                        SceneChoice.of("continue", "scene.demo.choice.continue", SceneTransition.complete()),
                        SceneChoice.of("return", "scene.demo.choice.return", SceneTransition.jump(DEMO_DIALOGUE_SCENE))))),
                Map.of("scope", "engine-placeholder")));
    }
}
