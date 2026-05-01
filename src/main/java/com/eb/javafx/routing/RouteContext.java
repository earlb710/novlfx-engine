package com.eb.javafx.routing;

import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.gamesupport.GameSupportService;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.scene.SceneExecutor;
import com.eb.javafx.scene.SceneRegistry;
import com.eb.javafx.state.GameState;
import com.eb.javafx.ui.UiTheme;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Shared services available to route factories when a scene is opened.
 *
 * <p>The context is created after bootstrap services and registries are ready,
 * then passed to route factories lazily so registration can remain side-effect
 * free.</p>
 */
public final class RouteContext {
    private final Stage primaryStage;
    private final PreferencesService preferencesService;
    private final ContentRegistry contentRegistry;
    private final ImageDisplayRegistry imageDisplayRegistry;
    private final SaveLoadService saveLoadService;
    private final GameRandomService randomService;
    private final GameSupportService gameSupportService;
    private final GameState gameState;
    private final SceneRegistry sceneRegistry;
    private final SceneExecutor sceneExecutor;
    private final UiTheme uiTheme;
    private final SceneRouter sceneRouter;

    public RouteContext(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            SaveLoadService saveLoadService,
            UiTheme uiTheme,
            SceneRouter sceneRouter) {
        this(primaryStage, preferencesService, contentRegistry, imageDisplayRegistry, saveLoadService,
                null, null, null, new SceneRegistry(), null, uiTheme, sceneRouter);
    }

    public RouteContext(
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
            UiTheme uiTheme,
            SceneRouter sceneRouter) {
        this.primaryStage = primaryStage;
        this.preferencesService = preferencesService;
        this.contentRegistry = contentRegistry;
        this.imageDisplayRegistry = imageDisplayRegistry;
        this.saveLoadService = saveLoadService;
        this.randomService = randomService;
        this.gameSupportService = gameSupportService;
        this.gameState = gameState;
        this.sceneRegistry = sceneRegistry;
        this.sceneExecutor = sceneExecutor == null && sceneRegistry != null ? new SceneExecutor(sceneRegistry) : sceneExecutor;
        this.uiTheme = uiTheme;
        this.sceneRouter = sceneRouter;
    }

    public Stage primaryStage() {
        return primaryStage;
    }

    public PreferencesService preferencesService() {
        return preferencesService;
    }

    public ContentRegistry contentRegistry() {
        return contentRegistry;
    }

    public ImageDisplayRegistry imageDisplayRegistry() {
        return imageDisplayRegistry;
    }

    public SaveLoadService saveLoadService() {
        return saveLoadService;
    }

    public GameRandomService randomService() {
        return randomService;
    }

    public GameSupportService gameSupportService() {
        return gameSupportService;
    }

    public GameState gameState() {
        return gameState;
    }

    public SceneRegistry sceneRegistry() {
        return sceneRegistry;
    }

    public SceneExecutor sceneExecutor() {
        return sceneExecutor;
    }

    public UiTheme uiTheme() {
        return uiTheme;
    }

    public SceneRouter sceneRouter() {
        return sceneRouter;
    }

    /** Opens a route and attaches it to the primary stage. */
    public void navigateTo(String routeId) {
        primaryStage.setScene(sceneRouter.open(routeId));
    }

    /** Creates a consistently sized and themed scene for a screen shell root. */
    public Scene themedScene(BorderPane root) {
        Scene scene = new Scene(root, preferencesService.windowWidth(), preferencesService.windowHeight());
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }
}
