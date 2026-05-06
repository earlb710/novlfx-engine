package com.eb.javafx.ui;

import com.eb.javafx.gamesupport.ActionContext;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.scene.EnginePlaceholderSceneModule;
import com.eb.javafx.scene.SceneCheckpointSession;
import com.eb.javafx.scene.SceneExecutionResult;
import com.eb.javafx.scene.SceneExecutionStatus;
import com.eb.javafx.scene.SceneExecutor;
import com.eb.javafx.scene.ScenePresenter;
import com.eb.javafx.scene.SceneViewModel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

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
        SceneCheckpointSession session = new SceneCheckpointSession(executor, actionContext);
        session.start(sceneId);
        ScenePresenter presenter = new ScenePresenter();
        VBox content = new VBox(ScreenShell.BODY_SPACING);
        Button mainMenu = ScreenNavigation.button(context, "Back to main menu", SceneRouter.MAIN_MENU_ROUTE);
        Runnable[] render = new Runnable[1];
        Runnable backward = () -> {
            if (session.rollbackAllowed()) {
                session.rollbackOneCheckpoint();
                render[0].run();
            }
        };
        Runnable forward = () -> {
            SceneExecutionResult current = session.currentResult();
            if (session.rollForwardAllowed()) {
                session.rollForwardUsingStoredCheckpointData();
            } else if (current != null && current.status() == SceneExecutionStatus.DISPLAYING_TEXT) {
                session.continueFromText();
            }
            render[0].run();
        };
        render[0] = () -> {
            SceneViewModel viewModel = presenter.present(actionContext, session.currentResult(), session.checkpointLog());
            VBox sceneContent = SceneFlowView.createContent(viewModel, choiceId -> {
                session.selectChoice(choiceId);
                render[0].run();
            }, backward, forward);
            content.getChildren().setAll(sceneContent, mainMenu);
        };
        render[0].run();
        return context.themedScene(ScreenShell.titled(context.contentRegistry().definition(titleDefinition), content));
    }
}
