package com.eb.javafx.bootstrap;

import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.content.JsonDisplayContentModule;
import com.eb.javafx.resources.ResourceCategory;
import com.eb.javafx.resources.ResourceRegistry;
import com.eb.javafx.routing.RouteModule;
import com.eb.javafx.scene.JsonConversationContentModule;
import com.eb.javafx.scene.JsonSceneModule;
import com.eb.javafx.scene.SceneModule;
import com.eb.javafx.util.Validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        for (ResourceCategory category : ResourceCategory.values()) {
            for (String spec : appConfig.resourceRoots(category)) {
                builder.addRoot(category, spec, applicationRoot, loader);
            }
        }
        for (ResourceCategory category : ResourceCategory.values()) {
            for (String spec : libraryConfig.resourceRoots(category)) {
                builder.addRoot(category, spec, applicationRoot, loader);
            }
        }
        return builder.build();
    }
}
