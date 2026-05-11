# Change summary: resource registry caller migration

## Goal

Migrate engine callers off the legacy filesystem-only `categoryCodeTablesPath` / `imageAssetRoot` / `jsonResourceRoot` fields on `ApplicationResourceConfig`, onto the `ResourceRegistry` introduced in the foundation PR. JSON loaders now accept `URL` so files inside a packaged JAR are discoverable too.

## URL-aware loaders

New `ResourceIo` helper in `com.eb.javafx.resources` centralizes `URL` → UTF-8 string reading, the filesystem-path view for `file:` URLs, and the `Path` → `URL` conversion.

URL overloads added to:

- `DisplayDefinitionJsonLoader.loadInto(URL, ImageDisplayRegistry)`
- `SceneDefinitionJson.load(URL)`
- `ConversationDefinitionJson.load(URL)` / `load(URL, ConversationConditionVariables)`
- `CategoryCodeTableDefinition.load(URL)`

The existing `Path` overloads delegate via `ResourceIo.toUrl(path)`. URL constructors added on `JsonDisplayContentModule`, `JsonSceneModule`, and `JsonConversationContentModule` (alongside their `Path` constructors).

## ImageDisplayRegistry

New `ImageDisplayRegistry(ResourceRegistry)` constructor. Asset resolution is delegated through a `Function<String, Optional<URL>>` resolver; the registry-based constructor wires it to `registry.find(ResourceCategory.IMAGES, sourcePath)`, the legacy `Path`-based constructors keep the `GameAssetLocator` filesystem walk. `resolveAssetUrl(imageId)` returns the URL; `resolveAssetPath(imageId)` now narrows to filesystem URLs via `ResourceIo.toFilesystemPath` so callers that genuinely need a `Path` keep working.

`BootstrapService` constructs `ImageDisplayRegistry` from `options.resourceRegistry()`. The `(Path, Path)` legacy constructor stays as a back-compat seam for tests that want a plain filesystem locator.

## Application-load discovery

`ApplicationJsonLoad.resolvePaths(Path)` is replaced by `resolveUrls(ResourceRegistry)`, which looks up `support/<path>/<fileName>` (single file) or every immediate JSON child under `support/<path>/` (directory load). `ApplicationJsonLoadDefinition` gains `load(URL)` plus `defaultUrl(ResourceRegistry)` (registry lookup for `config/app-load.json` under SUPPORT); the legacy `defaultPath(Path)` is gone, since `BootstrapOptions.fromConfig` now discovers app-load through the registry instead of resolving it against `jsonResourceRoot`.

## ApplicationResourceConfig field removal

Deleted entirely:

- `categoryCodeTablesPath` field, accessor, `withCategoryCodeTablesPath`, `resolveCategoryCodeTables`, `DEFAULT_CATEGORY_CODE_TABLES_PATH`
- `imageAssetRoot` field, accessor, `withImageAssetRoot`, `resolveImageAssetRoot`, `DEFAULT_IMAGE_ASSET_ROOT`
- `jsonResourceRoot` accessor, `withJsonResourceRoot`, `resolveJsonResourceRoot`, `JSON_RESOURCE_ROOT_ID`, `DEFAULT_JSON_RESOURCE_ROOT`
- The `ApplicationResourceConfig.of(String, String, Map)` / `of(boolean, String, String, Map)` / `of(boolean, ... 12 background args ..., Map)` factories
- The 13-arg private delegating constructor

The remaining shape is:

- `debug`, the nine background defaults
- `resources: Map<String, String>` for app-named generic overrides
- `resourceRoots: Map<ResourceCategory, List<String>>` for per-category roots

A small `of(Map<String, String> resources)` factory remains. Use `withResourceRoots(Map)` / `withAdditionalResourceRoot(category, spec)` to configure the registry-driven roots.

## Caller updates

