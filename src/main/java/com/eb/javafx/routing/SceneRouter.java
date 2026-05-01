package com.eb.javafx.routing;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.gamesupport.GameSupportService;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.scene.EnginePlaceholderSceneModule;
import com.eb.javafx.scene.SceneExecutor;
import com.eb.javafx.scene.SceneRegistry;
import com.eb.javafx.state.GameState;
import com.eb.javafx.ui.UiTheme;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Route table for labels, jumps, calls, and screen entry points.
 *
 * <p>The router is intentionally limited to route metadata, route factories, and
 * navigation. Scene construction belongs to reusable {@link RouteFactory}
 * implementations so game/app modules can contribute routes without editing the
 * engine router.</p>
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

    private final Map<String, RouteFactory> routes = new LinkedHashMap<>();
    private final Map<String, RouteDescriptor> routeDescriptors = new LinkedHashMap<>();
    private RouteContext routeContext;

    /**
     * Registers routes that must exist before startup can display the first scene.
     *
     * <p>Default reusable route factories are registered through
     * {@link DefaultRouteModule}. Re-registering a route ID replaces the previous
     * factory and descriptor.</p>
     */
    public void registerDefaultRoutes(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            SaveLoadService saveLoadService,
            UiTheme uiTheme) {
        GameRandomService randomService = new GameRandomService();
        randomService.initialize();
        GameSupportService gameSupportService = new GameSupportService();
        gameSupportService.initialize();
        SceneRegistry sceneRegistry = new SceneRegistry();
        new EnginePlaceholderSceneModule().registerScenes(sceneRegistry);
        sceneRegistry.validateScenes();
        registerDefaultRoutes(primaryStage, preferencesService, contentRegistry, imageDisplayRegistry, saveLoadService,
                randomService, gameSupportService, new GameState(contentRegistry.definition("startup.route")),
                sceneRegistry, new SceneExecutor(sceneRegistry), uiTheme);
    }

    public void registerDefaultRoutes(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            SaveLoadService saveLoadService,
            GameRandomService randomService,
            GameSupportService gameSupportService,
            GameState gameState,
            SceneRegistry sceneRegistry,
            SceneExecutor sceneExecutor,
            UiTheme uiTheme) {
        registerRoutes(new RouteContext(
                primaryStage,
                preferencesService,
                contentRegistry,
                imageDisplayRegistry,
                saveLoadService,
                randomService,
                gameSupportService,
                gameState,
                sceneRegistry,
                sceneExecutor,
                uiTheme,
                this), List.of(new DefaultRouteModule()));
    }

    /** Sets the shared route context and registers every supplied route module. */
    public void registerRoutes(RouteContext routeContext, List<RouteModule> routeModules) {
        this.routeContext = routeContext;
        routeModules.forEach(this::registerRouteModule);
    }

    /** Registers all routes from a module against this router. */
    public void registerRouteModule(RouteModule routeModule) {
        routeModule.registerRoutes(this);
    }

    /** Registers or replaces a route descriptor and factory. */
    public void registerRoute(RouteDescriptor descriptor, RouteFactory routeFactory) {
        routes.put(descriptor.id(), routeFactory);
        routeDescriptors.put(descriptor.id(), descriptor);
    }

    /**
     * Opens a route by ID and returns the JavaFX scene owned by that route.
     *
     * @param routeId stable route ID representing a label or screen name
     * @return scene to attach to the primary stage
     */
    public Scene open(String routeId) {
        RouteFactory route = routes.get(routeId);
        if (route == null) {
            throw new IllegalArgumentException("Unknown JavaFX route: " + routeId);
        }
        if (routeContext == null) {
            throw new IllegalStateException("JavaFX route context has not been registered.");
        }
        return route.createScene(routeContext);
    }

    /** Returns registered route IDs for diagnostics and the main menu shell. */
    public Map<String, RouteFactory> routes() {
        return Collections.unmodifiableMap(routes);
    }

    /** Returns route metadata for inventory, validation, and shell diagnostics. */
    public Map<String, RouteDescriptor> routeDescriptors() {
        return Collections.unmodifiableMap(routeDescriptors);
    }

    /**
     * Ensures every registered route has content-backed title text.
     *
     * @throws IllegalStateException when any descriptor references a missing title definition
     */
    public void validateRouteDefinitions(ContentRegistry contentRegistry) {
        routeDescriptors.values().forEach(descriptor ->
                contentRegistry.definition(descriptor.titleDefinition()));
    }
}
