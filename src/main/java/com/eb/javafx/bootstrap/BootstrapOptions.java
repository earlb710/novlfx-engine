package com.eb.javafx.bootstrap;

import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.content.JsonDisplayContentModule;
import com.eb.javafx.resources.ResourceCategory;
import com.eb.javafx.resources.ResourceRegistry;
import com.eb.javafx.routing.RouteModule;
import com.eb.javafx.scene.JsonConversationContentModule;
import com.eb.javafx.scene.JsonSceneModule;
import com.eb.javafx.scene.SceneModule;
import com.eb.javafx.ui.GlobalStylesheets;
import com.eb.javafx.util.FontResources;
import com.eb.javafx.util.Validation;
import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

/** Application-supplied startup options for reusable bootstrap service creation. */
public final class BootstrapOptions {
    /** Classpath path of the library's bundled {@code config.json}. */
    public static final String LIBRARY_CONFIG_RESOURCE = "/config.json";

    private final Path applicationRoot;
    private final ApplicationResourceConfig resourceConfig;
    private final ResourceRegistry resourceRegistry;
    private final List<StaticContentModule> staticContentModules;
    private final List<SceneModule> sceneModules;
    private final List<RouteModule> routeModules;

    private BootstrapOptions(
            Path applicationRoot,
            ApplicationResourceConfig resourceConfig,
            ResourceRegistry resourceRegistry,
            List<StaticContentModule> staticContentModules,
            List<SceneModule> sceneModules,
            List<RouteModule> routeModules) {
        this.applicationRoot = Validation.requireNonNull(applicationRoot, "Application root is required.")
                .toAbsolutePath()
                .normalize();
        this.resourceConfig = Validation.requireNonNull(resourceConfig, "Application resource config is required.");
        this.resourceRegistry = Validation.requireNonNull(resourceRegistry, "Resource registry is required.");
        this.staticContentModules = List.copyOf(Validation.requireNonNull(
                staticContentModules,
                "Static content modules list is required."));
        this.sceneModules = List.copyOf(Validation.requireNonNull(sceneModules, "Scene modules list is required."));
        this.routeModules = List.copyOf(Validation.requireNonNull(routeModules, "Route modules list is required."));
    }

