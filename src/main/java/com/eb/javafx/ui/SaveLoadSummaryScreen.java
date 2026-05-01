package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Reusable save/load route summarizing explicit save schema metadata. */
public final class SaveLoadSummaryScreen {
    private SaveLoadSummaryScreen() {
    }

    public static Scene createScene(RouteContext context) {
        VBox content = new VBox(8);
        content.getChildren().addAll(
                new Label("Save schema version: " + context.saveLoadService().schema().version()),
                new Label("Save directory: " + context.saveLoadService().schema().saveDirectory()),
                new Label("Transient UI state is intentionally excluded from save data."),
                ScreenNavigation.button(context, "Back to main menu", SceneRouter.MAIN_MENU_ROUTE));
        return context.themedScene(ScreenShell.titled(
                context.contentRegistry().definition("ui.saveLoad.title"), content));
    }
}
