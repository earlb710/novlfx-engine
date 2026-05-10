package com.eb.javafx.bootstrap;

import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.content.JsonDisplayContentModule;
import com.eb.javafx.routing.RouteModule;
import com.eb.javafx.scene.JsonConversationContentModule;
import com.eb.javafx.scene.JsonSceneModule;
import com.eb.javafx.scene.SceneModule;
import com.eb.javafx.util.Validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Application-supplied startup options for reusable bootstrap service creation. */
public final class BootstrapOptions {
    private final Path applicationRoot;
    private final ApplicationResourceConfig resourceConfig;
    private final List<StaticContentModule> staticContentModules;
    private final List<SceneModule> sceneModules;
    private final List<RouteModule> routeModules;

    private BootstrapOptions(
            Path applicationRoot,
            ApplicationResourceConfig resourceConfig,
            List<StaticContentModule> staticContentModules,
            List<SceneModule> sceneModules,
            List<RouteModule> routeModules) {
        this.applicationRoot = Validation.requireNonNull(applicationRoot, "Application root is required.")
                .toAbsolutePath()
                .normalize();
        this.resourceConfig = Validation.requireNonNull(resourceConfig, "Application resource config is required.");
        this.staticContentModules = List.copyOf(Validation.requireNonNull(
                staticContentModules,
                "Static content modules list is required."));
        this.sceneModules = List.copyOf(Validation.requireNonNull(sceneModules, "Scene modules list is required."));
        this.routeModules = List.copyOf(Validation.requireNonNull(routeModules, "Route modules list is required."));
    }

    /** Returns default options rooted at the current working directory. */
    public static BootstrapOptions defaults() {
        return of(Paths.get("").toAbsolutePath(), ApplicationResourceConfig.defaults());
    }

    /** Creates options rooted at an application directory with explicit resource locations. */
    public static BootstrapOptions of(Path applicationRoot, ApplicationResourceConfig resourceConfig) {
        return new BootstrapOptions(
                applicationRoot,
                resourceConfig,
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
        BootstrapOptions options = of(applicationRoot, resourceConfig);
        Path jsonResourceRoot = resourceConfig.resolveJsonResourceRoot(applicationRoot);
        Path appLoadPath = ApplicationJsonLoadDefinition.defaultPath(jsonResourceRoot);
        if (Files.isRegularFile(appLoadPath)) {
            return options.withApplicationJsonLoads(ApplicationJsonLoadDefinition.load(appLoadPath).loads());
        }
        return options;
    }

    public Path applicationRoot() {
        return applicationRoot;
    }

    public ApplicationResourceConfig resourceConfig() {
        return resourceConfig;
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
        return new BootstrapOptions(applicationRoot, resourceConfig, staticContentModules, sceneModules, routeModules);
    }

    public BootstrapOptions withResourceConfig(ApplicationResourceConfig resourceConfig) {
        return new BootstrapOptions(applicationRoot, resourceConfig, staticContentModules, sceneModules, routeModules);
    }

    public BootstrapOptions withStaticContentModules(List<StaticContentModule> staticContentModules) {
        return new BootstrapOptions(applicationRoot, resourceConfig, staticContentModules, sceneModules, routeModules);
    }

    public BootstrapOptions withSceneModules(List<SceneModule> sceneModules) {
        return new BootstrapOptions(applicationRoot, resourceConfig, staticContentModules, sceneModules, routeModules);
    }

    public BootstrapOptions withRouteModules(List<RouteModule> routeModules) {
        return new BootstrapOptions(applicationRoot, resourceConfig, staticContentModules, sceneModules, routeModules);
    }

    public BootstrapOptions withApplicationJsonLoads(List<ApplicationJsonLoad> loads) {
        Validation.requireNonNull(loads, "Application JSON loads are required.");
        ArrayList<StaticContentModule> updatedStaticModules = new ArrayList<>(staticContentModules);
        ArrayList<SceneModule> updatedSceneModules = new ArrayList<>(sceneModules);
        Path jsonResourceRoot = resourceConfig.resolveJsonResourceRoot(applicationRoot);
        for (ApplicationJsonLoad load : loads) {
            for (Path jsonPath : load.resolvePaths(jsonResourceRoot)) {
                switch (load.type()) {
                    case DISPLAY -> updatedStaticModules.add(new JsonDisplayContentModule(jsonPath));
                    case SCENE -> updatedSceneModules.add(new JsonSceneModule(jsonPath));
                    case CONVERSATION -> {
                        JsonConversationContentModule module = new JsonConversationContentModule(jsonPath);
                        updatedStaticModules.add(module);
                        updatedSceneModules.add(module);
                    }
                }
            }
        }
        return new BootstrapOptions(applicationRoot, resourceConfig, updatedStaticModules, updatedSceneModules, routeModules);
    }
}
