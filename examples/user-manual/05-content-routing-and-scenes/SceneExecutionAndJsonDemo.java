import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.gamesupport.GameSupportService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.scene.SceneDefinition;
import com.eb.javafx.scene.SceneDefinitionJson;
import com.eb.javafx.scene.SceneExecutionResult;
import com.eb.javafx.scene.SceneExecutor;
import com.eb.javafx.scene.SceneFlowState;
import com.eb.javafx.scene.SceneFlowStateJson;
import com.eb.javafx.scene.ScenePresenter;
import com.eb.javafx.scene.SceneRegistry;
import com.eb.javafx.scene.SceneViewModel;
import com.eb.javafx.state.GameState;

import java.nio.file.Path;
import java.util.List;

public final class SceneExecutionAndJsonDemo {
    private SceneExecutionAndJsonDemo() {
    }

    public static void main(String[] args) {
        Path jsonPath = Path.of("examples/user-manual/05-content-routing-and-scenes/scene-definitions.demo.json")
                .toAbsolutePath()
                .normalize();

        List<SceneDefinition> scenes = SceneDefinitionJson.load(jsonPath);
        SceneRegistry sceneRegistry = new SceneRegistry();
        scenes.forEach(sceneRegistry::register);
        sceneRegistry.validateScenes();

        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        GameSupportService gameSupportService = new GameSupportService();
        gameSupportService.initialize();

        ActionContext actionContext = new ActionContext(
                new GameState("chapter-start"),
                randomService,
                gameSupportService.gameClock());
        SceneExecutor executor = new SceneExecutor(sceneRegistry);
        ScenePresenter presenter = new ScenePresenter();

        SceneExecutionResult intro = executor.advanceUntilPause(actionContext, executor.start("chapter-start"));
        SceneViewModel introView = presenter.present(actionContext, intro);
        System.out.println("Intro status: " + introView.status());
        System.out.println("Intro choices: " + introView.choices().stream().map(choice -> choice.id()).toList());

        SceneExecutionResult afterChoice = executor.selectChoice(actionContext, intro.state(), "ask-guide");
        SceneViewModel afterChoiceView = presenter.present(actionContext, afterChoice);
        System.out.println("Active scene after CALL transition: " + afterChoiceView.sceneId());
        System.out.println("Display reference from JSON: " + afterChoiceView.displayReference());

        String savedStateJson = SceneFlowStateJson.toJson(afterChoice.state());
        SceneFlowState restoredState = SceneFlowStateJson.fromJson(savedStateJson, jsonPath.toString());
        SceneExecutionResult resumed = executor.continueFromText(actionContext, restoredState);
        System.out.println("Scene after RETURN transition: " + resumed.state().activeSceneId());
        System.out.println("Round-trip scene JSON length: " + SceneDefinitionJson.toJson(scenes).length());
    }
}
