package com.eb.javafx.ui;

import com.eb.javafx.audio.AudioService;
import com.eb.javafx.debug.DebugScreenInspector;
import com.eb.javafx.prefs.PreferencesService.FooterIconDisplay;
import com.eb.javafx.prefs.PreferencesService.FooterShortcutDisplay;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.prefs.PreferencesService.Language;
import com.eb.javafx.prefs.PreferencesService.TextSpeed;
import com.eb.javafx.prefs.PreferencesService.ThemeFamily;
import com.eb.javafx.prefs.PreferencesService.ThemeVariant;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.SceneRouter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Reusable preferences route that summarizes startup preferences and exposes theme selection.
 */
public final class PreferencesSummaryScreen {
    private static final String PREFERENCES_ID = "preferences";
    private static final Set<String> ENABLED_FOOTER_IDS = Set.of(PREFERENCES_ID);
    private static final double VOLUME_PERCENT_SCALE = 100.0;

    private static MainMenuConfirmation mainMenuConfirmationOverride;

    private PreferencesSummaryScreen() {
    }

    /**
     * Confirmation hook for the Main Menu button. Tests override this to bypass the modal dialog.
     */
    @FunctionalInterface
    public interface MainMenuConfirmation {
        boolean confirm();
    }

    public static void setMainMenuConfirmation(MainMenuConfirmation override) {
        mainMenuConfirmationOverride = override;
    }

    public static void clearMainMenuConfirmation() {
        mainMenuConfirmationOverride = null;
    }

    public static Scene createScene(RouteContext context) {
        return createScene(context, context.preferencesService().windowWidth(), context.preferencesService().windowHeight());
    }

