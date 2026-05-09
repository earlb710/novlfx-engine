package com.eb.javafx.ui;

import com.eb.javafx.audio.AudioService;
import com.eb.javafx.prefs.PreferencesService.FooterShortcutDisplay;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.prefs.PreferencesService.ThemeFamily;
import com.eb.javafx.prefs.PreferencesService.ThemeVariant;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Set;

/**
 * Reusable preferences route that summarizes startup preferences and exposes theme selection.
 */
public final class PreferencesSummaryScreen {
    private static final String CLOSE_LABEL = "Close";
    private static final String PREFERENCES_ID = "preferences";
    private static final Set<String> ENABLED_FOOTER_IDS = Set.of(PREFERENCES_ID);
    private static final double VOLUME_PERCENT_SCALE = 100.0;

    private PreferencesSummaryScreen() {
    }

    public static Scene createScene(RouteContext context) {
        PreferencesSummaryViewModel viewModel = viewModel(context);
        Runnable closeAction = () -> context.navigateTo(SceneRouter.MAIN_MENU_ROUTE);
        VBox content = new VBox(10);
        content.getChildren().add(sectionHeading("Audio"));
        content.getChildren().add(volumeRow(context, "Master volume", context.preferencesService().masterVolume(),
                PreferencesSummaryScreen::saveMasterVolume));
        content.getChildren().add(volumeRow(context, "Music volume", context.preferencesService().musicVolume(),
                PreferencesSummaryScreen::saveMusicVolume));
        content.getChildren().add(volumeRow(context, "Sound volume", context.preferencesService().soundVolume(),
                PreferencesSummaryScreen::saveSoundVolume));

        content.getChildren().add(sectionHeading("Theme color"));
        content.getChildren().add(themeSelectionRow(context));
        content.getChildren().add(sectionHeading("Footer display"));
        content.getChildren().add(footerDisplayRow(context));

        Button closeButton = ScreenNavigation.button(context, CLOSE_LABEL, SceneRouter.MAIN_MENU_ROUTE);
        content.getChildren().add(closeButton);
        BorderPane root = ScreenShell.titled(viewModel.title(), content, footerOptions());
        HBox footer = (HBox) root.getBottom();
        ScreenShell.applyFooterPreferences(footer, context.preferencesService());
        wireFooter(footer, closeAction);

        Scene scene = context.themedScene(root);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (isCloseShortcut(event.getCode(), event.isShortcutDown())) {
                closeAction.run();
                event.consume();
            }
        });
        return scene;
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
                        new PreferencesSummaryRowViewModel("Master volume", percentLabel(preferencesService.masterVolume())),
                        new PreferencesSummaryRowViewModel("Music volume", percentLabel(preferencesService.musicVolume())),
                        new PreferencesSummaryRowViewModel("Sound volume", percentLabel(preferencesService.soundVolume())),
                        new PreferencesSummaryRowViewModel("Theme color",
                                themeOptionLabel(preferencesService.themeFamily(), preferencesService.themeVariant())),
                        new PreferencesSummaryRowViewModel("Footer display", preferencesService.footerShortcutDisplay().label())),
                List.of(new ScreenActionViewModel(CLOSE_LABEL, SceneRouter.MAIN_MENU_ROUTE, true)));
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

    static List<ScreenShell.FooterOption> footerOptions() {
        return ScreenShell.defaultFooterOptions().stream()
                .map(option -> {
                    ScreenShell.FooterOption updated = option.withEnabled(ENABLED_FOOTER_IDS.contains(option.id()));
                    if (PREFERENCES_ID.equals(updated.id())) {
                        return updated.withTooltip("Close preferences.");
                    }
                    return updated;
                })
                .toList();
    }

    static boolean isCloseShortcut(KeyCode keyCode, boolean shortcutDown) {
        return shortcutDown && keyCode == KeyCode.P;
    }

    static String percentLabel(double volume) {
        return Math.round(volume * VOLUME_PERCENT_SCALE) + "%";
    }

    static String themeOptionLabel(ThemeFamily family, ThemeVariant variant) {
        return family.label() + " - " + variant.label();
    }

    private static Label sectionHeading(String text) {
        Label heading = new Label(text);
        heading.getStyleClass().add(ScreenShell.LAYOUT_SECTION_TITLE_STYLE_CLASS);
        heading.setPadding(new Insets(8, 0, 0, 0));
        return heading;
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
        slider.valueProperty().addListener((observable, previous, current) -> {
            double updatedVolume = current.doubleValue() / VOLUME_PERCENT_SCALE;
            value.setText(percentLabel(updatedVolume));
            volumeSaver.save(context, updatedVolume);
        });
        return new HBox(8, label, slider, value);
    }

    private static HBox themeSelectionRow(RouteContext context) {
        Label label = new Label("Theme");
        ComboBox<ThemeChoice> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(themeChoices());
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ThemeChoice choice) {
                return choice == null ? "" : choice.label();
            }

            @Override
            public ThemeChoice fromString(String string) {
                return themeChoices().stream()
                        .filter(choice -> choice.label().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
        comboBox.setValue(new ThemeChoice(context.preferencesService().themeFamily(), context.preferencesService().themeVariant()));
        comboBox.setOnAction(event -> {
            ThemeChoice selected = comboBox.getValue();
            if (selected != null) {
                applyTheme(context, selected.family(), selected.variant());
            }
        });
        return new HBox(8, label, comboBox);
    }

    private static HBox footerDisplayRow(RouteContext context) {
        Label label = new Label("Footer shortcuts");
        ComboBox<FooterShortcutDisplay> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(FooterShortcutDisplay.values());
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(FooterShortcutDisplay display) {
                return display == null ? "" : display.label();
            }

            @Override
            public FooterShortcutDisplay fromString(String string) {
                for (FooterShortcutDisplay display : FooterShortcutDisplay.values()) {
                    if (display.label().equals(string)) {
                        return display;
                    }
                }
                return null;
            }
        });
        comboBox.setValue(context.preferencesService().footerShortcutDisplay());
        comboBox.setOnAction(event -> {
            FooterShortcutDisplay selected = comboBox.getValue();
            if (selected != null) {
                context.preferencesService().saveFooterShortcutDisplay(selected);
                applyCurrentFooterPreferences(context);
            }
        });
        return new HBox(8, label, comboBox);
    }

    private static List<ThemeChoice> themeChoices() {
        return List.of(
                new ThemeChoice(ThemeFamily.OCEAN, ThemeVariant.DARK),
                new ThemeChoice(ThemeFamily.OCEAN, ThemeVariant.LIGHT_PASTEL),
                new ThemeChoice(ThemeFamily.FOREST, ThemeVariant.DARK),
                new ThemeChoice(ThemeFamily.FOREST, ThemeVariant.LIGHT_PASTEL),
                new ThemeChoice(ThemeFamily.SUNSET, ThemeVariant.DARK),
                new ThemeChoice(ThemeFamily.SUNSET, ThemeVariant.LIGHT_PASTEL),
                new ThemeChoice(ThemeFamily.VIOLET, ThemeVariant.DARK),
                new ThemeChoice(ThemeFamily.VIOLET, ThemeVariant.LIGHT_PASTEL));
    }

    private static void applyTheme(RouteContext context, ThemeFamily family, ThemeVariant variant) {
        context.preferencesService().saveThemePreferences(family, variant);
        context.uiTheme().initialize(context.preferencesService());
        context.primaryStage().setScene(createScene(context));
    }

    private static void applyCurrentFooterPreferences(RouteContext context) {
        Scene scene = context.primaryStage().getScene();
        if (scene == null || !(scene.getRoot() instanceof BorderPane root)) {
            return;
        }
        ScreenShell.applyFooterPreferences(root.getBottom(), context.preferencesService());
    }

    private static void wireFooter(HBox footer, Runnable closeAction) {
        for (Node child : footer.getChildren()) {
            if (child instanceof Label label) {
                label.setOnMouseClicked(event -> {
                    if (label.isDisabled()
                            || !(label.getUserData() instanceof ScreenShell.FooterOption option)
                            || !PREFERENCES_ID.equals(option.id())) {
                        return;
                    }
                    closeAction.run();
                });
            }
        }
    }

    private static void saveMasterVolume(RouteContext context, double volume) {
        context.preferencesService().saveMasterVolume(volume);
        AudioService audioService = context.audioService();
        if (audioService != null && audioService.isInitialized()) {
            audioService.setMasterVolume(context.preferencesService().masterVolume());
        }
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

    private record ThemeChoice(ThemeFamily family, ThemeVariant variant) {
        private String label() {
            return themeOptionLabel(family, variant);
        }
    }
}
