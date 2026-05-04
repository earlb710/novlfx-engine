package com.eb.javafx.testsupport;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.GameClock;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.scene.SceneChoice;
import com.eb.javafx.scene.SceneDefinition;
import com.eb.javafx.scene.SceneExecutor;
import com.eb.javafx.scene.SceneRegistry;
import com.eb.javafx.scene.SceneStep;
import com.eb.javafx.scene.SceneTransition;
import com.eb.javafx.state.GameState;

import java.util.List;

/** Generic scene-flow fixture builders for engine and downstream tests. */
public final class SceneFlowTestFixtures {
    private SceneFlowTestFixtures() {
    }

    public static SceneRegistry registryWith(SceneDefinition... definitions) {
        SceneRegistry registry = new SceneRegistry();
        for (SceneDefinition definition : definitions) {
            registry.register(definition);
        }
        registry.validateScenes();
        return registry;
    }

    public static SceneDefinition choiceScene(String sceneId, String targetSceneId) {
        return SceneDefinition.of(sceneId, List.of(
                SceneStep.choice("choice", List.of(SceneChoice.of("next", "choice.next", SceneTransition.jump(targetSceneId))))));
    }

    public static SceneDefinition completeScene(String sceneId) {
        return SceneDefinition.of(sceneId, List.of(
                SceneStep.narration("line", sceneId + ".line"),
                SceneStep.transition("complete", SceneTransition.complete())));
    }

    public static SceneExecutor executor(SceneRegistry registry) {
        return new SceneExecutor(registry);
    }

    public static ActionContext actionContext() {
        ContentRegistry contentRegistry = new ContentRegistry();
        contentRegistry.registerDefinition("startup.route", "startup");
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        return new ActionContext(new GameState("startup"), randomService, new GameClock());
    }
}
