package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Reusable HUD placeholder route wired to HUD preferences. */
public final class HudSummaryScreen {
    private HudSummaryScreen() {
    }

    public static Scene createScene(RouteContext context) {
        VBox content = new VBox(8);
        content.setOpacity(context.preferencesService().hudAlpha());
        content.getChildren().addAll(
                new Label("Persistent HUD layer placeholder"),
                new Label("HUD opacity is read from the JavaFX preferences service."),
                ScreenNavigation.button(context, "Back to main menu", SceneRouter.MAIN_MENU_ROUTE));
        return context.themedScene(ScreenShell.titled(
                context.contentRegistry().definition("ui.hud.title"), content));
    }
}
