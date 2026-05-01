package com.lr2alt.javafx.bootstrap;

import com.lr2alt.javafx.audio.AudioService;
import com.lr2alt.javafx.content.ContentRegistry;
import com.lr2alt.javafx.display.ImageDisplayRegistry;
import com.lr2alt.javafx.gamesupport.GameSupportService;
import com.lr2alt.javafx.prefs.PreferencesService;
import com.lr2alt.javafx.random.GameRandomService;
import com.lr2alt.javafx.renpy.RenpyApiAdapter;
import com.lr2alt.javafx.routing.SceneRouter;
import com.lr2alt.javafx.save.SaveLoadService;
import com.lr2alt.javafx.state.GameState;
import com.lr2alt.javafx.ui.UiTheme;

/**
 * Immutable handoff object returned after startup completes.
 *
 * <p>Ren'Py modules can read global variables after init blocks run. The JavaFX
 * port should avoid that hidden global coupling, so the bootstrap layer returns a
 * small context containing the services and state that downstream controllers need
 * for the first screen.</p>
 */
public final class BootContext {
    private final PreferencesService preferencesService;
    private final ContentRegistry contentRegistry;
    private final ImageDisplayRegistry imageDisplayRegistry;
    private final SaveLoadService saveLoadService;
    private final GameRandomService randomService;
    private final AudioService audioService;
    private final GameSupportService gameSupportService;
    private final RenpyApiAdapter renpyApiAdapter;
    private final SceneRouter sceneRouter;
    private final UiTheme uiTheme;
    private final GameState gameState;
    private final BootstrapReport bootstrapReport;

    public BootContext(
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            SaveLoadService saveLoadService,
            GameRandomService randomService,
            AudioService audioService,
            GameSupportService gameSupportService,
            RenpyApiAdapter renpyApiAdapter,
            SceneRouter sceneRouter,
            UiTheme uiTheme,
            GameState gameState,
            BootstrapReport bootstrapReport) {
        this.preferencesService = preferencesService;
        this.contentRegistry = contentRegistry;
        this.imageDisplayRegistry = imageDisplayRegistry;
        this.saveLoadService = saveLoadService;
        this.randomService = randomService;
        this.audioService = audioService;
        this.gameSupportService = gameSupportService;
        this.renpyApiAdapter = renpyApiAdapter;
        this.sceneRouter = sceneRouter;
        this.uiTheme = uiTheme;
        this.gameState = gameState;
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

    /** Returns no-content game support systems such as actions and clock/scheduling primitives. */
    public GameSupportService gameSupportService() {
        return gameSupportService;
    }

    /** Returns the adapter for migrated code replacing direct Ren'Py API calls. */
    public RenpyApiAdapter renpyApiAdapter() {
        return renpyApiAdapter;
    }

    /** Returns the router that replaces Ren'Py label/jump screen navigation. */
    public SceneRouter sceneRouter() {
        return sceneRouter;
    }

    /** Returns JavaFX theme tokens replacing Ren'Py GUI/style values. */
    public UiTheme uiTheme() {
        return uiTheme;
    }

    /** Returns the mutable runtime state for a new game placeholder. */
    public GameState gameState() {
        return gameState;
    }

    /** Returns phase-by-phase startup diagnostics for progress UI and tests. */
    public BootstrapReport bootstrapReport() {
        return bootstrapReport;
    }
}
