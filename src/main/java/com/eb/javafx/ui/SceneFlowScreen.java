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
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Diagnostic route showing section 1.3 scene-flow state without authored game content. */
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

        VBox content = new VBox(8);
        content.getChildren().addAll(
                new Label("Scene: " + viewModel.sceneId()),
                new Label("Status: " + viewModel.status()),
                new Label("Step: " + viewModel.stepId()),
                new Label("Speaker: " + (viewModel.speakerId() == null ? "narrator" : viewModel.speakerId())),
                new Label("Text key: " + viewModel.textDefinition()),
                new Label("Choices: " + viewModel.choices().size()));
        viewModel.choices().forEach(choice -> content.getChildren().add(new Label(
                choice.id() + " -> " + choice.textDefinition() + " [available=" + choice.available() + "]")));
        content.getChildren().add(ScreenNavigation.button(context, "Back to main menu", SceneRouter.MAIN_MENU_ROUTE));
        return context.themedScene(ScreenShell.titled(context.contentRegistry().definition(titleDefinition), content));
    }
}
