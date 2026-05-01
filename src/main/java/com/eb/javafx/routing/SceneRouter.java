package com.eb.javafx.routing;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.display.DisplayAnimationPlayer;
import com.eb.javafx.display.ImageAssetDefinition;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.display.LayeredCharacterDefinition;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.ui.CaptureTestScreen;
import com.eb.javafx.ui.ScreenShell;
import com.eb.javafx.ui.UiTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Route table that replaces Ren'Py labels, jumps, calls, and screen entry points.
 *
 * <p>The first implementation only registers a main-menu placeholder, but the
 * interface is intentionally route-ID based so future migrated screens can move
 * navigation out of stringly Ren'Py labels and into named Java controller routes.</p>
 */
public final class SceneRouter {
    public static final String MAIN_MENU_ROUTE = "main-menu";
    public static final String PREFERENCES_ROUTE = "preferences";
    public static final String SAVE_LOAD_ROUTE = "save-load";
    public static final String DIALOGUE_ROUTE = "dialogue";
    public static final String CHOICE_ROUTE = "choice";
    public static final String HUD_ROUTE = "hud";
    public static final String NOTIFICATION_ROUTE = "notification";
    public static final String TOOLTIP_ROUTE = "tooltip";
    public static final String DISPLAY_BINDINGS_ROUTE = "display-bindings";
    public static final String CAPTURE_TEST_ROUTE = "capture-test";

    private final Map<String, Supplier<Scene>> routes = new LinkedHashMap<>();
    private final Map<String, RouteDescriptor> routeDescriptors = new LinkedHashMap<>();

    /** Registers routes that must exist before startup can display the first scene. */
    public void registerDefaultRoutes(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            SaveLoadService saveLoadService,
            UiTheme uiTheme) {
        registerRoute(new RouteDescriptor(
                        MAIN_MENU_ROUTE,
                        "ui.mainMenu.title",
                        RouteCategory.MENU,
                        true,
                        "Shell route with navigation to registered JavaFX placeholders."),
                () -> buildMainMenuScene(primaryStage, preferencesService, contentRegistry, uiTheme));
        registerRoute(new RouteDescriptor(
                        PREFERENCES_ROUTE,
                        "ui.preferences.title",
                        RouteCategory.SETTINGS,
                        false,
                        "Placeholder route showing startup preference values."),
                () -> buildPreferencesScene(primaryStage, preferencesService, contentRegistry, uiTheme));
        registerRoute(new RouteDescriptor(
                        SAVE_LOAD_ROUTE,
                        "ui.saveLoad.title",
                        RouteCategory.SAVE_LOAD,
                        false,
                        "Placeholder route showing explicit save schema metadata."),
                () -> buildSaveLoadScene(primaryStage, preferencesService, contentRegistry, saveLoadService, uiTheme));
        registerRoute(new RouteDescriptor(
                        DIALOGUE_ROUTE,
                        "ui.dialogue.title",
                        RouteCategory.DIALOGUE,
                        false,
                        "Placeholder route for future say-window and dialogue executor integration."),
                () -> buildPlaceholderScene(primaryStage, preferencesService, contentRegistry, uiTheme,
                        "ui.dialogue.title", "Dialogue text, speaker names, portraits, and say-window opacity will bind here."));
        registerRoute(new RouteDescriptor(
                        CHOICE_ROUTE,
                        "ui.choice.title",
                        RouteCategory.DIALOGUE,
                        false,
                        "Placeholder route for future command-backed choice menus."),
                () -> buildPlaceholderScene(primaryStage, preferencesService, contentRegistry, uiTheme,
                        "ui.choice.title", "Ren'Py menu choices will become route-backed command selections here."));
        registerRoute(new RouteDescriptor(
                        HUD_ROUTE,
                        "ui.hud.title",
                        RouteCategory.HUD,
                        false,
                        "Placeholder route showing persistent HUD preference wiring."),
                () -> buildHudScene(primaryStage, preferencesService, contentRegistry, uiTheme));
        registerRoute(new RouteDescriptor(
                        NOTIFICATION_ROUTE,
                        "ui.notification.title",
                        RouteCategory.OVERLAY,
                        false,
                        "Placeholder route for notification and modal overlay layering."),
                () -> buildPlaceholderScene(primaryStage, preferencesService, contentRegistry, uiTheme,
                        "ui.notification.title", "Notifications and modal overlays share this JavaFX layer strategy."));
        registerRoute(new RouteDescriptor(
                        TOOLTIP_ROUTE,
                        "ui.tooltip.title",
                        RouteCategory.TOOLTIP,
                        false,
                        "Placeholder route for reusable tooltip panels."),
                () -> buildPlaceholderScene(primaryStage, preferencesService, contentRegistry, uiTheme,
                        "ui.tooltip.title", "Reusable tooltip panels for domain data, actions, requirements, and generic help will render here."));
        registerRoute(new RouteDescriptor(
                        DISPLAY_BINDINGS_ROUTE,
                        "ui.displayBindings.title",
                        RouteCategory.HUD,
                        true,
                        "Preview route showing parsed display bindings, resolved assets, layered character models, and animation profiles."),
                () -> buildDisplayBindingsScene(primaryStage, preferencesService, contentRegistry, imageDisplayRegistry, uiTheme));
        registerRoute(new RouteDescriptor(
                        CAPTURE_TEST_ROUTE,
                        "ui.captureTest.title",
                        RouteCategory.MENU,
                        true,
                        "Small routed JavaFX screen that captures field values with buttons."),
                () -> CaptureTestScreen.createScene(
                        contentRegistry.definition("ui.captureTest.title"),
                        preferencesService,
                        uiTheme,
                        () -> primaryStage.setScene(open(MAIN_MENU_ROUTE))));
    }

