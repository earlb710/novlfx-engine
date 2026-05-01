package com.eb.javafx.bootstrap;

import com.eb.javafx.audio.AudioService;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.gamesupport.GameSupportService;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.globalApi.GlobalApiAdapter;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.scene.SceneExecutor;
import com.eb.javafx.scene.SceneRegistry;
import com.eb.javafx.state.GameState;
import com.eb.javafx.ui.UiTheme;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Immutable handoff object returned after startup completes.
 *
 * <p>The JavaFX port should avoid hidden global coupling, so the bootstrap layer returns a
 * small context containing the services and state that downstream controllers need
 * for the first screen. Services in this context are initialized singletons for
 * the current application run, while {@link GameState} is the mutable per-save
 * object created for the new-game placeholder.</p>
 */
public final class BootContext {
    private final PreferencesService preferencesService;
    private final ContentRegistry contentRegistry;
    private final ImageDisplayRegistry imageDisplayRegistry;
    private final SaveLoadService saveLoadService;
    private final GameRandomService randomService;
    private final AudioService audioService;
    private final GameSupportService gameSupportService;
    private final SceneRegistry sceneRegistry;
    private final SceneExecutor sceneExecutor;
    private final GlobalApiAdapter globalApiAdapter;
    private final SceneRouter sceneRouter;
    private final UiTheme uiTheme;
    private final GameState gameState;
    private final Path applicationRoot;
    private final ApplicationResourceConfig resourceConfig;
    private final BootstrapReport bootstrapReport;

    /**
     * Creates a completed startup handoff containing initialized services and state.
     *
     * @param preferencesService loaded preference service
     * @param contentRegistry validated static text/content definitions
     * @param imageDisplayRegistry validated image, transform, layer, and animation definitions
     * @param saveLoadService initialized versioned save/load boundary
     * @param randomService initialized gameplay/UI random streams
     * @param audioService initialized channel-based audio boundary
     * @param gameSupportService initialized generic support systems
     * @param sceneRegistry validated structured scene definitions
     * @param sceneExecutor headless scene-flow executor
     * @param globalApiAdapter adapter for migrated global API calls
     * @param sceneRouter registered route table
     * @param uiTheme initialized theme tokens and stylesheet lookup
     * @param gameState mutable runtime state for the current session
     * @param bootstrapReport immutable phase diagnostics
     */
    public BootContext(
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            SaveLoadService saveLoadService,
            GameRandomService randomService,
            AudioService audioService,
            GameSupportService gameSupportService,
            SceneRegistry sceneRegistry,
            SceneExecutor sceneExecutor,
            GlobalApiAdapter globalApiAdapter,
            SceneRouter sceneRouter,
            UiTheme uiTheme,
            GameState gameState,
            BootstrapReport bootstrapReport) {
        this(preferencesService, contentRegistry, imageDisplayRegistry, saveLoadService, randomService, audioService,
                gameSupportService, sceneRegistry, sceneExecutor, globalApiAdapter, sceneRouter, uiTheme, gameState,
                Paths.get("").toAbsolutePath().normalize(), ApplicationResourceConfig.defaults(), bootstrapReport);
    }

    /**
     * Creates a completed startup handoff with initialized services, state, and application resource paths.
     */
    public BootContext(
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            SaveLoadService saveLoadService,
            GameRandomService randomService,
            AudioService audioService,
            GameSupportService gameSupportService,
            SceneRegistry sceneRegistry,
            SceneExecutor sceneExecutor,
            GlobalApiAdapter globalApiAdapter,
            SceneRouter sceneRouter,
            UiTheme uiTheme,
            GameState gameState,
            Path applicationRoot,
            ApplicationResourceConfig resourceConfig,
            BootstrapReport bootstrapReport) {
        this.preferencesService = preferencesService;
        this.contentRegistry = contentRegistry;
        this.imageDisplayRegistry = imageDisplayRegistry;
        this.saveLoadService = saveLoadService;
        this.randomService = randomService;
        this.audioService = audioService;
        this.gameSupportService = gameSupportService;
        this.sceneRegistry = sceneRegistry;
        this.sceneExecutor = sceneExecutor;
        this.globalApiAdapter = globalApiAdapter;
        this.sceneRouter = sceneRouter;
        this.uiTheme = uiTheme;
        this.gameState = gameState;
        this.applicationRoot = applicationRoot.toAbsolutePath().normalize();
        this.resourceConfig = resourceConfig;
        this.bootstrapReport = bootstrapReport;
    }

    /** Returns the preferences model loaded before UI construction. */
    public PreferencesService preferencesService() {
        return preferencesService;
    }

    /** Returns immutable static content registered during startup. */
    public ContentRegistry contentRegistry() {
        return contentRegistry;
    }

    /** Returns static image aliases, transforms, and layer bindings registered during startup. */
    public ImageDisplayRegistry imageDisplayRegistry() {
        return imageDisplayRegistry;
    }

    /** Returns the save/load boundary that will own serialized game state. */
    public SaveLoadService saveLoadService() {
        return saveLoadService;
    }

    /** Returns deterministic gameplay and separate UI randomness. */
    public GameRandomService randomService() {
        return randomService;
    }

    /** Returns the channel-based audio/media boundary. */
    public AudioService audioService() {
        return audioService;
    }

    /** Returns generic game-support systems such as actions and clock/scheduling primitives. */
    public GameSupportService gameSupportService() {
        return gameSupportService;
    }

    /** Returns validated structured dialogue/scene definitions. */
    public SceneRegistry sceneRegistry() {
        return sceneRegistry;
    }

    /** Returns the headless executor for structured scene flows. */
    public SceneExecutor sceneExecutor() {
        return sceneExecutor;
    }

    /** Returns the adapter for migrated code replacing direct global API calls. */
    public GlobalApiAdapter globalApiAdapter() {
        return globalApiAdapter;
    }

    /** Returns the router that replaces global label/jump screen navigation. */
    public SceneRouter sceneRouter() {
        return sceneRouter;
    }

    /** Returns JavaFX theme tokens for GUI/style values. */
    public UiTheme uiTheme() {
        return uiTheme;
    }

    /** Returns the mutable runtime state for a new game placeholder. */
    public GameState gameState() {
        return gameState;
    }

    /** Returns the application directory used to resolve configured resource paths. */
    public Path applicationRoot() {
        return applicationRoot;
    }

    /** Returns resource locations supplied to bootstrap, or engine defaults when none were supplied. */
    public ApplicationResourceConfig resourceConfig() {
        return resourceConfig;
    }

    /** Returns phase-by-phase startup diagnostics for progress UI and tests. */
    public BootstrapReport bootstrapReport() {
        return bootstrapReport;
    }
}
