package com.eb.javafx.routing;

import com.eb.javafx.ui.CaptureTestScreen;
import com.eb.javafx.ui.DisplayBindingsScreen;
import com.eb.javafx.ui.HudSummaryScreen;
import com.eb.javafx.ui.MainMenuScreen;
import com.eb.javafx.ui.PlaceholderScreen;
import com.eb.javafx.ui.PreferencesSummaryScreen;
import com.eb.javafx.ui.SaveLoadSummaryScreen;

/** Registers reusable engine-provided routes used by the initial JavaFX shell. */
public final class DefaultRouteModule implements RouteModule {
    @Override
    public void registerRoutes(SceneRouter router) {
        router.registerRoute(new RouteDescriptor(
                        SceneRouter.MAIN_MENU_ROUTE,
                        "ui.mainMenu.title",
                        RouteCategory.MENU,
                        true,
                        "Shell route with navigation to registered JavaFX placeholders."),
                MainMenuScreen::createScene);
        router.registerRoute(new RouteDescriptor(
                        SceneRouter.PREFERENCES_ROUTE,
                        "ui.preferences.title",
                        RouteCategory.SETTINGS,
                        false,
                        "Placeholder route showing startup preference values."),
                PreferencesSummaryScreen::createScene);
        router.registerRoute(new RouteDescriptor(
                        SceneRouter.SAVE_LOAD_ROUTE,
                        "ui.saveLoad.title",
                        RouteCategory.SAVE_LOAD,
                        false,
                        "Placeholder route showing explicit save schema metadata."),
                SaveLoadSummaryScreen::createScene);
        router.registerRoute(new RouteDescriptor(
                        SceneRouter.DIALOGUE_ROUTE,
                        "ui.dialogue.title",
                        RouteCategory.DIALOGUE,
                        false,
                        "Placeholder route for future say-window and dialogue executor integration."),
                context -> PlaceholderScreen.createScene(context,
                        "ui.dialogue.title",
                        "Dialogue text, speaker names, portraits, and say-window opacity will bind here."));
        router.registerRoute(new RouteDescriptor(
                        SceneRouter.CHOICE_ROUTE,
                        "ui.choice.title",
                        RouteCategory.DIALOGUE,
                        false,
                        "Placeholder route for future command-backed choice menus."),
                context -> PlaceholderScreen.createScene(context,
                        "ui.choice.title",
                        "Ren'Py menu choices will become route-backed command selections here."));
        router.registerRoute(new RouteDescriptor(
                        SceneRouter.HUD_ROUTE,
                        "ui.hud.title",
                        RouteCategory.HUD,
                        false,
                        "Placeholder route showing persistent HUD preference wiring."),
                HudSummaryScreen::createScene);
        router.registerRoute(new RouteDescriptor(
                        SceneRouter.NOTIFICATION_ROUTE,
                        "ui.notification.title",
                        RouteCategory.OVERLAY,
                        false,
                        "Placeholder route for notification and modal overlay layering."),
                context -> PlaceholderScreen.createScene(context,
                        "ui.notification.title",
                        "Notifications and modal overlays share this JavaFX layer strategy."));
        router.registerRoute(new RouteDescriptor(
                        SceneRouter.TOOLTIP_ROUTE,
                        "ui.tooltip.title",
                        RouteCategory.TOOLTIP,
                        false,
                        "Placeholder route for reusable tooltip panels."),
                context -> PlaceholderScreen.createScene(context,
                        "ui.tooltip.title",
                        "Reusable tooltip panels for domain data, actions, requirements, and generic help will render here."));
        router.registerRoute(new RouteDescriptor(
                        SceneRouter.DISPLAY_BINDINGS_ROUTE,
                        "ui.displayBindings.title",
                        RouteCategory.HUD,
                        true,
                        "Preview route showing parsed display bindings, resolved assets, layered character models, and animation profiles."),
                DisplayBindingsScreen::createScene);
        router.registerRoute(new RouteDescriptor(
                        SceneRouter.CAPTURE_TEST_ROUTE,
                        "ui.captureTest.title",
                        RouteCategory.MENU,
                        true,
                        "Small routed JavaFX screen that captures field values with buttons."),
                context -> CaptureTestScreen.createScene(
                        context.contentRegistry().definition("ui.captureTest.title"),
                        context.preferencesService(),
                        context.uiTheme(),
                        () -> context.navigateTo(SceneRouter.MAIN_MENU_ROUTE)));
    }
}
