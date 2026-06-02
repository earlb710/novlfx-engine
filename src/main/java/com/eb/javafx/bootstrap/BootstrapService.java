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

        // Config-driven startup window sizing + clamp bounds (the `window` object) and the
        // font-scale clamp range (`ui.fontScaleMin/Max`) must be applied BEFORE load() reads the
        // stored preferences, since load() clamps against these bounds.
        if (resourceConfig != null) {
            preferencesService.setWindowSizeBounds(
                    parsePositiveInt(resourceConfig.windowField("defaultWidth").orElse(null)),
                    parsePositiveInt(resourceConfig.windowField("defaultHeight").orElse(null)),
                    parsePositiveInt(resourceConfig.windowField("minWidth").orElse(null)),
                    parsePositiveInt(resourceConfig.windowField("maxWidth").orElse(null)),
                    parsePositiveInt(resourceConfig.windowField("minHeight").orElse(null)),
                    parsePositiveInt(resourceConfig.windowField("maxHeight").orElse(null)));
            preferencesService.setFontScaleBounds(
                    parsePositiveDouble(resourceConfig.uiField("fontScaleMin").orElse(null)),
                    parsePositiveDouble(resourceConfig.uiField("fontScaleMax").orElse(null)));
        }
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
        // colour / background / transparency, plus a global tooltip show-delay — layered over the
        // theme on every themed scene.  Plus footer keybinding/glyph overrides, configurable
        // text-speed durations, and the engine tooltip-delay override.
        if (resourceConfig != null) {
            com.eb.javafx.ui.FooterStyle.configure(
                    resourceConfig.footerStyle("font").orElse(null),
                    resourceConfig.footerStyle("color").orElse(null),
                    resourceConfig.footerStyle("selectColor").orElse(null),
                    resourceConfig.footerStyle("backgroundColor").orElse(null),
                    resourceConfig.footerStyle("transparency").orElse(null),
                    resourceConfig.tooltipDelayMillis().orElse(null));
            com.eb.javafx.ui.ScreenShell.setFooterOptionOverrides(collectFooterOptionOverrides());
            com.eb.javafx.ui.ScreenShell.setTooltipShowDelayMillis(parsePositiveDouble(
                    resourceConfig.tooltipDelayMillis().orElse(null)));
            // Minimum raster size for SVG backgrounds (`display.svgBackgroundMinRaster`).
            com.eb.javafx.ui.ScreenShell.setBackgroundSvgRasterMinSize(
                    parsePositiveInt(resourceConfig.displayField("svgBackgroundMinRaster.width").orElse(null)),
                    parsePositiveInt(resourceConfig.displayField("svgBackgroundMinRaster.height").orElse(null)));
            // Screen spacing / insets (`ui.spacing`) and footer rest/hover opacity (`footer.*Opacity`).
            com.eb.javafx.ui.ScreenShell.setSpacing(
                    parseDouble(resourceConfig.uiSpacingField("body").orElse(null)),
                    parseDouble(resourceConfig.uiSpacingField("outer").orElse(null)),
                    parseDouble(resourceConfig.uiSpacingField("panel").orElse(null)),
                    parseDouble(resourceConfig.uiSpacingField("footer").orElse(null)));
            com.eb.javafx.ui.ScreenShell.setFooterOpacity(
                    parseDouble(resourceConfig.footerStyle("restOpacity").orElse(null)),
                    parseDouble(resourceConfig.footerStyle("hoverOpacity").orElse(null)));
            preferencesService.setTextSpeedDurations(
                    parsePositiveInt(resourceConfig.textSpeedMillis("slow").orElse(null)),
                    parsePositiveInt(resourceConfig.textSpeedMillis("normal").orElse(null)),
                    parsePositiveInt(resourceConfig.textSpeedMillis("fast").orElse(null)));
            // Auto-advance cadence tuning (the `autoAdvance` config object).
            com.eb.javafx.ui.AutoSkipController.setAutoAdvanceTuning(
                    parsePositiveDouble(resourceConfig.autoAdvanceField("scrollFraction").orElse(null)),
                    parsePositiveInt(resourceConfig.autoAdvanceField("minScrollMs").orElse(null)),
                    parsePositiveDouble(resourceConfig.autoAdvanceField("readPauseMultiplier").orElse(null)));
            // Confirm/info/error popup card width (the `ui.dialog` config object).
            com.eb.javafx.ui.DialogMessages.setCardWidth(
                    parsePositiveDouble(resourceConfig.uiDialogField("minWidth").orElse(null)),
                    parsePositiveDouble(resourceConfig.uiDialogField("maxWidth").orElse(null)));
            // Dialog-block previous-entry fade opacity (`ui.dialog.previousEntryOpacity`).
            com.eb.javafx.ui.DialogEntriesView.setPreviousEntryOpacity(
                    parseDouble(resourceConfig.uiDialogField("previousEntryOpacity").orElse(null)));
            // Kinetic text-effect durations (`text.kineticEffects.{pulse,float,shake}`).
            com.eb.javafx.text.JavaFxRichTextRenderer.setKineticEffectDurations(
                    parsePositiveInt(resourceConfig.textKineticField("pulse").orElse(null)),
                    parsePositiveInt(resourceConfig.textKineticField("float").orElse(null)),
                    parsePositiveInt(resourceConfig.textKineticField("shake").orElse(null)));
            // Conversation-history sliding-window cap (`save.maxHistoryEntries`).
            com.eb.javafx.text.DialogHistory.setMaxConversations(
                    parsePositiveInt(resourceConfig.saveField("maxHistoryEntries").orElse(null)));
            // Save-tile thumbnail dimensions (`save.gridThumbnail*` / `save.listThumbnail*`).
            com.eb.javafx.ui.SaveScreen.setThumbnailSizes(
                    parsePositiveInt(resourceConfig.saveField("gridThumbnailWidth").orElse(null)),
                    parsePositiveInt(resourceConfig.saveField("gridThumbnailHeight").orElse(null)),
                    parsePositiveInt(resourceConfig.saveField("listThumbnailWidth").orElse(null)),
                    parsePositiveInt(resourceConfig.saveField("listThumbnailHeight").orElse(null)));
            // Persisted-thumbnail encoding (`save.thumbnail*`).  Resolution defaults to the
            // (configured) grid-tile size × a supersample factor so the saved preview stays crisp
            // for whatever tile size is in effect; an explicit save.thumbnailWidth/Height wins.
            Integer cfgThumbWidth  = parsePositiveInt(resourceConfig.saveField("thumbnailWidth").orElse(null));
            Integer cfgThumbHeight = parsePositiveInt(resourceConfig.saveField("thumbnailHeight").orElse(null));
            int targetThumbWidth = cfgThumbWidth != null ? cfgThumbWidth
                    : (int) Math.round(com.eb.javafx.ui.SaveScreen.gridThumbnailWidth() * THUMBNAIL_SUPERSAMPLE);
            int targetThumbHeight = cfgThumbHeight != null ? cfgThumbHeight
                    : (int) Math.round(com.eb.javafx.ui.SaveScreen.gridThumbnailHeight() * THUMBNAIL_SUPERSAMPLE);
            com.eb.javafx.save.SaveLoadService.setThumbnailEncoding(
                    targetThumbWidth, targetThumbHeight,
                    parseFloat(resourceConfig.saveField("thumbnailJpegQuality").orElse(null)));
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

    /** Builds the footer-option keybinding/glyph override map (id → {shortcut?, icon?}) from config. */
    private java.util.Map<String, java.util.Map<String, String>> collectFooterOptionOverrides() {
        if (resourceConfig == null) {
            return java.util.Map.of();
        }
        java.util.LinkedHashMap<String, java.util.Map<String, String>> overrides = new java.util.LinkedHashMap<>();
        for (String id : resourceConfig.footerOptionOverrideIds()) {
            java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
            resourceConfig.footerOptionOverride(id, "shortcut").ifPresent(v -> fields.put("shortcut", v));
            resourceConfig.footerOptionOverride(id, "icon").ifPresent(v -> fields.put("icon", v));
            if (!fields.isEmpty()) {
                overrides.put(id, fields);
            }
        }
        return overrides;
    }

    // Thin delegators to the shared engine parser (com.eb.javafx.util.ConfigValues) — kept as
    // short private names so the ~45 boot call sites stay readable; the logic lives in one place.
    private static Integer parsePositiveInt(String value) {
        return com.eb.javafx.util.ConfigValues.parsePositiveInt(value);
    }

    private static Double parsePositiveDouble(String value) {
        return com.eb.javafx.util.ConfigValues.parsePositiveDouble(value);
    }

    private static Double parseDouble(String value) {
        return com.eb.javafx.util.ConfigValues.parseDouble(value);
    }

    private static Float parseFloat(String value) {
        return com.eb.javafx.util.ConfigValues.parsePositiveFloat(value);
    }

    /** Default supersample factor: persisted thumbnails render at this multiple of the save-grid
     *  tile size so they stay crisp under Retina-style scaling (350×197 tile → ~490×276, matching
     *  the prior 480×270 default). */
    private static final double THUMBNAIL_SUPERSAMPLE = 1.4;

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
