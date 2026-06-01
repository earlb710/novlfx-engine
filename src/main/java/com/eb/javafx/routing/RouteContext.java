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
    /**
     * Back-stack of saved scene roots for navigations explicitly marked as "returnable" via
     * {@link #pushAndNavigateTo(String)}. Used by transient overlays like the preferences screen
     * to restore the previous view (with its state intact) on close, instead of navigating to a
     * fresh main-menu scene.
     */
    private final java.util.Deque<SavedRoute> backStack = new java.util.ArrayDeque<>();
    /** Tracks the active route id so {@link #pushAndNavigateTo(String)} can record it. */
    private String activeRouteId;
    /** Route id currently being constructed by a factory (set around {@code sceneRouter.open});
     *  lets {@link #themedScene(BorderPane)} resolve the per-screen background for the screen
     *  being built, since {@link #activeRouteId} still holds the outgoing route at that point. */
    private String navigatingRouteId;

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

    /**
     * Returns a {@link RouteContext} that shares every service / registry / router with this
     * context but drives navigations against {@code newPrimaryStage} rather than {@link #primaryStage()}.
     *
     * <p>Used when a host wants to open a screen in a non-primary window (e.g. the admin-menu
     * "Test Status/HUD" inspector).  Build the new context with {@code withPrimaryStage(myStage)},
     * pass it to the screen factory, and any button / handler wired with that context will keep
     * its navigations confined to {@code myStage}'s scene — because {@link #navigateTo(String)}
     * routes through {@link SceneRouter#open(String, RouteContext)} which propagates the calling
     * context down to the next scene's factory.  Each instance has its own back-stack, so
     * {@link #navigateBack()} also stays within the scoped stage's history.</p>
     */
    public RouteContext withPrimaryStage(Stage newPrimaryStage) {
        return new RouteContext(
                newPrimaryStage,
                preferencesService,
                contentRegistry,
                imageDisplayRegistry,
                saveLoadService,
                randomService,
                gameSupportService,
                audioService,
                gameState,
                sceneRegistry,
                sceneExecutor,
                uiTheme,
                sceneRouter,
                applicationRoot,
                resourceConfig);
    }

    /** Opens a route and attaches it to the primary stage. */
    public void navigateTo(String routeId) {
        // Pass {@code this} so the route factory wires its scene with the CURRENT context.
        // Buttons / handlers inside the new scene therefore capture this context's primaryStage,
        // and a stage-scoped context (see {@link #withPrimaryStage}) keeps subsequent navigations
        // confined to its scoped stage.
        //
        // Track the route being constructed so themedScene() can resolve a per-screen background
        // (config screenBackgrounds) for the screen the factory is building — activeRouteId still
        // points at the OUTGOING route until the swap below.
        String previousNavigating = navigatingRouteId;
        navigatingRouteId = routeId;
        Scene newScene;
        try {
            newScene = sceneRouter.open(routeId, this);
        } finally {
            navigatingRouteId = previousNavigating;
        }
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
        this.activeRouteId = routeId;
        DebugScreenInspector.attach(activeScene, routeId, resourceConfig.debug(), uiTheme);
    }

    /**
     * Opens {@code routeId} after saving the current scene root onto the back-stack so a later
     * {@link #navigateBack()} call can restore the caller's view — with its in-flight state
     * (scrollbars, scene-flow stepHistory, dialog cursor, ...) intact.
     *
     * <p>Use for transient overlays the user expects to dismiss back to where they were: the
     * Preferences screen, save/load picker, settings popovers, in-game help, etc. Plain
     * {@link #navigateTo(String)} is still right for "go to a fresh screen" navigations such as
     * starting a new game or returning to the main menu.</p>
     *
     * <p>If there is no current scene (first navigation of the application), the back-stack entry
     * is skipped and this degrades to {@link #navigateTo(String)}.</p>
     */
    public void pushAndNavigateTo(String routeId) {
        Scene currentScene = primaryStage == null ? null : primaryStage.getScene();
        if (currentScene != null && currentScene.getRoot() != null) {
            backStack.push(new SavedRoute(
                    activeRouteId,
                    currentScene.getRoot(),
                    java.util.List.copyOf(currentScene.getStylesheets())));
        }
        navigateTo(routeId);
    }

    /**
     * Pops the back-stack and restores the previously-saved scene root. Returns {@code true} when
     * a saved entry was restored; {@code false} when the stack was empty so the caller can fall
     * back (typically to the main menu).
     *
     * <p>Restoration swaps the saved {@link Parent} back into the primary stage's existing
     * {@link Scene} (same scene instance that {@link #navigateTo(String)} reuses for its root
     * swap), so node identity and listener subscriptions on both the saved tree and any other
     * scene-level handlers carry over.</p>
     */
    public boolean navigateBack() {
        if (backStack.isEmpty()) {
            return false;
        }
        Scene currentScene = primaryStage == null ? null : primaryStage.getScene();
        if (currentScene == null) {
            return false;
        }
        SavedRoute saved = backStack.pop();
        currentScene.setRoot(saved.root());
        currentScene.getStylesheets().setAll(saved.stylesheets());
        this.activeRouteId = saved.routeId();
        if (saved.routeId() != null) {
            DebugScreenInspector.attach(currentScene, saved.routeId(), resourceConfig.debug(), uiTheme);
        }
        return true;
    }

    /**
     * Returns {@code true} if {@link #navigateBack()} would succeed. Useful for hosts that want
     * to choose between "restore previous" and a fallback before calling either.
     */
    public boolean canNavigateBack() {
        return !backStack.isEmpty();
    }

    /** Returns the most recently opened route id, or {@code null} if no route has been opened yet. */
    public String activeRouteId() {
        return activeRouteId;
    }

    /** One saved scene root plus the stylesheet list at the moment of {@link #pushAndNavigateTo(String)}. */
    private record SavedRoute(String routeId, Parent root, java.util.List<String> stylesheets) {
    }

    /** Creates a consistently sized and themed scene for a screen shell root.  Honors a
     *  per-screen background override ({@code screenBackgrounds.<routeId>} in config) for the
     *  screen being built, falling back to the app default for any unset field. */
    public Scene themedScene(BorderPane root) {
        return themedSceneForScreen(
                root,
                currentScreenKey(),
                resourceConfig.defaultAppBackgroundColor(),
                resourceConfig.defaultAppBackgroundImage(),
                resourceConfig.defaultAppBackgroundImageTransparency());
    }

    /** Creates a themed scene for the preferences screen.  Precedence per field:
     *  {@code screenBackgrounds.preferences} → dedicated {@code defaultPreferencesScreen*} →. */
    public Scene themedPreferencesScene(BorderPane root) {
        return themedSceneForScreen(
                root,
                SceneRouter.PREFERENCES_ROUTE,
                resourceConfig.defaultPreferencesScreenBackgroundColor(),
                resourceConfig.defaultPreferencesScreenBackgroundImage(),
                resourceConfig.defaultPreferencesScreenBackgroundImageTransparency());
    }

    /** Creates a themed scene for the save/load screen.  Precedence per field:
     *  {@code screenBackgrounds.save-load} → dedicated {@code defaultSaveLoadScreen*} →. */
    public Scene themedSaveLoadScene(BorderPane root) {
        return themedSceneForScreen(
                root,
                SceneRouter.SAVE_LOAD_ROUTE,
                resourceConfig.defaultSaveLoadScreenBackgroundColor(),
                resourceConfig.defaultSaveLoadScreenBackgroundImage(),
                resourceConfig.defaultSaveLoadScreenBackgroundImageTransparency());
    }

    /** Resolves a scene background as: per-screen config override ({@code screenBackgrounds.key})
     *  per field, else the supplied fallback (a dedicated default or the app default). */
    private Scene themedSceneForScreen(BorderPane root, String screenKey,
                                       String fallbackColor, String fallbackImage,
                                       String fallbackTransparency) {
        return themedScene(
                root,
                resourceConfig.screenBackgroundColor(screenKey).orElse(fallbackColor),
                resourceConfig.screenBackgroundImage(screenKey).orElse(fallbackImage),
                resourceConfig.screenBackgroundImageTransparency(screenKey).orElse(fallbackTransparency));
    }

    /** The screen id used for per-screen background resolution: the route being constructed when
     *  inside a factory, otherwise the active route. */
    private String currentScreenKey() {
        return navigatingRouteId != null ? navigatingRouteId : activeRouteId;
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
