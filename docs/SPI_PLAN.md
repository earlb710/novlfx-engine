# Extension-Discovery SPI — Implementation Plan

Goal: let a project **depend on** the engine and **extend** it by dropping a jar on the path,
instead of forking/editing a central wiring method. Includes first-class handling of provider
resources (in-jar fonts/CSS/JSON and on-disk backgrounds) so plugins never fight JPMS encapsulation.

Backward-compatible: the existing explicit `BootstrapOptions` lists keep working; discovery is
additive.

---

## 1. Current state (what we're replacing)

- `BootstrapOptions` holds `List<StaticContentModule>` / `List<SceneModule>` / `List<RouteModule>`.
- `GameApplication.createBootstrapOptions()` news-up every module by hand (with constructor args
  derived from `applicationRoot` + `ApplicationResourceConfig` + registries) and stuffs the lists.
- `BootstrapService.boot()` consumes them: `staticContentModules.forEach(register)` →
  `sceneModules.forEach(registerScenes)` → validate pass → `sceneRouter.registerRoutes(...)`.
- `ResourceRegistry` is **built (frozen) before boot** from a `Builder` whose roots each carry a
  `ClassLoader`; roots are queried in insertion order, app roots before library roots.
- `FontResources` loads only from the **engine** classloader (`/com/eb/javafx/fonts`).

The constraint that shapes everything: modules need resolved context (root/config/registry) to be
constructed, so a bare `ServiceLoader.load(StaticContentModule.class)` (which requires a no-arg
ctor) cannot build them. We discover **factories**, not modules.

---

## 2. The SPI types (new, in `com.eb.javafx.bootstrap`)

```java
public interface EngineModuleProvider {                 // discovered; must have a public no-arg ctor
    default String id()       { return getClass().getName(); }   // identity for dedup / dependsOn
    default int    priority() { return 0; }                      // lower contributes first
    default Set<String> dependsOn() { return Set.of(); }         // optional: ids that must precede this
    void contribute(ModuleContext context);                      // register everything via the context
}
```

`contribute()` is void and push-based (registers into the context) rather than returning a bundle —
this lets a provider contribute resource roots, fonts, stylesheets, AND content/scene/route modules
through one typed surface.

```java
public interface ModuleContext {
    // resolved inputs (what createBootstrapOptions threads in today)
    Path                      applicationRoot();
    ApplicationResourceConfig resourceConfig();

    // module registration
    void addStaticContentModule(StaticContentModule module);
    void addSceneModule(SceneModule module);
    void addRouteModule(RouteModule module);

    // resource registration (see §4)
    ResourceRoots resourceRoots();        // add classpath roots (with the provider's ClassLoader) + filesystem roots
    FontRegistrar fonts();                // register a font from the provider's module / a Font instance
    void addStylesheet(String url);       // a CSS URL the provider produced via its own getResource(...)
    Path providerAssetBase();             // per-provider on-disk base for relative filesystem assets
}
```

---

## 3. Discovery & ordering

Discovery uses `java.util.ServiceLoader`, which works for both transports:

- **Modular consumer** — `module-info.java`:
  `provides com.eb.javafx.bootstrap.EngineModuleProvider with com.acme.game.AcmeModuleProvider;`
- **Engine** — `module-info.java`: `uses com.eb.javafx.bootstrap.EngineModuleProvider;`
- **Classpath consumer** — `META-INF/services/com.eb.javafx.bootstrap.EngineModuleProvider`.

`ServiceLoader` iteration order is **unspecified**, so we impose determinism:
`providers.sort(comparingInt(EngineModuleProvider::priority))`, then a topological pass honoring
`dependsOn()`. Dedup by `id()`; an explicitly-passed provider **wins** over a discovered one of the
same id (host override).

---

## 4. Resource handling (the folded-in part)

The rule, enforced by the API shape: **a provider owns its in-jar resources; the engine resolves
only filesystem resources and host-level overrides — it never reaches into a provider's jar by
string path.** This makes the "works on classpath, breaks on module path" trap impossible.

### 4.1 In-jar resources (fonts / CSS / scene JSON / SVG)

