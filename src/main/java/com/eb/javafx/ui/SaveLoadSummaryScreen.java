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
    private SaveLoadSummaryScreen() {
    }

    public static Scene createScene(RouteContext context) {
        SaveLoadSummaryViewModel viewModel = viewModel(context);
        return context.themedSaveLoadScene(ScreenShell.titled(
                viewModel.screenViewModel().title(),
                ViewModelScreen.content(context, viewModel.screenViewModel())));
    }

    public static SaveLoadSummaryViewModel viewModel(RouteContext context) {
        return viewModel(
                ScreenTextResources.title(ScreenTextResources.SAVE_LOAD),
                context.saveLoadService());
    }

    public static SaveLoadSummaryViewModel viewModel(String title, SaveLoadService saveLoadService) {
        return new SaveLoadSummaryViewModel(
                title,
                saveLoadService.schema().version(),
                saveLoadService.schema().saveDirectory(),
                ScreenTextResources.text(ScreenTextResources.SAVE_LOAD, "line.transient-state-note"),
                List.of(new ScreenActionViewModel(
                        ScreenTextResources.text(ScreenTextResources.SAVE_LOAD, "item.back.label"),
                        SceneRouter.MAIN_MENU_ROUTE,
                        true)));
    }
}