    static Scene createScene(RouteContext context, double width, double height) {
        PreferencesSummaryViewModel viewModel = viewModel(context);
        // Close returns to the screen that opened preferences when that opener used
        // RouteContext.pushAndNavigateTo (the back-stack will have a saved scene root); otherwise
        // — e.g. when reached from the main menu, or via a direct navigateTo from older callers —
        // fall back to the main menu so the user is never stranded.
        Runnable closeAction = () -> {
            if (!context.navigateBack()) {
                context.navigateTo(SceneRouter.MAIN_MENU_ROUTE);
            }
        };
        GridPane content = twoColumnBlocks(
                settingsBlock(
                        screenText("block.audio.title"),
                        volumeRow(context, screenText("item.master-volume.label"), context.preferencesService().masterVolume(),
                                PreferencesSummaryScreen::saveMasterVolume),
                        volumeRow(context, screenText("item.music-volume.label"), context.preferencesService().musicVolume(),
                                PreferencesSummaryScreen::saveMusicVolume),
                        volumeRow(context, screenText("item.sound-volume.label"), context.preferencesService().soundVolume(),
                                PreferencesSummaryScreen::saveSoundVolume),
                        muteAllRow(context)),
                settingsBlock(
                        screenText("block.visual.title"),
                        themeSelectionRow(context),
                        hudOpacityRow(context),
                        footerIconDisplayRow(context),
                        footerDisplayRow(context),
                        fullscreenRow(context)),
                settingsBlock(
                        screenText("block.text.title"),
                        textSizeRow(context),
                        textSpeedRow(context)),
                settingsBlock(
                        screenText("block.save.title"),
                        autoSaveDailyRow(context)),
                settingsBlock(
                        screenText("block.language.title"),
                        languageRow(context)));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Button mainMenuButton = ButtonVisuals.applySvgArtwork(new Button(screenText("item.main-menu.label")));
        mainMenuButton.setMinWidth(220);
        // Async confirmation via the in-scene DialogMessages helper — works in fullscreen
        // mode where stock Alert dialogs hide behind the primary stage.  Test escape
        // hatch (mainMenuConfirmationOverride) still answers synchronously for tests that
        // can't run the JFX event loop; see requestReturnToMainMenu below.
        mainMenuButton.setOnAction(event -> requestReturnToMainMenu(
                mainMenuButton.getScene(), context.uiTheme(),
                () -> context.navigateTo(SceneRouter.MAIN_MENU_ROUTE)));
        // Wired directly to closeAction (instead of ScreenNavigation.button(..., MAIN_MENU_ROUTE))
        // so the button honours the back-stack-aware navigation set up above — same behaviour as
        // the footer close icon and the Ctrl+P shortcut.
        Button closeButton = ButtonVisuals.applySvgArtwork(new Button(screenText("item.close.label")));
        closeButton.setOnAction(event -> closeAction.run());
        // Match the Main Menu button's size exactly — its longer label otherwise makes it grow
        // wider than the 220 floor, leaving the Close button visibly smaller. Binding min + pref +
        // max to the Main Menu button's actual width keeps the two footer buttons identical.
        closeButton.minWidthProperty().bind(mainMenuButton.widthProperty());
        closeButton.prefWidthProperty().bind(mainMenuButton.widthProperty());
        closeButton.maxWidthProperty().bind(mainMenuButton.widthProperty());
        HBox closeBox = new HBox(12, mainMenuButton, closeButton);
        closeBox.setAlignment(Pos.CENTER);
        closeBox.setPadding(new Insets(12, 0, 12, 0));

        BorderPane contentArea = new BorderPane();
        contentArea.setCenter(scrollPane);
        contentArea.setBottom(closeBox);
        BorderPane.setAlignment(closeBox, Pos.BOTTOM_CENTER);
        // The shell wraps this content in a top-aligned VBox, which would otherwise size the
        // content area to its preferred height — leaving the button bar floating just below the
        // (now shorter, two-column) settings grid. Grow the content area to fill the available
        // height so the Main Menu / Close buttons stay pinned to the bottom-centre of the screen.
        contentArea.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        BorderPane root = ScreenShell.titled(viewModel.title(), contentArea, footerOptions());
        HBox footer = (HBox) root.getBottom();
        ScreenShell.applyFooterPreferences(footer, context.preferencesService(), context.uiTheme());
        wireFooter(footer, closeAction);

        Scene scene = themedPreferencesScene(context, root, width, height);
        scene.getStylesheets().add(context.uiTheme().stylesheet());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (isCloseShortcut(event.getCode(), event.isShortcutDown())) {
                closeAction.run();
                event.consume();
            }
        });
        DebugScreenInspector.setScreenClass(scene, PreferencesSummaryScreen.class);
        return scene;
    }

    public static PreferencesSummaryViewModel viewModel(RouteContext context) {
        return viewModel(
                ScreenTextResources.title(ScreenTextResources.PREFERENCES),
                context.preferencesService());
    }

    public static PreferencesSummaryViewModel viewModel(String title, PreferencesService preferencesService) {
        return new PreferencesSummaryViewModel(
                title,
                List.of(
                        new PreferencesSummaryRowViewModel(screenText("item.master-volume.label"), percentLabel(preferencesService.masterVolume())),
                        new PreferencesSummaryRowViewModel(screenText("item.music-volume.label"), percentLabel(preferencesService.musicVolume())),
                        new PreferencesSummaryRowViewModel(screenText("item.sound-volume.label"), percentLabel(preferencesService.soundVolume())),
                        new PreferencesSummaryRowViewModel(screenText("item.theme-color.label"),
                                themeOptionLabel(preferencesService.themeFamily(), preferencesService.themeVariant())),
                        new PreferencesSummaryRowViewModel(screenText("item.footer-icon-display.label"), preferencesService.footerIconDisplay().label()),
                        new PreferencesSummaryRowViewModel(screenText("item.footer-display.label"), preferencesService.footerShortcutDisplay().label())),
                List.of(new ScreenActionViewModel(screenText("item.close.label"), SceneRouter.MAIN_MENU_ROUTE, true)));
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
                "Violet - Light pastel",
                "Crimson - Dark",
                "Crimson - Light pastel");
    }

    static List<ScreenShell.FooterOption> footerOptions() {
        return ScreenShell.defaultFooterOptions().stream()
                .map(option -> {
                    ScreenShell.FooterOption updated = option.withEnabled(ENABLED_FOOTER_IDS.contains(option.id()));
                    if (PREFERENCES_ID.equals(updated.id())) {
                        return updated.withTooltip(screenText("footer.preferences.close.tooltip"));
                    }
                    return updated;
                })
                .toList();
    }

    static boolean isCloseShortcut(KeyCode keyCode, boolean shortcutDown) {
        return shortcutDown && keyCode == KeyCode.P;
    }

    /**
     * Synchronous confirmation hook kept for tests that inject a
     * {@link MainMenuConfirmation} override — they can't pump the JFX event loop, so the
     * async {@link DialogMessages#confirm} path doesn't fit.  Production code goes
     * through {@link #requestReturnToMainMenu} instead, which surfaces the same prompt
     * via the in-scene overlay so it works in fullscreen mode.
     */
    static boolean confirmReturnToMainMenu() {
        if (mainMenuConfirmationOverride != null) {
            return mainMenuConfirmationOverride.confirm();
        }
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle(screenText("dialog.main-menu-confirm.title"));
        alert.setHeaderText(screenText("dialog.main-menu-confirm.header"));
        alert.setContentText(screenText("dialog.main-menu-confirm.content"));
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Production confirmation flow — shows the "Return to main menu?" prompt via the
     * in-scene {@link DialogMessages#confirm} overlay so it surfaces correctly under
     * fullscreen mode (stock {@link Alert} dialogs frequently hide behind the fullscreen
     * window).  Invokes {@code onConfirm} only when the player picks OK; cancels are no-ops.
     *
     * <p>Honours the {@link #setMainMenuConfirmation test override}: if set, the override's
     * synchronous boolean answer is used directly so unit tests that can't pump the JFX
     * event loop still work.  When no override is registered, the async DialogMessages
     * path is used.</p>
     */
    static void requestReturnToMainMenu(Scene activeScene, UiTheme theme, Runnable onConfirm) {
        if (mainMenuConfirmationOverride != null) {
            if (mainMenuConfirmationOverride.confirm()) {
                onConfirm.run();
            }
            return;
        }
        DialogMessages.confirm(activeScene, theme,
                screenText("dialog.main-menu-confirm.title"),
                screenText("dialog.main-menu-confirm.header"),
                screenText("dialog.main-menu-confirm.content"),
                result -> {
                    if (result == DialogMessages.Result.OK) {
                        onConfirm.run();
                    }
                });
    }

    static VBox settingsBlock(String title, Node... rows) {
        Node[] blockChildren = new Node[rows.length + 1];
        blockChildren[0] = sectionHeading(title);
        for (int index = 0; index < rows.length; index++) {
            blockChildren[index + 1] = rows[index];
        }
        return ScreenShell.styledPanel(ScreenShell.LAYOUT_CARD_STYLE_CLASS, blockChildren);
    }

    /** Horizontal gap and resting column count for the preferences block grid. */
    private static final double BLOCK_GRID_GAP = 10;
    private static final int BLOCK_COLUMNS = 2;

    /**
     * Lays the settings blocks out in a two-column grid (filled left-to-right, top-to-bottom).
     * Both columns share the available width equally and the blocks stretch to fill their cell so
     * the cards line up. The host {@link ScrollPane} keeps the grid scrollable when the rows exceed
     * the viewport height.
     */
    static GridPane twoColumnBlocks(VBox... blocks) {
        GridPane grid = new GridPane();
        grid.setHgap(BLOCK_GRID_GAP);
        grid.setVgap(BLOCK_GRID_GAP);
        for (int column = 0; column < BLOCK_COLUMNS; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / BLOCK_COLUMNS);
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setFillWidth(true);
            grid.getColumnConstraints().add(constraints);
        }
        for (int index = 0; index < blocks.length; index++) {
            VBox block = blocks[index];
            block.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(block, Priority.ALWAYS);
            GridPane.setFillWidth(block, true);
            grid.add(block, index % BLOCK_COLUMNS, index / BLOCK_COLUMNS);
        }
        return grid;
    }

    /** Fixed width of the right-aligned label column so every "label + field" row lines its
     *  fields up at the same x-position. */
    private static final double LABEL_COLUMN_WIDTH = 150;

    /**
     * Builds a "label + field" preference row as two columns: the label sits in a fixed-width,
     * right-aligned column and the field (plus any trailing nodes) start left-aligned immediately
     * after it, so fields line up vertically across rows.
     */
    private static HBox labeledRow(String labelText, Node field, Node... trailing) {
        Label label = new Label(labelText);
        label.getStyleClass().add(ScreenShell.SCREEN_TEXT_STYLE_CLASS);
        label.setMinWidth(LABEL_COLUMN_WIDTH);
        label.setPrefWidth(LABEL_COLUMN_WIDTH);
        label.setMaxWidth(Region.USE_PREF_SIZE);
        label.setAlignment(Pos.CENTER_RIGHT);
        label.setTextAlignment(TextAlignment.RIGHT);
        HBox row = new HBox(8, label, field);
        row.getChildren().addAll(trailing);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
        return row;
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
        Slider slider = new Slider(0, VOLUME_PERCENT_SCALE, currentVolume * VOLUME_PERCENT_SCALE);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        slider.setBlockIncrement(10);
        slider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(slider, Priority.ALWAYS);
        Label value = new Label(percentLabel(currentVolume));
        value.getStyleClass().add(ScreenShell.SCREEN_VALUE_STYLE_CLASS);
        slider.valueProperty().addListener((observable, previous, current) -> {
            double updatedVolume = current.doubleValue() / VOLUME_PERCENT_SCALE;
            value.setText(percentLabel(updatedVolume));
            volumeSaver.save(context, updatedVolume);
        });
        return labeledRow(labelText, slider, value);
    }

    /** Global text-size choices mapped to font-scale factors.  Accessibility control: bump every
     *  font up for low-vision players, or down for those who find the default too big. */
    private enum TextSize {
        SMALLER(0.85, "item.text-size.smaller"),
        NORMAL(1.0, "item.text-size.normal"),
        BIGGER(1.2, "item.text-size.bigger");

        private final double scale;
        private final String labelKey;

        TextSize(double scale, String labelKey) {
            this.scale = scale;
            this.labelKey = labelKey;
        }

        String label() {
            return screenText(labelKey);
        }

        /** The choice whose scale is closest to {@code fontScale}. */
        static TextSize nearest(double fontScale) {
            TextSize best = NORMAL;
            double bestDelta = Double.MAX_VALUE;
            for (TextSize size : values()) {
                double delta = Math.abs(size.scale - fontScale);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    best = size;
                }
            }
            return best;
        }
    }

    private static HBox textSizeRow(RouteContext context) {
        ComboBox<TextSize> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(TextSize.values());
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(TextSize size) {
                return size == null ? "" : size.label();
            }

            @Override
            public TextSize fromString(String string) {
                for (TextSize size : TextSize.values()) {
                    if (size.label().equals(string)) {
                        return size;
                    }
                }
                return null;
            }
        });
        comboBox.setValue(TextSize.nearest(context.preferencesService().fontScale()));
        comboBox.setOnAction(event -> {
            TextSize selected = comboBox.getValue();
            if (selected != null) {
                applyFontScale(context, selected.scale);
            }
        });
        return labeledRow(screenText("item.text-size.label"), comboBox);
    }

    /** Persists the global font scale, re-initialises the theme (which rewrites all font sizes),
     *  and rebuilds the preferences scene so the change is visible immediately — mirrors
     *  {@link #applyTheme}. */
    private static void applyFontScale(RouteContext context, double scale) {
        context.preferencesService().saveFontScale(scale);
        context.uiTheme().initialize(context.preferencesService());
        Scene currentScene = context.primaryStage() == null ? null : context.primaryStage().getScene();
        double width = currentScene == null ? context.preferencesService().windowWidth() : currentScene.getWidth();
        double height = currentScene == null ? context.preferencesService().windowHeight() : currentScene.getHeight();
        Scene rebuiltScene = createScene(context, width, height);
        if (currentScene != null && rebuiltScene != null && rebuiltScene.getRoot() != null) {
            javafx.scene.Parent rebuiltRoot = rebuiltScene.getRoot();
            rebuiltScene.setRoot(new javafx.scene.layout.Pane());
            currentScene.setRoot(rebuiltRoot);
            currentScene.getStylesheets().setAll(rebuiltScene.getStylesheets());
        } else if (context.primaryStage() != null) {
            context.primaryStage().setScene(rebuiltScene);
        }
    }

    /** HUD opacity presets (gameplay HUD overlay translucency).  Accessibility / preference:
     *  fade the HUD for a cleaner view, or keep it solid. */
    private enum HudOpacity {
        SOLID(1.0, "item.hud-opacity.solid"),
        SUBTLE(0.85, "item.hud-opacity.subtle"),
        FAINT(0.6, "item.hud-opacity.faint");

        private final double alpha;
        private final String labelKey;

        HudOpacity(double alpha, String labelKey) {
            this.alpha = alpha;
            this.labelKey = labelKey;
        }

        String label() {
            return screenText(labelKey);
        }

        static HudOpacity nearest(double hudAlpha) {
            HudOpacity best = SOLID;
            double bestDelta = Double.MAX_VALUE;
            for (HudOpacity value : values()) {
                double delta = Math.abs(value.alpha - hudAlpha);
                if (delta < bestDelta) {
                    bestDelta = delta;
                    best = value;
                }
            }
            return best;
        }
    }

    private static HBox hudOpacityRow(RouteContext context) {
        ComboBox<HudOpacity> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(HudOpacity.values());
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(HudOpacity value) {
                return value == null ? "" : value.label();
            }

            @Override
            public HudOpacity fromString(String string) {
                for (HudOpacity value : HudOpacity.values()) {
                    if (value.label().equals(string)) {
                        return value;
                    }
                }
                return null;
            }
        });
        comboBox.setValue(HudOpacity.nearest(context.preferencesService().hudAlpha()));
        comboBox.setOnAction(event -> {
            HudOpacity selected = comboBox.getValue();
            if (selected != null) {
                // Persist only — the gameplay HUD (not visible on this screen) picks it up the
                // next time a gameplay scene is built.  Preserve the say-window opacity.
                context.preferencesService().saveUiOpacity(
                        selected.alpha, context.preferencesService().sayWindowAlpha());
            }
        });
        return labeledRow(screenText("item.hud-opacity.label"), comboBox);
    }

    private static HBox themeSelectionRow(RouteContext context) {
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
        return labeledRow(screenText("item.theme.label"), comboBox);
    }

    private static HBox footerDisplayRow(RouteContext context) {
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
        return labeledRow(screenText("item.footer-shortcuts.label"), comboBox);
    }

    private static HBox footerIconDisplayRow(RouteContext context) {
        ComboBox<FooterIconDisplay> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(FooterIconDisplay.values());
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(FooterIconDisplay display) {
                return display == null ? "" : display.label();
            }

            @Override
            public FooterIconDisplay fromString(String string) {
                for (FooterIconDisplay display : FooterIconDisplay.values()) {
                    if (display.label().equals(string)) {
                        return display;
                    }
                }
                return null;
            }
        });
        comboBox.setValue(context.preferencesService().footerIconDisplay());
        comboBox.setOnAction(event -> {
            FooterIconDisplay selected = comboBox.getValue();
            if (selected != null) {
                context.preferencesService().saveFooterIconDisplay(selected);
                applyCurrentFooterPreferences(context);
            }
        });
        return labeledRow(screenText("item.footer-icon-display.label"), comboBox);
    }

    private static HBox muteAllRow(RouteContext context) {
        CheckBox checkBox = new CheckBox(screenText("item.mute-all.label"));
        checkBox.getStyleClass().add(ScreenShell.SCREEN_TEXT_STYLE_CLASS);
        checkBox.setSelected(context.preferencesService().muteAll());
        checkBox.selectedProperty().addListener((observable, previous, current) -> {
            context.preferencesService().saveMuteAll(current);
            AudioService audioService = context.audioService();
            if (audioService != null && audioService.isInitialized()) {
                audioService.setMuted(current);
            }
        });
        HBox row = new HBox(8, checkBox);
        row.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
        return row;
    }

    private static HBox autoSaveDailyRow(RouteContext context) {
        CheckBox checkBox = new CheckBox(screenText("item.auto-save-daily.label"));
        checkBox.getStyleClass().add(ScreenShell.SCREEN_TEXT_STYLE_CLASS);
        checkBox.setSelected(context.preferencesService().autoSaveDaily());
        checkBox.selectedProperty().addListener((observable, previous, current) ->
                context.preferencesService().saveAutoSaveDaily(current));
        HBox row = new HBox(8, checkBox);
        row.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
        return row;
    }

    private static HBox fullscreenRow(RouteContext context) {
        CheckBox checkBox = new CheckBox(screenText("item.fullscreen.label"));
        checkBox.getStyleClass().add(ScreenShell.SCREEN_TEXT_STYLE_CLASS);
        checkBox.setSelected(context.preferencesService().fullscreen());
        checkBox.selectedProperty().addListener((observable, previous, current) -> {
            context.preferencesService().saveFullscreen(current);
            if (context.primaryStage() != null) {
                context.primaryStage().setFullScreen(current);
            }
        });
        HBox row = new HBox(8, checkBox);
        row.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
        return row;
    }

    private static HBox textSpeedRow(RouteContext context) {
        ComboBox<TextSpeed> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(TextSpeed.values());
        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(TextSpeed value) {
                return value == null ? "" : value.label();
            }

            @Override
            public TextSpeed fromString(String string) {
                for (TextSpeed candidate : TextSpeed.values()) {
                    if (candidate.label().equals(string)) {
                        return candidate;
                    }
                }
                return null;
            }
        });
        comboBox.setValue(context.preferencesService().textSpeed());
        comboBox.setOnAction(event -> {
            TextSpeed selected = comboBox.getValue();
            if (selected != null) {
                context.preferencesService().saveTextSpeed(selected);
            }
        });
        return labeledRow(screenText("item.text-speed.label"), comboBox);
    }

    private static HBox languageRow(RouteContext context) {
        ToggleGroup group = new ToggleGroup();
        HBox row = new HBox(8);
        row.getStyleClass().add(ScreenShell.LAYOUT_SECTION_ROW_STYLE_CLASS);
        Language current = context.preferencesService().language();
        for (Language language : Language.values()) {
            RadioButton button = new RadioButton(language.label());
            button.getStyleClass().add(ScreenShell.SCREEN_TEXT_STYLE_CLASS);
            button.setToggleGroup(group);
            button.setUserData(language);
            button.setDisable(!language.enabled());
            if (language == current && language.enabled()) {
                button.setSelected(true);
            }
            row.getChildren().add(button);
        }
        if (group.getSelectedToggle() == null) {
            for (Toggle toggle : group.getToggles()) {
                if (toggle instanceof RadioButton button && !button.isDisable()) {
                    button.setSelected(true);
                    break;
                }
            }
        }
        group.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected instanceof RadioButton button && button.getUserData() instanceof Language language) {
                context.preferencesService().saveLanguage(language);
            }
        });
        return row;
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
                new ThemeChoice(ThemeFamily.VIOLET, ThemeVariant.LIGHT_PASTEL),
                new ThemeChoice(ThemeFamily.CRIMSON, ThemeVariant.DARK),
                new ThemeChoice(ThemeFamily.CRIMSON, ThemeVariant.LIGHT_PASTEL));
    }

    private static void applyTheme(RouteContext context, ThemeFamily family, ThemeVariant variant) {
        context.preferencesService().saveThemePreferences(family, variant);
        context.uiTheme().initialize(context.preferencesService());
        Scene currentScene = context.primaryStage().getScene();
        double width = currentScene == null ? context.preferencesService().windowWidth() : currentScene.getWidth();
        double height = currentScene == null ? context.preferencesService().windowHeight() : currentScene.getHeight();
        Scene rebuiltScene = createScene(context, width, height);
        Scene activeScene;
        if (currentScene != null && rebuiltScene != null && rebuiltScene.getRoot() != null) {
            javafx.scene.Parent rebuiltRoot = rebuiltScene.getRoot();
            rebuiltScene.setRoot(new javafx.scene.layout.Pane());
            currentScene.setRoot(rebuiltRoot);
            currentScene.getStylesheets().setAll(rebuiltScene.getStylesheets());
            activeScene = currentScene;
        } else {
            context.primaryStage().setScene(rebuiltScene);
            activeScene = rebuiltScene;
        }
        DebugScreenInspector.attach(
                activeScene,
                SceneRouter.PREFERENCES_ROUTE,
                context.resourceConfig().debug(),
                context.uiTheme());
    }

    private static void applyCurrentFooterPreferences(RouteContext context) {
        Scene scene = context.primaryStage().getScene();
        BorderPane root = scene == null ? null : ScreenShell.shellRoot(scene.getRoot());
        if (root == null) {
            return;
        }
        ScreenShell.applyFooterPreferences(root.getBottom(), context.preferencesService(), context.uiTheme());
    }

    private static void wireFooter(HBox footer, Runnable closeAction) {
        for (Node child : footer.getChildren()) {
            if (child instanceof Label label
                    && label.getUserData() instanceof ScreenShell.FooterOption option
                    && PREFERENCES_ID.equals(option.id())) {
                label.setOnMouseClicked(event -> {
                    if (!ScreenShell.isFooterOptionEnabled(label)) {
                        return;
                    }
                    closeAction.run();
                });
            }
        }
    }

    private static Scene themedPreferencesScene(RouteContext context, BorderPane root, double width, double height) {
        Parent sceneRoot = ScreenShell.withConfiguredBackground(
                root,
                context.applicationRoot(),
                context.resourceConfig().defaultPreferencesScreenBackgroundColor(),
                context.resourceConfig().defaultPreferencesScreenBackgroundImage(),
                context.resourceConfig().defaultPreferencesScreenBackgroundImageTransparency());
        return new Scene(sceneRoot, width, height);
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

    private static String screenText(String key) {
        return ScreenTextResources.text(ScreenTextResources.PREFERENCES, key);
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
