package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Generic reusable status screen for registered routes not yet fully migrated. */
public final class PlaceholderScreen {
    private PlaceholderScreen() {
    }

    public static Scene createScene(RouteContext context, String titleDefinition, String bodyText) {
        VBox content = new VBox(8);
        content.getChildren().addAll(
                new Label(bodyText),
                ScreenNavigation.button(context, "Back to main menu", SceneRouter.MAIN_MENU_ROUTE));
        return context.themedScene(ScreenShell.titled(
                context.contentRegistry().definition(titleDefinition), content));
    }
}
