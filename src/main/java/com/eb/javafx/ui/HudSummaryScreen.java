package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
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
        return ViewModelScreen.createScene(context, viewModel(context).screenViewModel());
    }

    public static HudSummaryViewModel viewModel(RouteContext context) {
        return viewModel(
                ScreenTextResources.title(ScreenTextResources.HUD),
                context.preferencesService());
    }

    public static HudSummaryViewModel viewModel(String title, PreferencesService preferencesService) {
        return new HudSummaryViewModel(
                title,
                ScreenTextResources.text(ScreenTextResources.HUD, "line.layer-description"),
                preferencesService.hudAlpha(),
                List.of(new ScreenActionViewModel(
                        ScreenTextResources.text(ScreenTextResources.HUD, "item.back.label"),
                        SceneRouter.MAIN_MENU_ROUTE,
                        true)));
    }
}