Under JPMS, a resource in a package that contains `.class` files is encapsulated to its module
(see the engine's own `opens com.eb.javafx.ui;` / `opens com.eb.javafx.gamesupport;`). So providers
contribute in-jar resources through one of these, all of which the context supports:

- **Fonts** — `ModuleContext.fonts()`:
  ```java
  ctx.fonts().registerFromModule("Acme Sans", getClass(), "/com/acme/fonts/AcmeSans.ttf");
  // or, if already loaded: ctx.fonts().register(Font.loadFont(getClass().getResourceAsStream(...), 12));
  ```
  Backed by a new **module-aware `FontResources` overload** that resolves against a supplied
  `Class`/`ClassLoader` instead of the engine's:
  ```java
  public static Font loadResource(String resourcePath, double size, ClassLoader loader);
  // existing loadResource(path, size) delegates with FontResources.class.getClassLoader()
  ```
- **Stylesheets** — provider builds the URL itself (same-module access is always allowed) and hands
  it over: `ctx.addStylesheet(getClass().getResource("/com/acme/theme.css").toExternalForm());`
- **Scene JSON / SVG / meshes** — provider reads them in `contribute()` (same-module) and registers
  parsed content into the registries via its `StaticContentModule`/`SceneModule`.
- **Classpath resource roots** — `ResourceRoots.addClasspathRoot(category, path, getClass().getClassLoader())`.
  Works cross-module **only for resource-only packages** (no `.class`), mirroring the engine's
  `com.eb.javafx.ui.screens`. Document this: a provider that ships bundled assets should keep them
  in a resource-only package **or** `opens` the package in its own `module-info`.

### 4.2 On-disk resources (backgrounds / override root / external CSS+fonts / config)

JPMS-immune (filesystem I/O). Unchanged in mechanism, but discovery adds multi-source needs:

- `ModuleContext.providerAssetBase()` gives each provider its own base for **relative** filesystem
  assets, so two providers' `images/` dirs don't collide.
- `ResourceRoots.addFilesystemRoot(category, dir)` lets a provider contribute an additional on-disk
  search root (layered into `ResourceRegistry`, app-before-library order preserved).
- The host-level `ResourceOverrides` root stays **singular and host-owned** (it overrides everyone);
  it is not per-provider.

### 4.3 Inside vs outside, at a glance

| Resource | Location | Provider path |
|---|---|---|
| Fonts in plugin jar | classpath | `ctx.fonts().registerFromModule(...)` (module-aware loader) |
| CSS/theme in plugin jar | classpath | `ctx.addStylesheet(getClass().getResource(...).toExternalForm())` |
| Scene JSON / SVG / meshes in plugin jar | classpath | read in `contribute()` (same-module), register parsed |
| Classpath asset root | classpath | `resourceRoots().addClasspathRoot(.., getClass().getClassLoader())` — resource-only pkg or `opens` |
| Background images | filesystem | resolved under `providerAssetBase()` / a contributed filesystem root |
| `config.json`, external CSS/fonts, mapBuildingColors | filesystem | unchanged (host-owned) |
| Asset-override root (`ResourceOverrides`) | filesystem | host-level only, not per-provider |

---

## 5. Sequencing (critical)

`ResourceRegistry` is **frozen before boot**, so providers that add resource roots must run during
**options assembly**, not mid-boot. New flow, in a `BootstrapOptions.discovering(applicationRoot)`
factory:

1. `BootstrapOptions.fromConfig(...)` → load `config.json` → `ApplicationResourceConfig`.
2. Create a `ResourceRegistry.Builder` and a `ModuleContext` over (root, config, builder, collectors).
3. Discover providers (`ServiceLoader` + any explicit), sort/dedup, call `contribute(context)` on each
   → they add resource roots, fonts, stylesheets, and content/scene/route modules.
4. Freeze the registry (`builder.build()`), collect the module lists + fonts + stylesheets.
5. Build `BootstrapOptions` carrying the populated lists (+ the collected fonts/stylesheets so boot
   applies them).

`BootstrapService.boot()` then consumes the lists exactly as today (insertion points unchanged:
register at the `staticContentModules.forEach(...)` site, validate at the validate pass, routes at
`sceneRouter.registerRoutes(...)`), and applies the collected fonts/stylesheets where it already
calls `ConfiguredFonts.register(...)`.

---

## 6. Backward compatibility

- All existing `BootstrapOptions` constructors and the explicit lists remain. `discovering(...)` is a
  new factory, not a replacement.
- The hybrid merge: explicit modules + discovered modules, deduped by `id()` (explicit wins).
- The engine's own defaults (`EnginePlaceholderContentModule`, `EnginePlaceholderSceneModule`,
  `DefaultRouteModule`) become **engine-internal providers at low priority**, so a host that provides
  nothing still boots a working baseline. They are NOT exported as SPI providers to consumers.

---

## 7. AltLife migration (the proof)

- Add `AltLifeModuleProvider implements EngineModuleProvider`. Move the body of
  `createBootstrapOptions()` into `contribute(ctx)`: build `AltLifeLocationContent`,
  `AltLifeSceneContentModule`, `AltLifeGeneratedConversationModule`, etc. from `ctx.resourceConfig()`
  / `ctx.applicationRoot()`, and register them via `ctx.addStaticContentModule(...)` etc.
- Declare `provides com.eb.javafx.bootstrap.EngineModuleProvider with com.altlife.javafx.AltLifeModuleProvider;`
  in AltLife's `module-info`.
- `GameApplication.start()` calls `BootstrapOptions.discovering(detectApplicationRoot())` instead of
  `createBootstrapOptions(...)`. Net: the hand-maintained module lists disappear.

---

## 8. Tests

