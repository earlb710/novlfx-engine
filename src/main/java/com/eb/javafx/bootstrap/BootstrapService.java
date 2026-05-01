package com.eb.javafx.bootstrap;

import com.eb.javafx.audio.AudioService;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.EnginePlaceholderContentModule;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.gamesupport.GameSupportService;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.globalApi.GlobalApiAdapter;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.state.GameState;
import com.eb.javafx.state.GameStateFactory;
import com.eb.javafx.ui.UiTheme;
import javafx.stage.Stage;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Coordinates deterministic startup for the JavaFX port.
 *
 * <p>This class implements the first migration slice from the plan: the boot
 * sequence is no longer a side effect of engine loading scripts. Every stage of
 * startup is named, invoked once, and checked before the first scene is displayed.</p>
 */
public final class BootstrapService {
    private final PreferencesService preferencesService;
    private final ContentRegistry contentRegistry;
    private final ImageDisplayRegistry imageDisplayRegistry;
    private final GameStateFactory gameStateFactory;
    private final SaveLoadService saveLoadService;
    private final GameRandomService randomService;
    private final AudioService audioService;
    private final GameSupportService gameSupportService;
    private final SceneRouter sceneRouter;
    private final UiTheme uiTheme;
    private final List<StaticContentModule> staticContentModules;

    /**
     * Creates a bootstrap service using default audio/game-support services and no static modules.
     *
     * <p>This constructor is useful for the minimal shell and tests that only need
     * the core registries supplied directly.</p>
     */
    public BootstrapService(
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            GameStateFactory gameStateFactory,
            SaveLoadService saveLoadService,
            GameRandomService randomService,
            SceneRouter sceneRouter,
            UiTheme uiTheme) {
        this(
                preferencesService,
                contentRegistry,
                imageDisplayRegistry,
                gameStateFactory,
                saveLoadService,
                randomService,
                new AudioService(),
                new GameSupportService(),
                sceneRouter,
                uiTheme,
                Collections.emptyList());
    }

    /**
     * Creates a fully injectable bootstrap service for app/game content modules and tests.
     *
     * <p>The supplied services are owned for the duration of startup; static modules
     * register after base registries are ready and validate before route creation.</p>
     */
    public BootstrapService(
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            GameStateFactory gameStateFactory,
            SaveLoadService saveLoadService,
            GameRandomService randomService,
            AudioService audioService,
            GameSupportService gameSupportService,
            SceneRouter sceneRouter,
            UiTheme uiTheme,
            List<StaticContentModule> staticContentModules) {
        this.preferencesService = preferencesService;
        this.contentRegistry = contentRegistry;
        this.imageDisplayRegistry = imageDisplayRegistry;
        this.gameStateFactory = gameStateFactory;
        this.saveLoadService = saveLoadService;
        this.randomService = randomService;
        this.audioService = audioService;
        this.gameSupportService = gameSupportService;
        this.sceneRouter = sceneRouter;
        this.uiTheme = uiTheme;
        this.staticContentModules = List.copyOf(staticContentModules);
    }

    /**
     * Runs all startup phases and returns the initialized application context.
     *
     * @param primaryStage JavaFX stage passed in so route factories can size scenes
     * @return initialized services plus the first mutable game state object
     * @throws RuntimeException when any phase fails; the caller should report it via startup UI
     */
    public BootContext boot(Stage primaryStage) {
        Instant startedAt = Instant.now();
        EnumSet<BootstrapPhase> completedPhases = EnumSet.noneOf(BootstrapPhase.class);
        Map<BootstrapPhase, String> phaseMessages = new EnumMap<>(BootstrapPhase.class);

        // Core services: load user preferences before constructing scenes that use window sizing.
        preferencesService.load();
        saveLoadService.initialize();
        randomService.initialize();
        audioService.initialize(preferencesService);
        gameSupportService.initialize();
        uiTheme.initialize(preferencesService);
        completePhase(completedPhases, phaseMessages, BootstrapPhase.CORE_SERVICES,
                "Preferences, save/load, random, audio, game support, and theme services initialized.");

        // Static content registries: replace define/import side effects with named registry calls.
        contentRegistry.registerBaseContent();
        imageDisplayRegistry.registerBaseDisplayContent();
        new EnginePlaceholderContentModule().register(contentRegistry, imageDisplayRegistry);
        staticContentModules.forEach(module -> module.register(contentRegistry, imageDisplayRegistry));
        completePhase(completedPhases, phaseMessages, BootstrapPhase.STATIC_CONTENT_REGISTRIES,
                "Base static content and display definitions registered.");

        // Game rules: validate required definitions before any screen can depend on them.
        contentRegistry.validateRules();
        imageDisplayRegistry.validateDisplayContent();
        new EnginePlaceholderContentModule().validate(contentRegistry, imageDisplayRegistry);
        staticContentModules.forEach(module -> module.validate(contentRegistry, imageDisplayRegistry));
        completePhase(completedPhases, phaseMessages, BootstrapPhase.GAME_RULES,
                "Required content and display definitions validated.");

        // UI routes/controllers: replace label and jump targets with explicit route IDs.
        sceneRouter.registerDefaultRoutes(primaryStage, preferencesService, contentRegistry, imageDisplayRegistry, saveLoadService, uiTheme);
        sceneRouter.validateRouteDefinitions(contentRegistry);
        GlobalApiAdapter globalApiAdapter = new GlobalApiAdapter(randomService, sceneRouter, audioService);
        completePhase(completedPhases, phaseMessages, BootstrapPhase.UI_ROUTES_AND_CONTROLLERS,
                "Default JavaFX shell routes and Global API adapter registered and validated.");

        // Runtime state: create mutable new-game state only after services and registries exist.
        GameState newGameState = gameStateFactory.createNewGame(contentRegistry);
        completePhase(completedPhases, phaseMessages, BootstrapPhase.RUNTIME_STATE,
                "Initial mutable game state created.");

        assertComplete(completedPhases);
        BootstrapReport bootstrapReport = new BootstrapReport(startedAt, Instant.now(), completedPhases, phaseMessages);
        return new BootContext(
                preferencesService,
                contentRegistry,
                imageDisplayRegistry,
                saveLoadService,
                randomService,
                audioService,
                gameSupportService,
                globalApiAdapter,
                sceneRouter,
                uiTheme,
                newGameState,
                bootstrapReport);
    }

    private void completePhase(
            EnumSet<BootstrapPhase> completedPhases,
            Map<BootstrapPhase, String> phaseMessages,
            BootstrapPhase phase,
            String message) {
        completedPhases.add(phase);
        phaseMessages.put(phase, message);
    }

    /** Ensures future edits cannot accidentally skip a documented startup phase. */
    private void assertComplete(EnumSet<BootstrapPhase> completedPhases) {
        if (!completedPhases.containsAll(EnumSet.allOf(BootstrapPhase.class))) {
            throw new IllegalStateException("JavaFX bootstrap did not complete all required phases.");
        }
    }
}
