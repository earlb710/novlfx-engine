package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.RouteDescriptor;
import com.eb.javafx.routing.SceneRouter;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Reusable main menu route that lists registered JavaFX routes. */
public final class MainMenuScreen {
    private MainMenuScreen() {
    }

    public static Scene createScene(RouteContext context) {
        VBox menu = new VBox(8);
        menu.setPadding(new Insets(8));
        menu.getChildren().addAll(
                new Label(context.contentRegistry().definition("application.name")),
                new Label(context.contentRegistry().definition("migration.slice")),
                new Label("Startup phase complete. Section 1.1 services, section 1.2 shell routes, and section 1.4-1.8 support foundations are ready."));

        for (RouteDescriptor descriptor : context.sceneRouter().routeDescriptors().values()) {
            if (!SceneRouter.MAIN_MENU_ROUTE.equals(descriptor.id())) {
                menu.getChildren().add(ScreenNavigation.button(
                        context,
                        context.contentRegistry().definition(descriptor.titleDefinition()),
                        descriptor.id()));
            }
        }

        if (context.primaryStage() != null) {
            context.primaryStage().setMinWidth(640);
            context.primaryStage().setMinHeight(480);
        }

        return context.themedScene(ScreenShell.titled(
                context.contentRegistry().definition("ui.mainMenu.title"), menu));
    }
}
