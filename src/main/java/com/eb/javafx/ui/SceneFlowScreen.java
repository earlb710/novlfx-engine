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
        Button mainMenu = ScreenNavigation.button(context, screenText(titleDefinition, "item.back.label"), SceneRouter.MAIN_MENU_ROUTE);
        renderCheckpointScene(actionContext, session, presenter, content, mainMenu);
        return context.themedScene(ScreenShell.titled(title(titleDefinition), content));
    }

    private static void renderCheckpointScene(
            ActionContext actionContext,
            SceneCheckpointSession session,
            ScenePresenter presenter,
            VBox content,
            Button mainMenu) {
        SceneViewModel viewModel = presenter.present(actionContext, session.currentResult(), session.checkpointLog());
        VBox sceneContent = SceneFlowView.createContent(viewModel, choiceId -> {
            session.selectChoice(choiceId);
            renderCheckpointScene(actionContext, session, presenter, content, mainMenu);
        }, () -> {
            if (session.rollbackAllowed()) {
                session.rollbackOneCheckpoint();
                renderCheckpointScene(actionContext, session, presenter, content, mainMenu);
            }
        }, () -> {
            SceneExecutionResult current = session.currentResult();
            if (session.rollForwardAllowed()) {
                session.rollForwardUsingStoredCheckpointData();
            } else if (current != null && current.status() == SceneExecutionStatus.DISPLAYING_TEXT) {
                session.continueFromText();
            }
            renderCheckpointScene(actionContext, session, presenter, content, mainMenu);
        });
        content.getChildren().setAll(sceneContent, mainMenu);
    }

    private static String title(String titleDefinition) {
        return ScreenTextResources.title(screenId(titleDefinition));
    }

    private static String screenText(String titleDefinition, String key) {
        return ScreenTextResources.text(screenId(titleDefinition), key);
    }

    private static String screenId(String titleDefinition) {
        return "ui.choice.title".equals(titleDefinition) ? ScreenTextResources.CHOICE : ScreenTextResources.DIALOGUE;
    }
}