    /** Returns default options rooted at the current working directory using only the library's bundled config. */
    public static BootstrapOptions defaults() {
        Path workingDir = Paths.get("").toAbsolutePath().normalize();
        ApplicationResourceConfig libraryConfig = loadLibraryConfig();
        ResourceRegistry registry = buildRegistry(
                ApplicationResourceConfig.defaults(), libraryConfig, workingDir);
        return new BootstrapOptions(
                workingDir,
                libraryConfig,
                registry,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
    }

    /** Creates options rooted at an application directory with explicit resource locations. */
    public static BootstrapOptions of(Path applicationRoot, ApplicationResourceConfig resourceConfig) {
        Path normalizedRoot = Validation.requireNonNull(applicationRoot, "Application root is required.")
                .toAbsolutePath()
                .normalize();
        ApplicationResourceConfig libraryConfig = loadLibraryConfig();
        ResourceRegistry registry = buildRegistry(resourceConfig, libraryConfig, normalizedRoot);
        return new BootstrapOptions(
                normalizedRoot,
                resourceConfig,
                registry,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
    }

    /**
     * Loads resource configuration from a JSON file and roots relative paths at the config parent directory.
     */
    public static BootstrapOptions fromConfig(Path configPath) {
        Path normalizedConfigPath = Validation.requireNonNull(configPath, "Bootstrap config path is required.")
                .toAbsolutePath()
                .normalize();
        Path parent = normalizedConfigPath.getParent();
        Path applicationRoot = parent == null ? Paths.get("").toAbsolutePath().normalize() : parent;
        ApplicationResourceConfig resourceConfig = ApplicationResourceConfig.load(normalizedConfigPath);
        ApplicationResourceConfig libraryConfig = loadLibraryConfig();
        ResourceRegistry registry = buildRegistry(resourceConfig, libraryConfig, applicationRoot);
        BootstrapOptions options = new BootstrapOptions(
                applicationRoot,
                resourceConfig,
                registry,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList());
        Optional<URL> appLoadUrl = ApplicationJsonLoadDefinition.defaultUrl(registry);
        if (appLoadUrl.isPresent()) {
            return options.withApplicationJsonLoads(ApplicationJsonLoadDefinition.load(appLoadUrl.get()).loads());
        }
        return options;
    }

    public Path applicationRoot() {
        return applicationRoot;
    }

    public ApplicationResourceConfig resourceConfig() {
        return resourceConfig;
    }

    /** Returns the resource registry built from the library's bundled config plus any application overrides. */
    public ResourceRegistry resourceRegistry() {
        return resourceRegistry;
    }

    public List<StaticContentModule> staticContentModules() {
        return staticContentModules;
    }

    public List<SceneModule> sceneModules() {
        return sceneModules;
    }

    public List<RouteModule> routeModules() {
        return routeModules;
    }

    public BootstrapOptions withApplicationRoot(Path applicationRoot) {
        return new BootstrapOptions(applicationRoot, resourceConfig, resourceRegistry,
                staticContentModules, sceneModules, routeModules);
    }

    public BootstrapOptions withResourceConfig(ApplicationResourceConfig resourceConfig) {
        return new BootstrapOptions(applicationRoot, resourceConfig, resourceRegistry,
                staticContentModules, sceneModules, routeModules);
    }

    /** Returns a copy with the supplied {@link ResourceRegistry}, replacing the previously built one. */
    public BootstrapOptions withResourceRegistry(ResourceRegistry resourceRegistry) {
        return new BootstrapOptions(applicationRoot, resourceConfig, resourceRegistry,
                staticContentModules, sceneModules, routeModules);
    }

    public BootstrapOptions withStaticContentModules(List<StaticContentModule> staticContentModules) {
        return new BootstrapOptions(applicationRoot, resourceConfig, resourceRegistry,
                staticContentModules, sceneModules, routeModules);
    }

    public BootstrapOptions withSceneModules(List<SceneModule> sceneModules) {
        return new BootstrapOptions(applicationRoot, resourceConfig, resourceRegistry,
                staticContentModules, sceneModules, routeModules);
    }

    public BootstrapOptions withRouteModules(List<RouteModule> routeModules) {
        return new BootstrapOptions(applicationRoot, resourceConfig, resourceRegistry,
                staticContentModules, sceneModules, routeModules);
    }

    public BootstrapOptions withApplicationJsonLoads(List<ApplicationJsonLoad> loads) {
        Validation.requireNonNull(loads, "Application JSON loads are required.");
        ArrayList<StaticContentModule> updatedStaticModules = new ArrayList<>(staticContentModules);
        ArrayList<SceneModule> updatedSceneModules = new ArrayList<>(sceneModules);
        for (ApplicationJsonLoad load : loads) {
            for (URL jsonUrl : load.resolveUrls(resourceRegistry)) {
                switch (load.type()) {
                    case DISPLAY -> updatedStaticModules.add(new JsonDisplayContentModule(jsonUrl));
                    case SCENE -> updatedSceneModules.add(new JsonSceneModule(jsonUrl));
                    case CONVERSATION -> {
                        JsonConversationContentModule module = new JsonConversationContentModule(jsonUrl);
                        updatedStaticModules.add(module);
                        updatedSceneModules.add(module);
                    }
                }
            }
        }
        return new BootstrapOptions(applicationRoot, resourceConfig, resourceRegistry,
                updatedStaticModules, updatedSceneModules, routeModules);
    }

    /**
     * Loads the library's bundled {@code config.json} from the classpath. Returns {@link
     * ApplicationResourceConfig#defaults()} when the bundled config is not present (e.g. running outside the
     * normal build artifact layout).
     */
    static ApplicationResourceConfig loadLibraryConfig() {
        URL libraryConfigUrl = BootstrapOptions.class.getResource(LIBRARY_CONFIG_RESOURCE);
        if (libraryConfigUrl == null) {
            return ApplicationResourceConfig.defaults();
        }
        try (InputStream stream = libraryConfigUrl.openStream()) {
            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return ApplicationResourceConfig.fromJson(json, libraryConfigUrl.toString());
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to load library " + LIBRARY_CONFIG_RESOURCE, exception);
        }
    }

    /**
     * Builds a {@link ResourceRegistry} by walking application roots first, then library roots. First-occurrence
     * wins inside each category, so applications can override library files of the same relative name.
     */
    static ResourceRegistry buildRegistry(
            ApplicationResourceConfig appConfig,
            ApplicationResourceConfig libraryConfig,
            Path applicationRoot) {
        ResourceRegistry.Builder builder = ResourceRegistry.builder();
        ClassLoader loader = BootstrapOptions.class.getClassLoader();
        addConfigRoots(builder, appConfig, applicationRoot, loader);
        addConfigRoots(builder, libraryConfig, applicationRoot, loader);
        return builder.build();
    }

    private static void addConfigRoots(ResourceRegistry.Builder builder, ApplicationResourceConfig config,
            Path applicationRoot, ClassLoader loader) {
        for (ResourceCategory category : ResourceCategory.values()) {
            for (String spec : config.resourceRoots(category)) {
                builder.addRoot(category, spec, applicationRoot, loader);
            }
        }
    }

    // -------------------------------------------------------------------------------------------
    // Extension-discovery SPI assembly (see docs/SPI_PLAN.md). `discovering(...)` mirrors
    // `fromConfig(...)` but additionally discovers EngineModuleProvider implementations (explicit +
    // ServiceLoader), orders them, and lets each contribute modules / resource roots / fonts /
    // stylesheets BEFORE the resource registry is frozen. Root layering stays:
    // application roots -> provider roots -> library roots (first match wins).
    // -------------------------------------------------------------------------------------------

    /** Like {@link #fromConfig(Path)}, plus SPI discovery. Explicit providers take precedence over
     *  discovered ones of the same {@link EngineModuleProvider#id() id}. */
    public static BootstrapOptions discovering(Path configPath, EngineModuleProvider... explicitProviders) {
        Path normalizedConfigPath = Validation.requireNonNull(configPath, "Bootstrap config path is required.")
                .toAbsolutePath()
                .normalize();
        Path parent = normalizedConfigPath.getParent();
        Path applicationRoot = parent == null ? Paths.get("").toAbsolutePath().normalize() : parent;
        ApplicationResourceConfig resourceConfig = ApplicationResourceConfig.load(normalizedConfigPath);
        return discovering(applicationRoot, resourceConfig, List.of(explicitProviders));
    }

    /** SPI discovery rooted at an explicit application root + already-parsed config. */
    public static BootstrapOptions discovering(Path applicationRoot, ApplicationResourceConfig resourceConfig,
            List<EngineModuleProvider> explicitProviders) {
        Path normalizedRoot = Validation.requireNonNull(applicationRoot, "Application root is required.")
                .toAbsolutePath()
                .normalize();
        Validation.requireNonNull(resourceConfig, "Application resource config is required.");
        ApplicationResourceConfig libraryConfig = loadLibraryConfig();

        List<EngineModuleProvider> providers = orderProviders(
                explicitProviders == null ? List.of() : explicitProviders);

        ResourceRegistry.Builder builder = ResourceRegistry.builder();
        ClassLoader loader = BootstrapOptions.class.getClassLoader();
        addConfigRoots(builder, resourceConfig, normalizedRoot, loader);     // application roots first

        DiscoveryContext context = new DiscoveryContext(normalizedRoot, resourceConfig, builder);
        for (EngineModuleProvider provider : providers) {
            try {
                provider.contribute(context);                                // provider roots + modules + fonts + css
            } catch (RuntimeException exception) {
                System.err.println("[BootstrapOptions] EngineModuleProvider '" + provider.id()
                        + "' failed to contribute: " + exception);
                exception.printStackTrace();
            }
        }

        addConfigRoots(builder, libraryConfig, normalizedRoot, loader);      // library roots last
        ResourceRegistry registry = builder.build();

        BootstrapOptions options = new BootstrapOptions(
                normalizedRoot, resourceConfig, registry,
                context.staticContentModules, context.sceneModules, context.routeModules);

        Optional<URL> appLoadUrl = ApplicationJsonLoadDefinition.defaultUrl(registry);
        if (appLoadUrl.isPresent()) {
            return options.withApplicationJsonLoads(ApplicationJsonLoadDefinition.load(appLoadUrl.get()).loads());
        }
        return options;
    }

    /** Combines explicit + ServiceLoader-discovered providers, de-dups by id (explicit wins), sorts
     *  by {@link EngineModuleProvider#priority() priority}, then resolves {@code dependsOn()} into a
     *  deterministic topological order. */
    static List<EngineModuleProvider> orderProviders(List<EngineModuleProvider> explicit) {
        LinkedHashMap<String, EngineModuleProvider> byId = new LinkedHashMap<>();
        for (EngineModuleProvider provider : explicit) {
            if (provider != null) {
                byId.putIfAbsent(provider.id(), provider);
            }
        }
        for (EngineModuleProvider provider : ServiceLoader.load(EngineModuleProvider.class)) {
            byId.putIfAbsent(provider.id(), provider);
        }
        List<EngineModuleProvider> nodes = new ArrayList<>(byId.values());
        nodes.sort(Comparator.comparingInt(EngineModuleProvider::priority));     // stable: ties keep order
        return topologicalByDependsOn(nodes);
    }

    /** Stable topological order over {@code dependsOn()} that preserves the incoming (priority) order
     *  for ready nodes. Unknown deps are ignored; cycles are broken by taking the first remaining
     *  node (nothing is dropped). */
    private static List<EngineModuleProvider> topologicalByDependsOn(List<EngineModuleProvider> ordered) {
        Set<String> present = new HashSet<>();
        for (EngineModuleProvider provider : ordered) {
            present.add(provider.id());
        }
        List<EngineModuleProvider> remaining = new ArrayList<>(ordered);
        List<EngineModuleProvider> result = new ArrayList<>();
        Set<String> emitted = new HashSet<>();
        while (!remaining.isEmpty()) {
            EngineModuleProvider next = null;
            for (EngineModuleProvider provider : remaining) {
                boolean depsReady = true;
                for (String dep : provider.dependsOn()) {
                    if (present.contains(dep) && !emitted.contains(dep)) {
                        depsReady = false;
                        break;
                    }
                }
                if (depsReady) {
                    next = provider;
                    break;                                                   // first in priority order
                }
            }
            if (next == null) {
                next = remaining.get(0);                                      // cycle / unsatisfiable
            }
            remaining.remove(next);
            emitted.add(next.id());
            result.add(next);
        }
        return result;
    }

    /** {@link ModuleContext} implementation backing {@link #discovering}. Collects modules into
     *  lists, registers fonts immediately (a side effect of {@link FontResources#loadResource}),
     *  funnels resource roots into the in-progress registry builder, and routes stylesheets to the
     *  global sink. */
    private static final class DiscoveryContext implements ModuleContext {
        private final Path applicationRoot;
        private final ApplicationResourceConfig resourceConfig;
        private final ResourceRoots resourceRoots;
        private final FontRegistrar fonts;
        final List<StaticContentModule> staticContentModules = new ArrayList<>();
        final List<SceneModule> sceneModules = new ArrayList<>();
        final List<RouteModule> routeModules = new ArrayList<>();

        DiscoveryContext(Path applicationRoot, ApplicationResourceConfig resourceConfig,
                ResourceRegistry.Builder builder) {
            this.applicationRoot = applicationRoot;
            this.resourceConfig = resourceConfig;
            this.resourceRoots = new ResourceRoots() {
                @Override public ResourceRoots addClasspathRoot(ResourceCategory category, String classpathPath,
                        ClassLoader classLoader) {
                    builder.addClasspathRoot(category, classpathPath, classLoader);
                    return this;
                }
                @Override public ResourceRoots addFilesystemRoot(ResourceCategory category, Path directory) {
                    builder.addFilesystemRoot(category, directory);
                    return this;
                }
            };
            this.fonts = new FontRegistrar() {
                @Override public Font registerFromModule(Class<?> owner, String resourcePath, double size) {
                    return FontResources.loadResource(resourcePath, size, owner.getClassLoader());
                }
                @Override public void register(Font font) {
                    // Font.loadFont already registers the family globally; this is a tracking hook.
                }
            };
        }

        @Override public Path applicationRoot() { return applicationRoot; }
        @Override public ApplicationResourceConfig resourceConfig() { return resourceConfig; }
        @Override public Path providerAssetBase() { return applicationRoot; }
        @Override public void addStaticContentModule(StaticContentModule module) { staticContentModules.add(module); }
        @Override public void addSceneModule(SceneModule module) { sceneModules.add(module); }
        @Override public void addRouteModule(RouteModule module) { routeModules.add(module); }
        @Override public ResourceRoots resourceRoots() { return resourceRoots; }
        @Override public FontRegistrar fonts() { return fonts; }
        @Override public void addStylesheet(String url) { GlobalStylesheets.add(url); }
    }
}
