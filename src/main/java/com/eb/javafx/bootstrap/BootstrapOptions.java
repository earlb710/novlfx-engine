package com.eb.javafx.bootstrap;

import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.routing.RouteModule;
import com.eb.javafx.scene.SceneModule;
import com.eb.javafx.util.Validation;

import java.nio.file.Path;
import java.nio.file.Paths;
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
        return of(applicationRoot, ApplicationResourceConfig.load(normalizedConfigPath));
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
}