    private void registerRoute(RouteDescriptor descriptor, Supplier<Scene> routeFactory) {
        routes.put(descriptor.id(), routeFactory);
        routeDescriptors.put(descriptor.id(), descriptor);
    }

    /**
     * Opens a route by ID and returns the JavaFX scene owned by that route.
     *
     * @param routeId stable route ID, currently replacing a Ren'Py label/screen name
     * @return scene to attach to the primary stage
     */
    public Scene open(String routeId) {
        Supplier<Scene> route = routes.get(routeId);
        if (route == null) {
            throw new IllegalArgumentException("Unknown JavaFX route: " + routeId);
        }
        return route.get();
    }

    /** Returns registered route IDs for diagnostics and the main menu shell. */
    public Map<String, Supplier<Scene>> routes() {
        return Collections.unmodifiableMap(routes);
    }

    /** Returns route metadata for inventory, validation, and shell diagnostics. */
    public Map<String, RouteDescriptor> routeDescriptors() {
        return Collections.unmodifiableMap(routeDescriptors);
    }

    /** Ensures every registered route has content-backed title text. */
    public void validateRouteDefinitions(ContentRegistry contentRegistry) {
        routeDescriptors.values().forEach(descriptor ->
                contentRegistry.definition(descriptor.titleDefinition()));
    }

    /** Builds the first visible shell proving that the bootstrap sequence completed. */
    private Scene buildMainMenuScene(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            UiTheme uiTheme) {
        VBox menu = new VBox(8);
        menu.setPadding(new Insets(8));

        Label title = new Label(contentRegistry.definition("application.name"));
        Label slice = new Label(contentRegistry.definition("migration.slice"));
        Label status = new Label("Startup phase complete. Section 1.1 services, section 1.2 shell routes, and section 1.4-1.8 support foundations are ready.");
        menu.getChildren().addAll(title, slice, status);
        addRouteNavigationButtons(primaryStage, contentRegistry, menu);

        // Keep a reference to the stage parameter in this early route to make the
        // intended controller handoff explicit; future controllers can use it for
        // dialogs, fullscreen transitions, or shutdown confirmation.
        primaryStage.setMinWidth(640);
        primaryStage.setMinHeight(480);

        return themedScene(ScreenShell.titled(contentRegistry.definition("ui.mainMenu.title"), menu), preferencesService, uiTheme);
    }

