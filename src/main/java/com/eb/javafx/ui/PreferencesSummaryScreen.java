package com.eb.javafx.ui;

import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.scene.Scene;

import java.util.List;

/**
 * Reusable preferences route model summarizing startup preference values.
 *
 * <p>The route snapshots persisted window, UI, accessibility, input, and volume preferences into display
 * lines so users can verify startup state without editing the preferences service directly.</p>
 */
public final class PreferencesSummaryScreen {
    private static final String BACK_LABEL = "Back to main menu";

    private PreferencesSummaryScreen() {
    }

    public static Scene createScene(RouteContext context) {
        return ViewModelScreen.createScene(context, viewModel(context).screenViewModel());
    }

    public static PreferencesSummaryViewModel viewModel(RouteContext context) {
        return viewModel(
                context.contentRegistry().definition("ui.preferences.title"),
                context.preferencesService());
    }

    public static PreferencesSummaryViewModel viewModel(String title, PreferencesService preferencesService) {
        return new PreferencesSummaryViewModel(
                title,
                List.of(
                        new PreferencesSummaryRowViewModel("Window", preferencesService.windowWidth() + "x" + preferencesService.windowHeight()),
                        new PreferencesSummaryRowViewModel("HUD alpha", Double.toString(preferencesService.hudAlpha())),
                        new PreferencesSummaryRowViewModel("Say-window alpha", Double.toString(preferencesService.sayWindowAlpha())),
                        new PreferencesSummaryRowViewModel("Show portrait", Boolean.toString(preferencesService.showPortrait())),
                        new PreferencesSummaryRowViewModel("Cheats visible", Boolean.toString(preferencesService.cheatsVisible())),
                        new PreferencesSummaryRowViewModel("Log stat changes", Boolean.toString(preferencesService.logStatChanges())),
                        new PreferencesSummaryRowViewModel("Font family", preferencesService.fontFamily()),
                        new PreferencesSummaryRowViewModel("Font scale", Double.toString(preferencesService.fontScale())),
                        new PreferencesSummaryRowViewModel("High contrast", Boolean.toString(preferencesService.highContrast())),
                        new PreferencesSummaryRowViewModel("Reduced motion", Boolean.toString(preferencesService.reducedMotion())),
                        new PreferencesSummaryRowViewModel("Input mode", preferencesService.inputMode()),
                        new PreferencesSummaryRowViewModel("Master volume", Double.toString(preferencesService.masterVolume()))),
                List.of(new ScreenActionViewModel(BACK_LABEL, SceneRouter.MAIN_MENU_ROUTE, true)));
    }
}
