package com.eb.javafx.ui;

import com.eb.javafx.audio.AudioService;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.prefs.PreferencesService.ThemeFamily;
import com.eb.javafx.prefs.PreferencesService.ThemeVariant;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Reusable preferences route that summarizes startup preferences and exposes theme selection.
 */
public final class PreferencesSummaryScreen {
    private static final String BACK_LABEL = "Back to main menu";
    private static final double VOLUME_PERCENT_SCALE = 100.0;

    private PreferencesSummaryScreen() {
    }

    public static Scene createScene(RouteContext context) {
        PreferencesSummaryViewModel viewModel = viewModel(context);
        VBox content = new VBox(10);
        for (PreferencesSummaryRowViewModel row : viewModel.rows()) {
            content.getChildren().add(new Label(row.line()));
        }

        Label audioHeading = new Label("Audio");
        audioHeading.getStyleClass().add(ScreenShell.LAYOUT_SECTION_TITLE_STYLE_CLASS);
        audioHeading.setPadding(new Insets(8, 0, 0, 0));
        content.getChildren().add(audioHeading);
        content.getChildren().add(volumeRow(context, "Music volume", context.preferencesService().musicVolume(),
                PreferencesSummaryScreen::saveMusicVolume));
        content.getChildren().add(volumeRow(context, "Sound volume", context.preferencesService().soundVolume(),
                PreferencesSummaryScreen::saveSoundVolume));

        Label themeHeading = new Label("Theme color");
        themeHeading.getStyleClass().add(ScreenShell.LAYOUT_SECTION_TITLE_STYLE_CLASS);
        themeHeading.setPadding(new Insets(8, 0, 0, 0));
        content.getChildren().add(themeHeading);

        Label themeHelp = new Label("Choose one of four theme colors with dark or light pastel variants.");
        content.getChildren().add(themeHelp);

        for (ThemeFamily family : ThemeFamily.values()) {
            content.getChildren().add(themeFamilyRow(context, family));
        }

        Button backButton = ScreenNavigation.button(context, BACK_LABEL, SceneRouter.MAIN_MENU_ROUTE);
        content.getChildren().add(backButton);
        return context.themedScene(ScreenShell.titled(viewModel.title(), content));
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
                        new PreferencesSummaryRowViewModel("Footer shortcut display", preferencesService.footerShortcutDisplay().label()),
                        new PreferencesSummaryRowViewModel("Font family", preferencesService.fontFamily()),
                        new PreferencesSummaryRowViewModel("Font scale", Double.toString(preferencesService.fontScale())),
                        new PreferencesSummaryRowViewModel("Music volume", percentLabel(preferencesService.musicVolume())),
                        new PreferencesSummaryRowViewModel("Sound volume", percentLabel(preferencesService.soundVolume())),
                        new PreferencesSummaryRowViewModel("Theme color", preferencesService.themeFamily().label()),
                        new PreferencesSummaryRowViewModel("Theme variant", preferencesService.themeVariant().label()),
                        new PreferencesSummaryRowViewModel("High contrast", Boolean.toString(preferencesService.highContrast())),
                        new PreferencesSummaryRowViewModel("Reduced motion", Boolean.toString(preferencesService.reducedMotion())),
                        new PreferencesSummaryRowViewModel("Input mode", preferencesService.inputMode()),
                        new PreferencesSummaryRowViewModel("Master volume", Double.toString(preferencesService.masterVolume()))),
                List.of(new ScreenActionViewModel(BACK_LABEL, SceneRouter.MAIN_MENU_ROUTE, true)));
    }

    static List<String> themeOptionLabels() {
        return List.of(
                "Ocean - Dark",
                "Ocean - Light pastel",
                "Forest - Dark",
                "Forest - Light pastel",
                "Sunset - Dark",
                "Sunset - Light pastel",
                "Violet - Dark",
                "Violet - Light pastel");
    }

    static String percentLabel(double volume) {
        return Math.round(volume * VOLUME_PERCENT_SCALE) + "%";
    }

    private static HBox volumeRow(
            RouteContext context,
            String labelText,
            double currentVolume,
            VolumeSaver volumeSaver) {
        Label label = new Label(labelText);
        Slider slider = new Slider(0, VOLUME_PERCENT_SCALE, currentVolume * VOLUME_PERCENT_SCALE);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        slider.setBlockIncrement(10);
        Label value = new Label(percentLabel(currentVolume));
        slider.valueProperty().addListener((observable, previous, current) ->
                value.setText(Math.round(current.doubleValue()) + "%"));
        Button save = ButtonVisuals.applySvgArtwork(new Button("Save"));
        save.setOnAction(event -> {
            volumeSaver.save(context, slider.getValue() / VOLUME_PERCENT_SCALE);
            value.setText(percentLabel(slider.getValue() / VOLUME_PERCENT_SCALE));
        });
        return new HBox(8, label, slider, value, save);
    }

    private static VBox themeFamilyRow(RouteContext context, ThemeFamily family) {
        VBox container = new VBox(6);
        Label label = new Label(family.label());
        HBox buttonRow = new HBox(8,
                themeButton(context, family, ThemeVariant.DARK),
                themeButton(context, family, ThemeVariant.LIGHT_PASTEL));
        container.getChildren().addAll(label, buttonRow);
        return container;
    }

    private static Button themeButton(RouteContext context, ThemeFamily family, ThemeVariant variant) {
        Button button = ButtonVisuals.applySvgArtwork(new Button(variant.label()));
        button.setDisable(context.preferencesService().themeFamily() == family
                && context.preferencesService().themeVariant() == variant);
        button.setOnAction(event -> applyTheme(context, family, variant));
        return button;
    }

    private static void applyTheme(RouteContext context, ThemeFamily family, ThemeVariant variant) {
        context.preferencesService().saveThemePreferences(family, variant);
        context.uiTheme().initialize(context.preferencesService());
        context.navigateTo(SceneRouter.PREFERENCES_ROUTE);
    }

    private static void saveMusicVolume(RouteContext context, double volume) {
        context.preferencesService().saveMusicVolume(volume);
        AudioService audioService = context.audioService();
        if (audioService != null && audioService.isInitialized()) {
            audioService.setChannelVolume(AudioService.MUSIC_CHANNEL, context.preferencesService().musicVolume());
        }
    }

    private static void saveSoundVolume(RouteContext context, double volume) {
        context.preferencesService().saveSoundVolume(volume);
        AudioService audioService = context.audioService();
        if (audioService != null && audioService.isInitialized()) {
            audioService.setChannelVolume(AudioService.SOUND_CHANNEL, context.preferencesService().soundVolume());
        }
    }

    @FunctionalInterface
    private interface VolumeSaver {
        void save(RouteContext context, double volume);
    }
}
