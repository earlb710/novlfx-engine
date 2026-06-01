package com.eb.javafx.bootstrap;

import com.eb.javafx.audio.AudioService;
import com.eb.javafx.content.ContentRegistry;
import com.eb.javafx.content.EnginePlaceholderContentModule;
import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.display.ImageDisplayRegistry;
import com.eb.javafx.gamesupport.GameSupportService;
import com.eb.javafx.prefs.PreferencesService;
import com.eb.javafx.random.GameRandomService;
import com.eb.javafx.resources.ResourceRegistry;
import com.eb.javafx.globalApi.GlobalApiAdapter;
import com.eb.javafx.routing.DefaultRouteModule;
import com.eb.javafx.routing.RouteContext;
import com.eb.javafx.routing.RouteModule;
import com.eb.javafx.routing.SceneRouter;
import com.eb.javafx.save.SaveLoadService;
import com.eb.javafx.scene.EnginePlaceholderSceneModule;
import com.eb.javafx.scene.SceneExecutor;
import com.eb.javafx.scene.SceneModule;
import com.eb.javafx.scene.SceneRegistry;
import com.eb.javafx.state.GameState;
import com.eb.javafx.state.GameStateFactory;
import com.eb.javafx.ui.UiTheme;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    private final Path applicationRoot;
    private final ApplicationResourceConfig resourceConfig;
    private final ResourceRegistry resourceRegistry;
    private final PreferencesService preferencesService;
    private final ContentRegistry contentRegistry;
    private final ImageDisplayRegistry imageDisplayRegistry;
    private final GameStateFactory gameStateFactory;
    private final SaveLoadService saveLoadService;
    private final GameRandomService randomService;
    private final AudioService audioService;
    private final GameSupportService gameSupportService;
    private final SceneRegistry sceneRegistry;
    private final SceneRouter sceneRouter;
    private final UiTheme uiTheme;
    private final List<StaticContentModule> staticContentModules;
    private final List<SceneModule> sceneModules;
    private final List<RouteModule> routeModules;

    /** Creates a bootstrap service with default services rooted by the supplied application options. */
    public BootstrapService(BootstrapOptions options) {
        this(
                options.applicationRoot(),
                options.resourceConfig(),
                options.resourceRegistry(),
                new PreferencesService(),
                new ContentRegistry(),
                new ImageDisplayRegistry(options.resourceRegistry()),
                new GameStateFactory(),
                new SaveLoadService(),
                new GameRandomService(),
                new AudioService(),
                new GameSupportService(),
                new SceneRegistry(),
                new SceneRouter(),
                new UiTheme(),
                options.staticContentModules(),
                options.sceneModules(),
                options.routeModules());
    }

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
                defaultApplicationRoot(),
                ApplicationResourceConfig.defaults(),
                preferencesService,
                contentRegistry,
                imageDisplayRegistry,
                gameStateFactory,
                saveLoadService,
                randomService,
                new AudioService(),
                new GameSupportService(),
                new SceneRegistry(),
                sceneRouter,
                uiTheme,
                Collections.emptyList(),
                Collections.emptyList(),
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
        this(defaultApplicationRoot(), ApplicationResourceConfig.defaults(), preferencesService, contentRegistry, imageDisplayRegistry, gameStateFactory, saveLoadService,
                randomService, audioService, gameSupportService, new SceneRegistry(), sceneRouter, uiTheme,
                staticContentModules, Collections.emptyList(), Collections.emptyList());
    }

    public BootstrapService(
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            GameStateFactory gameStateFactory,
            SaveLoadService saveLoadService,
            GameRandomService randomService,
            AudioService audioService,
            GameSupportService gameSupportService,
            SceneRegistry sceneRegistry,
            SceneRouter sceneRouter,
            UiTheme uiTheme,
            List<StaticContentModule> staticContentModules,
            List<SceneModule> sceneModules) {
        this(defaultApplicationRoot(), ApplicationResourceConfig.defaults(), preferencesService, contentRegistry, imageDisplayRegistry,
                gameStateFactory, saveLoadService, randomService, audioService, gameSupportService, sceneRegistry, sceneRouter,
                uiTheme, staticContentModules, sceneModules, Collections.emptyList());
    }

    public BootstrapService(
            Path applicationRoot,
            ApplicationResourceConfig resourceConfig,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            GameStateFactory gameStateFactory,
            SaveLoadService saveLoadService,
            GameRandomService randomService,
            AudioService audioService,
            GameSupportService gameSupportService,
            SceneRegistry sceneRegistry,
            SceneRouter sceneRouter,
            UiTheme uiTheme,
            List<StaticContentModule> staticContentModules,
            List<SceneModule> sceneModules,
            List<RouteModule> routeModules) {
        this(applicationRoot, resourceConfig, ResourceRegistry.builder().build(), preferencesService, contentRegistry,
                imageDisplayRegistry, gameStateFactory, saveLoadService, randomService, audioService, gameSupportService,
                sceneRegistry, sceneRouter, uiTheme, staticContentModules, sceneModules, routeModules);
    }

    public BootstrapService(
            Path applicationRoot,
            ApplicationResourceConfig resourceConfig,
            ResourceRegistry resourceRegistry,
            PreferencesService preferencesService,
            ContentRegistry contentRegistry,
            ImageDisplayRegistry imageDisplayRegistry,
            GameStateFactory gameStateFactory,
            SaveLoadService saveLoadService,
            GameRandomService randomService,
            AudioService audioService,
            GameSupportService gameSupportService,
            SceneRegistry sceneRegistry,
            SceneRouter sceneRouter,
            UiTheme uiTheme,
            List<StaticContentModule> staticContentModules,
            List<SceneModule> sceneModules,
            List<RouteModule> routeModules) {
        this.applicationRoot = applicationRoot.toAbsolutePath().normalize();
        this.resourceConfig = resourceConfig;
        this.resourceRegistry = resourceRegistry == null
                ? ResourceRegistry.builder().build()
                : resourceRegistry;
        this.preferencesService = preferencesService;
        this.contentRegistry = contentRegistry;
        this.imageDisplayRegistry = imageDisplayRegistry;
        this.gameStateFactory = gameStateFactory;
        this.saveLoadService = saveLoadService;
        this.randomService = randomService;
        this.audioService = audioService;
        this.gameSupportService = gameSupportService;
        this.sceneRegistry = sceneRegistry;
        this.sceneRouter = sceneRouter;
        this.uiTheme = uiTheme;
        this.staticContentModules = List.copyOf(staticContentModules);
        this.sceneModules = List.copyOf(sceneModules);
        this.routeModules = List.copyOf(routeModules);
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
        // Config-driven asset overrides: a directory whose files mirror bundled resource paths,
        // letting a game / mod replace icons & images without a rebuild.  Resource loaders that
        // consult ResourceOverrides prefer a file here over the classpath copy.  Installed BEFORE
        // theme init so the button-artwork SVGs (resolved at ButtonVisuals static init, which the
        // theme triggers) can be overridden too.
        com.eb.javafx.util.ResourceOverrides.setOverrideRoot(
                resourceConfig == null ? null
                        : resourceConfig.resolveResource(applicationRoot,
                                com.eb.javafx.util.ResourceOverrides.OVERRIDE_ROOT_RESOURCE_ID)
                                .orElse(null));
        // Per-resource icon repoints: `resources` entries keyed `icon:<originalPath>` map a single
        // icon/image to a replacement path without mirroring its full path under the override root.
        com.eb.javafx.util.ResourceOverrides.setAliases(collectIconAliases());
        // Config-driven custom theme palette (resources.themePalette): retint the whole UI — or
        // define a new look — by overriding palette colour fields, no code/CSS edit.  Loaded
        // before the theme initialises so the generated stylesheet picks it up.
        UiTheme.loadCustomPalette(resolveConfigFile(THEME_PALETTE_RESOURCE_ID));
        uiTheme.initialize(preferencesService);
        // Window chrome from config: title (resources.windowTitle) + app/taskbar icon
        // (resources.appIcon).  Generic — every host's primary stage gets it via boot.
        applyWindowChrome(primaryStage);
        // Config-driven footer styling (the top-level `footer` object): font / colour / select
        // colour / background / transparency, layered over the theme on every themed scene.
        if (resourceConfig != null) {
            com.eb.javafx.ui.FooterStyle.configure(
                    resourceConfig.footerStyle("font").orElse(null),
                    resourceConfig.footerStyle("color").orElse(null),
                    resourceConfig.footerStyle("selectColor").orElse(null),
                    resourceConfig.footerStyle("backgroundColor").orElse(null),
                    resourceConfig.footerStyle("transparency").orElse(null));
        }
        // Config-driven fonts: register any `font.*` entries from the app config so a game / mod
        // can add fonts purely through setup (config.json), before any scene CSS resolves a
        // -fx-font-family.  Best-effort — bad entries log and are skipped.
        ConfiguredFonts.register(resourceConfig, applicationRoot);
        completePhase(completedPhases, phaseMessages, BootstrapPhase.CORE_SERVICES,
                "Preferences, save/load, random, audio, game support, and theme services initialized.");

        // Static content registries: replace define/import side effects with named registry calls.
        contentRegistry.registerBaseContent();
        imageDisplayRegistry.registerBaseDisplayContent();
        EnginePlaceholderContentModule enginePlaceholderContentModule = new EnginePlaceholderContentModule();
        enginePlaceholderContentModule.register(contentRegistry, imageDisplayRegistry);
        staticContentModules.forEach(module -> module.register(contentRegistry, imageDisplayRegistry));
        EnginePlaceholderSceneModule enginePlaceholderSceneModule = new EnginePlaceholderSceneModule();
        enginePlaceholderSceneModule.registerScenes(sceneRegistry);
        sceneModules.forEach(module -> module.registerScenes(sceneRegistry));
        completePhase(completedPhases, phaseMessages, BootstrapPhase.STATIC_CONTENT_REGISTRIES,
                "Base static content, display definitions, and scene definitions registered.");

        // Game rules: validate required definitions before any screen can depend on them.
        contentRegistry.validateRules();
        imageDisplayRegistry.validateDisplayContent();
        enginePlaceholderContentModule.validate(contentRegistry, imageDisplayRegistry);
        staticContentModules.forEach(module -> module.validate(contentRegistry, imageDisplayRegistry));
        sceneRegistry.validateScenes();
        enginePlaceholderSceneModule.validateScenes(sceneRegistry);
        sceneModules.forEach(module -> module.validateScenes(sceneRegistry));
        completePhase(completedPhases, phaseMessages, BootstrapPhase.GAME_RULES,
                "Required content, display, and scene definitions validated.");

        SceneExecutor sceneExecutor = new SceneExecutor(sceneRegistry);

        // UI routes/controllers: replace label and jump targets with explicit route IDs.
        GameState newGameState = gameStateFactory.createNewGame(contentRegistry);
        RouteContext routeContext = new RouteContext(
                primaryStage,
                preferencesService,
                contentRegistry,
                imageDisplayRegistry,
                saveLoadService,
                randomService,
                gameSupportService,
                audioService,
                newGameState,
                sceneRegistry,
                sceneExecutor,
                uiTheme,
                sceneRouter,
                applicationRoot,
                resourceConfig);
        List<RouteModule> allRouteModules = new ArrayList<>();
        allRouteModules.add(new DefaultRouteModule());
        allRouteModules.addAll(routeModules);
        sceneRouter.registerRoutes(routeContext, allRouteModules);
        sceneRouter.validateRouteDefinitions(contentRegistry);
        GlobalApiAdapter globalApiAdapter = new GlobalApiAdapter(randomService, sceneRouter, audioService);
        completePhase(completedPhases, phaseMessages, BootstrapPhase.UI_ROUTES_AND_CONTROLLERS,
                "Default JavaFX shell routes and Global API adapter registered and validated.");

        // Runtime state: create mutable new-game state only after services and registries exist.
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
                sceneRegistry,
                sceneExecutor,
                globalApiAdapter,
                sceneRouter,
                uiTheme,
                newGameState,
                applicationRoot,
                resourceConfig,
                resourceRegistry,
                bootstrapReport);
    }

    // ----- Config-driven app chrome (window title / icon / custom palette) --------------------

    /** Reserved {@code resources} ids for config-driven app chrome. */
    private static final String THEME_PALETTE_RESOURCE_ID = "themePalette";
    private static final String WINDOW_TITLE_RESOURCE_ID = "windowTitle";
    private static final String APP_ICON_RESOURCE_ID = "appIcon";
    private static final String CLASSPATH_SCHEME = "classpath:";

    /** Collects per-resource icon repoints from {@code resources} entries keyed
     *  {@code icon:<originalPath>} into an originalPath → replacementPath map. */
    private java.util.Map<String, String> collectIconAliases() {
        if (resourceConfig == null) {
            return java.util.Map.of();
        }
        java.util.LinkedHashMap<String, String> aliases = new java.util.LinkedHashMap<>();
        resourceConfig.resources().forEach((id, value) -> {
            if (id.startsWith(com.eb.javafx.util.ResourceOverrides.ALIAS_RESOURCE_PREFIX)) {
                String original = id.substring(
                        com.eb.javafx.util.ResourceOverrides.ALIAS_RESOURCE_PREFIX.length());
                if (!original.isBlank()) {
                    aliases.put(original, value);
                }
            }
        });
        return aliases;
    }

    /** Resolves a {@code resources} entry to an existing file on disk, or null. */
    private java.nio.file.Path resolveConfigFile(String resourceId) {
        if (resourceConfig == null) {
            return null;
        }
        return resourceConfig.resolveResource(applicationRoot, resourceId)
                .filter(java.nio.file.Files::isRegularFile)
                .orElse(null);
    }

    /** Applies the config-driven window title (falling back to the preference default) and the
     *  optional app/taskbar icon to {@code primaryStage}.  Best-effort; failures are logged. */
    private void applyWindowChrome(javafx.stage.Stage primaryStage) {
        if (primaryStage == null) {
            return;
        }
        String configuredTitle = resourceConfig == null ? null
                : resourceConfig.resourcePath(WINDOW_TITLE_RESOURCE_ID)
                        .filter(value -> !value.isBlank()).orElse(null);
        primaryStage.setTitle(configuredTitle != null ? configuredTitle : preferencesService.windowTitle());

        if (resourceConfig != null) {
            resourceConfig.resourcePath(APP_ICON_RESOURCE_ID)
                    .flatMap(this::loadAppIcon)
                    .ifPresent(icon -> primaryStage.getIcons().add(icon));
        }
    }

    /** Loads an app-icon image from a {@code classpath:}-prefixed path, an on-disk file under the
     *  application root, or a bare classpath resource — whichever resolves first.  Empty on any
     *  failure so a bad icon spec never aborts boot. */
    private java.util.Optional<javafx.scene.image.Image> loadAppIcon(String spec) {
        if (spec == null || spec.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            String trimmed = spec.trim();
            if (trimmed.startsWith(CLASSPATH_SCHEME)) {
                return loadIconFromClasspath(trimmed.substring(CLASSPATH_SCHEME.length()));
            }
            if (applicationRoot != null) {
                java.nio.file.Path file = applicationRoot.resolve(trimmed);
                if (java.nio.file.Files.isRegularFile(file)) {
                    try (java.io.InputStream in = java.nio.file.Files.newInputStream(file)) {
                        return imageOrEmpty(new javafx.scene.image.Image(in));
                    }
                }
            }
            return loadIconFromClasspath(trimmed);
        } catch (java.io.IOException | RuntimeException exception) {
            System.err.println("[BootstrapService] Could not load app icon '" + spec + "': " + exception);
            return java.util.Optional.empty();
        }
    }

    private static java.util.Optional<javafx.scene.image.Image> loadIconFromClasspath(String path)
            throws java.io.IOException {
        String normalized = path.startsWith("/") ? path : "/" + path;
        try (java.io.InputStream in = BootstrapService.class.getResourceAsStream(normalized)) {
            if (in == null) {
                return java.util.Optional.empty();
            }
            return imageOrEmpty(new javafx.scene.image.Image(in));
        }
    }

    private static java.util.Optional<javafx.scene.image.Image> imageOrEmpty(javafx.scene.image.Image image) {
        return image.isError() ? java.util.Optional.empty() : java.util.Optional.of(image);
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

    private static Path defaultApplicationRoot() {
        return Paths.get("").toAbsolutePath().normalize();
    }
}