- **Engine**: a fake provider on the test classpath is discovered, ordered by `priority()`, and its
  modules/roots/fonts land in the assembled options (ServiceLoader via `META-INF/services` in test
  resources). Dedup-by-id (explicit wins). `dependsOn()` topo-order. Module-aware
  `FontResources.loadResource(path, size, loader)` resolves a font from a supplied loader.
- **AltLife**: `AltLifeModuleProvider.contribute(...)` registers the same module set the old
  `createBootstrapOptions()` did (assert registry contents equivalent); the app still boots.

---

## 9. Rollout phases

1. ✅ **Done** — Added SPI types (`EngineModuleProvider`, `ModuleContext`, `ResourceRoots`,
   `FontRegistrar` in `com.eb.javafx.bootstrap`) + the module-aware
   `FontResources.loadResource(path, size, ClassLoader)` overload. Pure additions; nothing is
   discovered or wired into boot yet, so there is no behavior change. Tests: `EngineModuleProviderTest`,
   `FontResourcesTest.moduleAwareLoadResolvesAgainstSuppliedClassLoader`.
2. ✅ **Done** — `BootstrapOptions.discovering(configPath, explicit...)` /
   `discovering(root, config, explicit)` discover providers (explicit + `ServiceLoader`), de-dup by
   `id()` (explicit wins), sort by `priority()`, then topologically by `dependsOn()`, and run
   `contribute(ModuleContext)` against an in-progress `ResourceRegistry.Builder` (root layering:
   application → provider → library). A throwing provider is isolated. Resource hooks are live:
   roots funnel into the builder, fonts register immediately via the module-aware loader, stylesheets
   go to the new `com.eb.javafx.ui.GlobalStylesheets` sink (applied to every themed scene in
   `RouteContext.themedScene`). Added `uses com.eb.javafx.bootstrap.EngineModuleProvider;` to
   `module-info`. The explicit `BootstrapOptions` path is unchanged. Tests:
   `BootstrapOptionsDiscoveryTest`.
3. ✅ **Done** — Engine defaults (`EnginePlaceholderContentModule`, `EnginePlaceholderSceneModule`,
   `DefaultRouteModule`) are now expressed as the internal lowest-priority `EngineDefaultsProvider`.
   `BootstrapService.boot()` composes it ahead of the application-supplied modules on **every** boot
   path (via a minimal collecting context), so the baseline is always present and registered first.
   It is deliberately **not** a `ServiceLoader` service and is kept **out of the option module
   lists** (so `BootstrapOptions.with*` replacements can't clobber it). Tests:
   `EngineDefaultsProviderTest`; AltLife `GameApplicationTest`/`GameApplicationRouteIntegrationTest`
   boot through it unchanged.
4. ✅ **Done** — `AltLifeModuleProvider` contributes AltLife's content/scene/route modules via
   `contribute(ModuleContext)`; `GameApplication.createBootstrapOptions(...)` now delegates to
   `BootstrapOptions.discovering(config, new AltLifeModuleProvider())` (so the boot path — and all
   11 existing boot tests — route through the SPI). AltLife declares
   `provides EngineModuleProvider with AltLifeModuleProvider` (verified in the compiled descriptor),
   making it ServiceLoader-discoverable on the module path; it is also passed explicitly so the path
   is robust on the class path too (de-duped by id). The migration surfaced one interface
   refinement: **`ModuleContext.resourceRegistry()`** (the config-resolved registry a provider needs
   to construct registry-backed modules such as `AltLifeCodeTableContentModule.fromRegistry`), so
   `discovering()` now builds the config registry first, exposes it during `contribute()`, and
   layers any provider-added roots into the final runtime registry. Tests:
   `GameApplicationTest` / `GameApplicationRouteIntegrationTest` boot through it unchanged.
5. ✅ **Done** — Provider contract documented for plugin authors in USER_MANUAL §14 ("Writing an
   extension — the module-provider SPI"), covering the provider interface, `ServiceLoader`
   declaration (modular `provides` / classpath `META-INF/services`), the `discovering(...)` entry
   point, the `ModuleContext` surface, the in-jar vs on-disk resource contract, and ordering.

---

## 10. Risks / gotchas

- **JPMS resolution**: `ServiceLoader` only sees providers in the resolved module graph; a provider
  module nobody `requires` needs `--add-modules`. Non-issue on the classpath.
- **Determinism**: discovery order is graph/filesystem dependent — the `priority()`/`dependsOn()`
  sort is what makes cron/headless runs reproducible.
- **Encapsulation trap**: the #1 failure is a provider letting the engine load its in-jar resource by
  path (works on classpath, null on module path). The push-based resource hooks + the "resource-only
  package or `opens`" contract prevent it by construction.
- **Error isolation**: wrap each `contribute()` so a bad provider fails loudly with its `id()` rather
  than silently dropping content or aborting boot.
- **Double registration**: enforce dedup-by-id, or two jars providing the "same" content collide.
