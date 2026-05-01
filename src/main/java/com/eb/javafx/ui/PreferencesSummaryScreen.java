package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.scene.Scene;

import java.util.List;

/** Reusable preferences route model summarizing startup preference values. */
public final class PreferencesSummaryScreen {
    private PreferencesSummaryScreen() {
    }

    public static Scene createScene(RouteContext context) {
        return ViewModelScreen.createScene(context, viewModel(context));
    }

    public static ScreenViewModel viewModel(RouteContext context) {
        PreferencesService preferencesService = context.preferencesService();
        return new ScreenViewModel(
                context.contentRegistry().definition("ui.preferences.title"),
                List.of(
                        "Window: " + preferencesService.windowWidth() + "x" + preferencesService.windowHeight(),
                        "HUD alpha: " + preferencesService.hudAlpha(),
                        "Say-window alpha: " + preferencesService.sayWindowAlpha(),
                        "Show portrait: " + preferencesService.showPortrait(),
                        "Cheats visible: " + preferencesService.cheatsVisible(),
                        "Log stat changes: " + preferencesService.logStatChanges(),
                        "Font family: " + preferencesService.fontFamily(),
                        "Font scale: " + preferencesService.fontScale(),
                        "High contrast: " + preferencesService.highContrast(),
                        "Reduced motion: " + preferencesService.reducedMotion(),
                        "Input mode: " + preferencesService.inputMode(),
                        "Master volume: " + preferencesService.masterVolume()),
                List.of(new ScreenActionViewModel("Back to main menu", SceneRouter.MAIN_MENU_ROUTE, true)));
    }
}