- `BootstrapServiceTest.optionsWireResourceConfigModulesAndCustomImageRootIntoBootstrap` rebuilt: now constructs the config with `withAdditionalResourceRoot(IMAGES, "assets/images")` instead of the old `imageAssetRoot` factory argument. Image resolution comes through the registry.
- `BootstrapServiceTest.optionsBootLoadsJsonBackedDisplayContentModuleFromConfiguredResource` rewrites the inline `config.json` to use `resourceRoots.images` instead of `imageAssetRoot` / `categoryCodeTablesPath`.
- `BootstrapServiceTest.fromConfigLoadsApplicationJsonDirectoriesAtStartup` rewrites the inline `config.json` to use `resourceRoots.images` and `resourceRoots.support` so the test's `display/`, `scenes/`, `conversations/`, and `config/app-load.json` subtrees flow through the registry.
- `SceneRouterTest.routeContextThemedScenesUseConfiguredAppAndSpecializedBackgrounds` rebuilds the config through `withDefault...BackgroundColor` mutators (replacing the dropped 13-arg `of`).
- `ApplicationResourceConfigTest`: round-trip test no longer asserts legacy fields; `factoryRejectsBlankValues` now exercises the new `of(Map)` factory; `resourceRoots` tests retained.
- `DefaultDisplayValuesApplication.APPLICATION_CONFIG_RESOURCE` repointed from `/com/eb/javafx/bootstrap/config.json` (deleted in the foundation PR) to `/config.json` (the bundled library manifest). The legacy `switch` cases for `categoryCodeTablesPath` / `imageAssetRoot` / `resources.jsonResourceRoot` are intentionally left as dead branches; the existing `DefaultDisplayValuesApplicationTest` regressions reproduce on `origin/main` and are tracked separately.

## Config JSON files

- Repo-root `config.json`: removed the leftover `categoryCodeTablesPath`, `imageAssetRoot`, and `resources.jsonResourceRoot` entries since the runtime no longer reads them.
- `examples/resources/json/config/config.demo.json`: rewritten around `resourceRoots.images` and `resourceRoots.support` plus the named `displayDefinitions`/`sceneDefinitions`/`uiTheme` overrides. Paths are now resolved relative to the demo config's own directory (matching `BootstrapOptions.fromConfig`).

## Demo update

`examples/user-manual/04-startup-and-service-wiring/ApplicationResourceConfigDemo.java` rebuilt around `BootstrapOptions.fromConfig` and `ResourceRegistry.find(SUPPORT, ...)` instead of the removed `resolveCategoryCodeTables` / `resolveImageAssetRoot` / `resolveJsonResourceRoot` helpers.

## Deferred to a follow-up PR

- Remaining demos under `examples/user-manual/04-…/BootstrapDemo.java`, `examples/user-manual/07-…/DisplaySupportDemo.java`, `examples/user-manual/09-…/CategoryCodeTableDefinitionDemo.java` still reference legacy APIs and need their walkthroughs rewritten.
- `docs/USER_MANUAL.md` section 4 rewrite for the new `resourceRoots` configuration shape and the URL-based loader surface.
- `DefaultDisplayValuesApplication` legacy-key switch cleanup, plus updating `DefaultDisplayValuesApplicationTest` to match the new config schema (currently in the pre-existing failing set on main).

These items are documentation/example polish; the engine compile path and bootstrap+resources test surface is fully migrated in this PR.

## Validation

- `./gradlew --no-daemon compileJava testClasses` — green
- `./gradlew --no-daemon test --tests "com.eb.javafx.resources.*" --tests "com.eb.javafx.bootstrap.*" --tests "com.eb.javafx.scene.SceneDefinitionJsonTest" --tests "com.eb.javafx.scene.ConversationDefinitionJsonTest" --tests "com.eb.javafx.gamesupport.CategoryCodeTableDefinitionTest" --tests "com.eb.javafx.content.JsonDisplayContentModuleTest"` — green
- The 9 unrelated UI/JavaFX test failures (`DefaultDisplayValuesApplicationTest`, `PreferencesSummaryScreenTest`, `BlockBackgroundImageTestScreenTest`, `JsonBlockBackgroundImageTestScreenTest`) reproduce on `origin/main` without these changes.
