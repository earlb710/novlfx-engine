package com.eb.javafx.bootstrap;

import com.eb.javafx.content.StaticContentModule;
import com.eb.javafx.resources.ResourceRegistry;
import com.eb.javafx.routing.RouteModule;
import com.eb.javafx.scene.SceneModule;

import java.nio.file.Path;

/**
 * The surface an {@link EngineModuleProvider} uses to contribute modules and resources during
 * bootstrap assembly. It carries the resolved inputs a provider needs and the registration sinks it
 * pushes into.
 *
 * <p>Resource rule: a provider <strong>owns its in-jar resources</strong> — it loads them through
 * its own module (see {@link #fonts()} / {@link #addStylesheet(String)}) or registers a classpath
 * root with its own class loader (see {@link #resourceRoots()}); the engine resolves only filesystem
 * resources (relative to {@link #providerAssetBase()}) and host-level overrides. This keeps the JPMS
 * encapsulation boundary clean. See {@code docs/SPI_PLAN.md} §4.</p>
 */
public interface ModuleContext {

    // ---- Resolved inputs --------------------------------------------------------------------

    /** The resolved application content root (the base for filesystem resources). */
    Path applicationRoot();

    /** The parsed application configuration ({@code config.json}). */
    ApplicationResourceConfig resourceConfig();

    /** The resource registry resolved from configuration roots (application + library), available
     *  for a provider to construct content modules that resolve their assets up front (e.g. a code
     *  table whose JSON is found via the registry). Provider-contributed roots (see
     *  {@link #resourceRoots()}) are layered into the engine's final runtime registry but are not
     *  reflected here — a provider resolves its <em>own</em> bundled resources through its module,
     *  not through this registry. */
    ResourceRegistry resourceRegistry();

    /** A per-provider base directory for this provider's <em>relative</em> filesystem assets, so two
     *  providers' relative paths don't collide. Defaults to {@link #applicationRoot()} unless the
     *  host scopes it per provider. */
    Path providerAssetBase();

    // ---- Module registration ----------------------------------------------------------------

    /** Registers a static content module (code tables, display content, etc.). */
    void addStaticContentModule(StaticContentModule module);

    /** Registers a scene module. */
    void addSceneModule(SceneModule module);

    /** Registers a route module. */
    void addRouteModule(RouteModule module);

    // ---- Resource registration --------------------------------------------------------------

    /** Adds resource search roots (classpath roots resolved with the provider's own class loader,
     *  and filesystem roots) layered into the engine's resource registry. */
    ResourceRoots resourceRoots();

    /** Registers fonts bundled in the provider's module so their families resolve in CSS. */
    FontRegistrar fonts();

    /** Adds a stylesheet URL the provider produced from its <em>own</em> {@code getResource(...)}
     *  (so the URL carries the provider module's class loader and loads without encapsulation
     *  issues). */
    void addStylesheet(String url);
}
