package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.scene.Scene;

import java.util.List;

/** Reusable save/load route model summarizing explicit save schema metadata. */
public final class SaveLoadSummaryScreen {
    private SaveLoadSummaryScreen() {
    }

    public static Scene createScene(RouteContext context) {
        return ViewModelScreen.createScene(context, viewModel(context));
    }

    public static ScreenViewModel viewModel(RouteContext context) {
        return new ScreenViewModel(
                context.contentRegistry().definition("ui.saveLoad.title"),
                List.of(
                        "Save schema version: " + context.saveLoadService().schema().version(),
                        "Save directory: " + context.saveLoadService().schema().saveDirectory(),
                        "Transient UI state is intentionally excluded from save data."),
                List.of(new ScreenActionViewModel("Back to main menu", SceneRouter.MAIN_MENU_ROUTE, true)));
    }
}