    private Scene buildPreferencesScene(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            UiTheme uiTheme) {
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
                navigationButton(primaryStage, "Back to main menu", MAIN_MENU_ROUTE));
        return themedScene(ScreenShell.titled(contentRegistry.definition("ui.preferences.title"), content), preferencesService, uiTheme);
    }

    private Scene buildSaveLoadScene(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            SaveLoadService saveLoadService,
            UiTheme uiTheme) {
        VBox content = new VBox(8);
        content.getChildren().addAll(
                new Label("Save schema version: " + saveLoadService.schema().version()),
                new Label("Save directory: " + saveLoadService.schema().saveDirectory()),
                new Label("Transient UI state is intentionally excluded from save data."),
                navigationButton(primaryStage, "Back to main menu", MAIN_MENU_ROUTE));
        return themedScene(ScreenShell.titled(contentRegistry.definition("ui.saveLoad.title"), content), preferencesService, uiTheme);
    }

    private Scene buildHudScene(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            UiTheme uiTheme) {
        VBox content = new VBox(8);
        content.setOpacity(preferencesService.hudAlpha());
        content.getChildren().addAll(
                new Label("Persistent HUD layer placeholder"),
                new Label("HUD opacity is read from the JavaFX preferences service."),
                navigationButton(primaryStage, "Back to main menu", MAIN_MENU_ROUTE));
        return themedScene(ScreenShell.titled(contentRegistry.definition("ui.hud.title"), content), preferencesService, uiTheme);
    }

    private Scene buildPlaceholderScene(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            UiTheme uiTheme,
            String titleDefinition,
            String bodyText) {
        VBox content = new VBox(8);
        content.getChildren().addAll(
                new Label(bodyText),
                navigationButton(primaryStage, "Back to main menu", MAIN_MENU_ROUTE));
        return themedScene(ScreenShell.titled(contentRegistry.definition(titleDefinition), content), preferencesService, uiTheme);
    }

    private Scene buildDisplayBindingsScene(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            UiTheme uiTheme) {
        VBox content = new VBox(12);
        content.getChildren().addAll(
                new Label("Parsed image aliases: " + imageDisplayRegistry.images().size()),
                new Label("Parsed transforms: " + imageDisplayRegistry.transforms().size()),
                new Label("Layered character models: " + imageDisplayRegistry.layeredCharacters().size()),
                new Label("Animation profiles: " + imageDisplayRegistry.animations().size()));

        FlowPane previews = new FlowPane(12, 12);
        previews.setPrefWrapLength(680);
        previews.setAlignment(Pos.TOP_LEFT);
        int previewCount = 0;
        for (ImageAssetDefinition definition : imageDisplayRegistry.images().values()) {
            if (previewCount++ >= 12) {
                break;
            }
            VBox tile = new VBox(6);
            tile.setPadding(new Insets(8));
            tile.getStyleClass().add("screen-panel");
            Label title = new Label(definition.id() + " [" + definition.layer() + "]");
            Label source = new Label(definition.sourcePath());
            tile.getChildren().addAll(title, imageDisplayRegistry.createDisplayNode(definition.id()), source);

            if ("empty_token_small".equals(definition.id())) {
                DisplayAnimationPlayer animationPlayer = new DisplayAnimationPlayer();
                animationPlayer.buildTimeline(tile, imageDisplayRegistry.animation(ImageDisplayRegistry.DISPLAY_PREVIEW_PULSE_ANIMATION),
                        preferencesService.reducedMotion()).play();
            }
            previews.getChildren().add(tile);
        }

        VBox layeredCharacterSummary = new VBox(8);
        layeredCharacterSummary.getChildren().add(new Label("Layered display composition"));
        for (LayeredCharacterDefinition definition : imageDisplayRegistry.layeredCharacters().values()) {
            layeredCharacterSummary.getChildren().add(new Label(
                    definition.id() + " -> " + definition.drawOrder()
                            + ", metadata=" + definition.metadata()));
        }

        VBox animationSummary = new VBox(8);
        animationSummary.getChildren().add(new Label("Animation profiles"));
        imageDisplayRegistry.animations().values().forEach(animation ->
                animationSummary.getChildren().add(new Label(
                        animation.id() + " -> steps=" + animation.steps().size()
                                + ", repeat=" + animation.repeatCount()
                                + ", autoReverse=" + animation.autoReverse())));

        HBox sections = new HBox(16, layeredCharacterSummary, animationSummary);
        sections.setAlignment(Pos.TOP_LEFT);

        content.getChildren().addAll(previews, sections, navigationButton(primaryStage, "Back to main menu", MAIN_MENU_ROUTE));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        return themedScene(ScreenShell.titled(contentRegistry.definition("ui.displayBindings.title"), scrollPane), preferencesService, uiTheme);
    }

    private Button navigationButton(Stage primaryStage, String text, String routeId) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> primaryStage.setScene(open(routeId)));
        return button;
    }

    private void addRouteNavigationButtons(Stage primaryStage, ContentRegistry contentRegistry, VBox menu) {
        for (RouteDescriptor descriptor : routeDescriptors.values()) {
            if (!MAIN_MENU_ROUTE.equals(descriptor.id())) {
                String title = contentRegistry.definition(descriptor.titleDefinition());
                menu.getChildren().add(navigationButton(primaryStage, title, descriptor.id()));
            }
        }
    }

    private Scene themedScene(BorderPane root, PreferencesService preferencesService, UiTheme uiTheme) {
        Scene scene = new Scene(root, preferencesService.windowWidth(), preferencesService.windowHeight());
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }
}
