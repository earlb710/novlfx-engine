package com.eb.javafx.ui;

import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.scene.Scene;

import java.util.List;

/**
 * Reusable HUD route model wired to HUD preferences.
 *
 * <p>The route reads visibility and opacity preferences and turns them into a neutral summary model, giving
 * applications a working HUD diagnostics screen before they add authored overlays.</p>
 */
public final class HudSummaryScreen {
    private HudSummaryScreen() {
    }

    public static Scene createScene(RouteContext context) {
        return ViewModelScreen.createScene(context, viewModel(context));
    }

    public static ScreenViewModel viewModel(RouteContext context) {
        return new ScreenViewModel(
                context.contentRegistry().definition("ui.hud.title"),
                List.of(
                        "Persistent HUD layer",
                        "HUD opacity: " + context.preferencesService().hudAlpha()),
                List.of(new ScreenActionViewModel("Back to main menu", SceneRouter.MAIN_MENU_ROUTE, true)));
    }
}
