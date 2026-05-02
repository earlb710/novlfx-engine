package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.scene.EnginePlaceholderSceneModule;
import com.eb.javafx.scene.SceneExecutionResult;
import com.eb.javafx.scene.SceneExecutor;
import com.eb.javafx.scene.SceneFlowState;
import com.eb.javafx.scene.ScenePresenter;
import com.eb.javafx.scene.SceneViewModel;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic route showing section 1.3 scene-flow state without authored game content.
 *
 * <p>The screen starts a placeholder scene flow, advances to the first UI-visible executor result, and renders
 * the neutral presentation state for smoke testing dialogue and choice routing.</p>
 */
public final class SceneFlowScreen {
    private SceneFlowScreen() {
    }

    public static Scene createDialogueScene(RouteContext context) {
        return createScene(context, "ui.dialogue.title", EnginePlaceholderSceneModule.DEMO_DIALOGUE_SCENE);
    }

    public static Scene createChoiceScene(RouteContext context) {
        return createScene(context, "ui.choice.title", EnginePlaceholderSceneModule.DEMO_CHOICE_SCENE);
    }

    private static Scene createScene(RouteContext context, String titleDefinition, String sceneId) {
        ActionContext actionContext = new ActionContext(
                context.gameState(),
                context.randomService(),
                context.gameSupportService().gameClock());
        SceneExecutor executor = context.sceneExecutor();
        SceneExecutionResult result = executor.advanceUntilPause(actionContext, SceneFlowState.start(sceneId));
        SceneViewModel viewModel = new ScenePresenter().present(actionContext, result);

        List<String> lines = new ArrayList<>(List.of(
                "Scene: " + viewModel.sceneId(),
                "Status: " + viewModel.status(),
                "Step: " + viewModel.stepId(),
                "Speaker: " + (viewModel.speakerId() == null ? "narrator" : viewModel.speakerId()),
                "Text key: " + viewModel.textDefinition(),
                "Choices: " + viewModel.choices().size()));
        viewModel.choices().forEach(choice -> lines.add(
                choice.id() + " -> " + choice.textDefinition() + " [available=" + choice.available() + "]"));
        return ViewModelScreen.createScene(context, new ScreenViewModel(
                context.contentRegistry().definition(titleDefinition),
                lines,
                List.of(new ScreenActionViewModel("Back to main menu", SceneRouter.MAIN_MENU_ROUTE, true))));
    }
}
