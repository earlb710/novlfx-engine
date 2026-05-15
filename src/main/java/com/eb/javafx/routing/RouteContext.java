package com.eb.javafx.routing;

import com.eb.javafx.bootstrap.ApplicationResourceConfig;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.audio.AudioService;
import com.eb.javafx.debug.DebugScreenInspector;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.gamesupport.GameSupportService;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.scene.SceneExecutor;
import com.eb.javafx.scene.SceneRegistry;
import com.eb.javafx.state.GameState;
import com.eb.javafx.ui.ScreenShell;
import com.eb.javafx.ui.UiTheme;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;

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
    private final AudioService audioService;
    private final GameState gameState;
    private final SceneRegistry sceneRegistry;
    private final SceneExecutor sceneExecutor;
    private final UiTheme uiTheme;
    private final SceneRouter sceneRouter;
    private final Path applicationRoot;
    private final ApplicationResourceConfig resourceConfig;

    public RouteContext(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            SaveLoadService saveLoadService,
            UiTheme uiTheme,
            SceneRouter sceneRouter) {
        this(primaryStage, preferencesService, contentRegistry, imageDisplayRegistry, saveLoadService,
                null, null, null, null, new SceneRegistry(), null, uiTheme, sceneRouter,
                Paths.get("").toAbsolutePath().normalize(), ApplicationResourceConfig.defaults());
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
        this(primaryStage, preferencesService, contentRegistry, imageDisplayRegistry, saveLoadService, randomService,
                gameSupportService, null, gameState, sceneRegistry, sceneExecutor, uiTheme, sceneRouter,
                Paths.get("").toAbsolutePath().normalize(), ApplicationResourceConfig.defaults());
    }

    public RouteContext(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            SaveLoadService saveLoadService,
            GameRandomService randomService,
            GameSupportService gameSupportService,
            AudioService audioService,
            GameState gameState,
            SceneRegistry sceneRegistry,
            SceneExecutor sceneExecutor,
            UiTheme uiTheme,
            SceneRouter sceneRouter) {
        this(primaryStage, preferencesService, contentRegistry, imageDisplayRegistry, saveLoadService, randomService,
                gameSupportService, audioService, gameState, sceneRegistry, sceneExecutor, uiTheme, sceneRouter,
                Paths.get("").toAbsolutePath().normalize(), ApplicationResourceConfig.defaults());
    }

    public RouteContext(
            Stage primaryStage,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            SaveLoadService saveLoadService,
            GameRandomService randomService,
            GameSupportService gameSupportService,
            AudioService audioService,
            GameState gameState,
            SceneRegistry sceneRegistry,
            SceneExecutor sceneExecutor,
            UiTheme uiTheme,
            SceneRouter sceneRouter,
            Path applicationRoot,
            ApplicationResourceConfig resourceConfig) {
        this.primaryStage = primaryStage;
        this.preferencesService = preferencesService;
        this.contentRegistry = contentRegistry;
        this.imageDisplayRegistry = imageDisplayRegistry;
        this.saveLoadService = saveLoadService;
        this.randomService = randomService;
        this.gameSupportService = gameSupportService;
        this.audioService = audioService;
        this.gameState = gameState;
        this.sceneRegistry = sceneRegistry;
        this.sceneExecutor = sceneExecutor == null && sceneRegistry != null ? new SceneExecutor(sceneRegistry) : sceneExecutor;
        this.uiTheme = uiTheme;
        this.sceneRouter = sceneRouter;
        this.applicationRoot = applicationRoot == null
                ? Paths.get("").toAbsolutePath().normalize()
                : applicationRoot.toAbsolutePath().normalize();
        this.resourceConfig = resourceConfig == null ? ApplicationResourceConfig.defaults() : resourceConfig;
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

    public AudioService audioService() {
        return audioService;
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

    public Path applicationRoot() {
        return applicationRoot;
    }

    public ApplicationResourceConfig resourceConfig() {
        return resourceConfig;
    }

    /** Opens a route and attaches it to the primary stage. */
    public void navigateTo(String routeId) {
        Scene newScene = sceneRouter.open(routeId);
        Scene currentScene = primaryStage == null ? null : primaryStage.getScene();
        Scene activeScene;
        if (currentScene != null && newScene != null && newScene.getRoot() != null) {
            Parent newRoot = newScene.getRoot();
            newScene.setRoot(new javafx.scene.layout.Pane());
            currentScene.setRoot(newRoot);
            currentScene.getStylesheets().setAll(newScene.getStylesheets());
            activeScene = currentScene;
        } else {
            primaryStage.setScene(newScene);
            activeScene = newScene;
        }
        DebugScreenInspector.attach(activeScene, routeId, resourceConfig.debug(), uiTheme);
    }

    /** Creates a consistently sized and themed scene for a screen shell root. */
    public Scene themedScene(BorderPane root) {
        return themedScene(
                root,
                resourceConfig.defaultAppBackgroundColor(),
                resourceConfig.defaultAppBackgroundImage(),
                resourceConfig.defaultAppBackgroundImageTransparency());
    }

    /** Creates a themed scene using the configured preferences screen background defaults. */
    public Scene themedPreferencesScene(BorderPane root) {
        return themedScene(
                root,
                resourceConfig.defaultPreferencesScreenBackgroundColor(),
                resourceConfig.defaultPreferencesScreenBackgroundImage(),
                resourceConfig.defaultPreferencesScreenBackgroundImageTransparency());
    }

    /** Creates a themed scene using the configured save/load screen background defaults. */
    public Scene themedSaveLoadScene(BorderPane root) {
        return themedScene(
                root,
                resourceConfig.defaultSaveLoadScreenBackgroundColor(),
                resourceConfig.defaultSaveLoadScreenBackgroundImage(),
                resourceConfig.defaultSaveLoadScreenBackgroundImageTransparency());
    }

    private Scene themedScene(
            BorderPane root,
            String backgroundColor,
            String backgroundImage,
            String backgroundImageTransparency) {
        Parent sceneRoot = ScreenShell.withConfiguredBackground(
                root,
                applicationRoot,
                backgroundColor,
                backgroundImage,
                backgroundImageTransparency);
        Scene scene = new Scene(sceneRoot, preferencesService.windowWidth(), preferencesService.windowHeight());
        scene.getStylesheets().add(uiTheme.stylesheet());
        return scene;
    }
}
