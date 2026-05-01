package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.RouteDescriptor;
import com.eb.javafx.routing.SceneRouter;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;

/** Reusable main menu route that lists registered JavaFX routes. */
public final class MainMenuScreen {
    private MainMenuScreen() {
    }

    public static Scene createScene(RouteContext context) {
        if (context.primaryStage() != null) {
            context.primaryStage().setMinWidth(640);
            context.primaryStage().setMinHeight(480);
        }

        return ViewModelScreen.createScene(context, viewModel(context));
    }

    public static ScreenViewModel viewModel(RouteContext context) {
        List<String> lines = List.of(
                context.contentRegistry().definition("application.name"),
                context.contentRegistry().definition("ui.mainMenu.status"),
                "Startup phase complete. Reusable engine services, routes, and support foundations are ready.");
        List<ScreenActionViewModel> actions = new ArrayList<>();

        for (RouteDescriptor descriptor : context.sceneRouter().routeDescriptors().values()) {
            if (!SceneRouter.MAIN_MENU_ROUTE.equals(descriptor.id())) {
                actions.add(new ScreenActionViewModel(
                        context.contentRegistry().definition(descriptor.titleDefinition()),
                        descriptor.id(),
                        descriptor.migrated()));
            }
        }

        return new ScreenViewModel(context.contentRegistry().definition("ui.mainMenu.title"), lines, actions);
    }
}
