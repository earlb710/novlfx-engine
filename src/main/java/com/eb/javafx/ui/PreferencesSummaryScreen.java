package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Reusable preferences route summarizing startup preference values. */
public final class PreferencesSummaryScreen {
    private PreferencesSummaryScreen() {
    }

    public static Scene createScene(RouteContext context) {
        PreferencesService preferencesService = context.preferencesService();
        VBox content = new VBox(8);
        content.getChildren().addAll(
                new Label("Window: " + preferencesService.windowWidth() + "x" + preferencesService.windowHeight()),
                new Label("HUD alpha: " + preferencesService.hudAlpha()),
                new Label("Say-window alpha: " + preferencesService.sayWindowAlpha()),
                new Label("Show portrait: " + preferencesService.showPortrait()),
                new Label("Cheats visible: " + preferencesService.cheatsVisible()),
                new Label("Log stat changes: " + preferencesService.logStatChanges()),
                new Label("Font family: " + preferencesService.fontFamily()),
                new Label("Font scale: " + preferencesService.fontScale()),
                new Label("High contrast: " + preferencesService.highContrast()),
                new Label("Reduced motion: " + preferencesService.reducedMotion()),
                new Label("Input mode: " + preferencesService.inputMode()),
                new Label("Master volume: " + preferencesService.masterVolume()),
                ScreenNavigation.button(context, "Back to main menu", SceneRouter.MAIN_MENU_ROUTE));
        return context.themedScene(ScreenShell.titled(
                context.contentRegistry().definition("ui.preferences.title"), content));
    }
}
