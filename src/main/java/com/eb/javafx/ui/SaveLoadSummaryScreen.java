package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import javafx.scene.Scene;

import java.util.List;

/**
 * Reusable save/load route model summarizing explicit save schema metadata.
 *
 * <p>The route reports known save slots, compatibility, and schema details from the save service, making the
 * reusable save foundation visible before an application provides a custom save browser.</p>
 */
public final class SaveLoadSummaryScreen {
    private static final String BACK_LABEL = "Back to main menu";
    private static final String TRANSIENT_STATE_NOTE = "Transient UI state is intentionally excluded from save data.";

    private SaveLoadSummaryScreen() {
    }

    public static Scene createScene(RouteContext context) {
        return ViewModelScreen.createScene(context, viewModel(context).screenViewModel());
    }

    public static SaveLoadSummaryViewModel viewModel(RouteContext context) {
        return viewModel(
                context.contentRegistry().definition("ui.saveLoad.title"),
                context.saveLoadService());
    }

    public static SaveLoadSummaryViewModel viewModel(String title, SaveLoadService saveLoadService) {
        return new SaveLoadSummaryViewModel(
                title,
                saveLoadService.schema().version(),
                saveLoadService.schema().saveDirectory(),
                TRANSIENT_STATE_NOTE,
                List.of(new ScreenActionViewModel(BACK_LABEL, SceneRouter.MAIN_MENU_ROUTE, true)));
    }
}
