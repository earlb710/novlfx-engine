# Change summary: resource registry foundation

## Goal

Lift resource discovery out of hardcoded classpath strings into a single library manifest that the engine loads at startup, and let consuming applications layer their own directories on top per category.

## Schema

A new top-level `config.json` lives at the repository root (also bundled into the JAR at `/config.json` via `processResources`). It declares per-category resource directories under a `resourceRoots` map:

```json
{
  "resourceRoots": {
    "bootstrap": ["classpath:/com/eb/javafx/bootstrap"],
    "fonts":     ["classpath:/com/eb/javafx/fonts"],
    "images":    ["classpath:/com/eb/javafx/images"],
    "support":   ["classpath:/com/eb/javafx/gamesupport", "classpath:/com/eb/javafx/conversations"],
    "ui":        ["classpath:/com/eb/javafx/ui"]
  }
}
```

The old `src/main/resources/com/eb/javafx/bootstrap/config.json` is removed; the manifest now lives at the project root.

Root specs may use the `classpath:` prefix to address files inside the JAR (used by the library for its own directories) or be filesystem paths resolved against the application root (used by consuming apps).

## New code

- `com.eb.javafx.resources.ResourceCategory` — strict enum (`BOOTSTRAP`, `FONTS`, `IMAGES`, `SUPPORT`, `UI`).
- `com.eb.javafx.resources.ResourceRegistry` — catalogs files discovered under each category's roots. Recursive walk; classpath and filesystem; `jar:` URLs handled via `FileSystems.newFileSystem`. App roots take precedence over library roots within each category; categories never fall through.
- `ResourceRegistry.Builder.addRoot(category, spec, base, classLoader)` dispatches on `classpath:` prefix vs filesystem path.

## Bootstrap integration

- `BootstrapOptions.fromConfig(...)` and `BootstrapOptions.defaults()` load the library's bundled `config.json` from the classpath, parse the application config (if supplied), and build a `ResourceRegistry` before any boot phase runs. Application roots are added first so app files override library files.
- `BootstrapOptions.resourceRegistry()` exposes the built registry; `withResourceRegistry(...)` allows replacement.
- `BootstrapService` accepts and forwards the registry into `BootContext`.
- `BootContext.resourceRegistry()` lets downstream code query the registry.

## ApplicationResourceConfig

Additive field `Map<ResourceCategory, List<String>> resourceRoots`. JSON parsing accepts a top-level `resourceRoots` object with string-array values, validates entries non-blank, and rejects unknown category keys. Round-trips through `toJson()`/`save(...)`. `withResourceRoots(Map)` and `withAdditionalResourceRoot(category, spec)` mutators added.

Note: the existing `with*()` mutators (`withDebug`, `withImageAssetRoot`, etc.) reset `resourceRoots` to empty when called. Callers that need both a `with*()` change and a non-empty `resourceRoots` map should chain `withResourceRoots(...)` last. This is documented on the `resourceRoots()` accessor.

## Tests

- `ResourceRegistryTest` — covers recursive filesystem indexing, app-precedence on name collision, strict category isolation, missing-root tolerance, classpath walking of engine-bundled fonts, prefix dispatch by `addRoot`, key normalization (forward/backslash, leading slash), empty-registry behavior, and reading a resolved classpath URL.
- `ApplicationResourceConfigTest` — additional tests for `resourceRoots` parsing, JSON round-trip, unknown-category rejection, blank-entry rejection, and `withAdditionalResourceRoot` append order.
- `./gradlew --no-daemon compileJava testClasses` green; `./gradlew --no-daemon test --tests "com.eb.javafx.resources.*" --tests "com.eb.javafx.bootstrap.*"` green. The 9 unrelated UI/JavaFX test failures in `DefaultDisplayValuesApplicationTest`, `PreferencesSummaryScreenTest`, `BlockBackgroundImageTestScreenTest`, and `JsonBlockBackgroundImageTestScreenTest` reproduce on `origin/main` without these changes.

## Docs

`docs/USER_MANUAL.md` section 4 gains a "Library resource manifest and `ResourceRegistry`" subsection that documents the schema, the precedence rule, classpath vs filesystem roots, and how to access the registry from `BootContext`.

`README.md` placeholders added under `src/main/resources/com/eb/javafx/bootstrap` and `.../conversations` so the bundled classpath directories survive in git and the JAR.

## Follow-up (not in this PR)

Migrating `ImageDisplayRegistry`, `ApplicationJsonLoad(Definition)`, and remaining callers off `jsonResourceRoot` / `imageAssetRoot` / `categoryCodeTablesPath` onto the registry — and then deleting those fields — should land as a separate PR to keep the migration reviewable independently of the foundation.
