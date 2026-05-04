# User Manual

## 1. Contents summary

This manual explains how to use the existing `novlfx-engine` code as a reusable JavaFX foundation for visual novel projects.

Example/demo index: [`examples/user-manual/README.md`](../examples/user-manual/README.md)

- **Project setup and validation**: open the repository, build it, run tests, and launch the manual test screen.
- **Module and package layout**: understand the `com.novlfx.engine` module and exported `com.eb.javafx.*` packages.
- **Startup and service wiring**: initialize the engine with `BootstrapService` and use the resulting `BootContext`.
- **Content, routing, and scenes**: register static content, routes, scene definitions, choices, transitions, JSON-backed scene data, and resumable scene flow state.
- **UI screens and themes**: use reusable JavaFX screens, navigation helpers, startup error reporting, and CSS theme loading.
- **Display support**: define image assets, layers, transforms, layered characters, JSON-backed display definitions, interpolation, and animations.
- **Audio support**: validate channel-based sound requests and produce playback commands for application media adapters.
- **Game support services**: use reusable action, requirement, effect, game-clock, state, save/load, preference, and random utilities.
- **Text and utility helpers**: parse simple styled text tags and use common validation, collection, path, packaged font, JSON, and time helpers.
- **Extension boundaries**: keep authored game content, application launchers, concrete assets, and domain-specific rules in the application repository.
- **Application shell integration**: create the first app-owned JavaFX launcher, bootstrap flow, and media adapter on top of the reusable engine.

## 2. Project setup and validation

Open the repository root as a Gradle project. The project uses the checked-in Gradle wrapper, Java 17, and JavaFX 17 controls.

Validate the library with:

```bash
./gradlew --no-daemon build
```

This compiles the main module, compiles tests, and runs the JUnit test suite.

Launch the manual test screen with:

```bash
./gradlew --no-daemon runTestScreen
```

The test screen is supplied from the test source set and is useful for manually checking reusable UI surfaces without adding an application-specific entry point to the engine. Its window title reports the total discovered tests plus recorded success and error counts. Shell script examples run directly on macOS/Linux and on Windows when `bash.exe` is available on `PATH`.

Example/demo code: [`examples/user-manual/02-project-setup-and-validation/demo.sh`](../examples/user-manual/02-project-setup-and-validation/demo.sh)

## 3. Module and package layout

The engine publishes the Java module `com.novlfx.engine`. It exports reusable packages under `com.eb.javafx.*`:

- `audio`: channel definitions, sound requests, and validated playback commands.
- `bootstrap`: startup phases, bootstrap reporting, and boot context assembly.
- `content`: reusable static content module and registry contracts, including JSON-backed display content modules.
- `display`: image assets, display layers, transforms, layered characters, JSON definition loading, interpolation, and animation playback.
- `gamesupport`: generic action, requirement, effect, clock, date/time, and game-support registry behavior.
- `globalApi`: adapters for global-style navigation, screen visibility, randomness, and sound requests.
- `prefs`: persisted user preferences such as window size, fullscreen state, and master volume.
- `random`: deterministic and non-deterministic random helpers.
- `routing`: route descriptors, route modules, route factories, contexts, and scene router behavior.
- `save`: reusable save/load slot metadata and JSON persistence workflow.
- `scene`: scene definitions, JSON import/export, steps, choices, transitions, execution results, flow-state JSON snapshots, and view models.
- `state`: basic mutable game-state creation and snapshots.
- `text`: text tag parsing, text tokens, token types, and text style metadata.
- `ui`: reusable JavaFX screens, navigation shells, theme loading, and startup failure reporting.
- `util`: validation, immutable collection, result, path, packaged font resource, time, JSON string, and initialization helpers.

Use these packages from an application project instead of copying source-engine-specific global code into the engine.

Example/demo code: [`examples/user-manual/03-module-and-package-layout/ModuleUsageExample.java`](../examples/user-manual/03-module-and-package-layout/ModuleUsageExample.java)

## 4. Startup and service wiring

Use `BootstrapService` when you want the engine to create and initialize the standard reusable services in the correct order. The bootstrap process loads preferences, initializes save/load, random, audio, game-support, and theme services, registers static content and route modules, validates game rules, and creates runtime state.

For application integration, prefer `BootstrapOptions` with the options-based `BootstrapService` constructor. Options group the application root, `ApplicationResourceConfig`, static content modules, scene modules, and route modules. When created this way, bootstrap constructs its `ImageDisplayRegistry` from the configured image asset root and exposes both the application root and resource config from `BootContext`.

The main result is a `BootContext`. Use it to access initialized services such as:

- `PreferencesService`
- `SaveLoadService`
- `GameRandomService`
- `AudioService`
- `GameSupportService`
- `UiTheme`
- `ContentRegistry`
- `SceneRouter`
- `GameState`
- `GlobalApiAdapter`
- `SceneRegistry`
- `ImageDisplayRegistry`

Inspect the `BootstrapReport` when startup diagnostics matter. It records completed phases and phase messages so an application can display useful failure or progress information. Use `BootstrapDiagnostics.requireComplete(...)` when an application launcher should fail fast on incomplete startup, or use `BootstrapDiagnostics.viewModel(...)` / `phaseLines(...)` to render content-neutral startup summaries in an app-owned startup or error screen.

Use `BootstrapCompletenessPolicy` when an application has additional reusable startup requirements beyond all phases completing. A policy can require specific `BootstrapPhase` values, route IDs, and content definition IDs, then produce a `BootstrapCompletenessReport` for startup UI or throw through `requireComplete(...)`. `ApplicationShellSupport` combines that policy check with content-neutral startup-route opening and optional window-size preference persistence; the application still owns the JavaFX `Application` subclass and concrete route modules.

Do not use guarded services before initialization. Several services use initialization guards and will fail fast if called before bootstrap or explicit initialization.

Applications can also keep an external `config.json` and load it with `ApplicationResourceConfig.load(Path)` when authored resources need to live outside the engine defaults. The config stores a category code-table JSON path, an image asset root, and a generic `resources` map for other overrideable files such as themes or image groups:

```json
{
  "categoryCodeTablesPath": "config/category-code-tables.en.json",
  "imageAssetRoot": "game",
  "resources": {
    "sceneDefinitions": "content/scene-definitions.json",
    "displayDefinitions": "content/display-definitions.json",
    "uiTheme": "src/main/resources/com/eb/javafx/ui/eb.css",
    "backgrounds": "assets/backgrounds"
  }
}
```

Resolve those paths relative to an application-chosen base directory:

- `resolveCategoryCodeTables(baseDir)` returns the authored category JSON file to pass into `CategoryCodeTableDefinition.load(...)`.
- `resolveImageAssetRoot(baseDir)` returns the image root used by options-based bootstrap or to pass into `new ImageDisplayRegistry(repoRoot, imageAssetRoot)`.
- `resolveResource(baseDir, "sceneDefinitions")`, `resolveResource(baseDir, "displayDefinitions")`, or other named resource IDs resolve override points that the application owns.

```java
BootstrapOptions options = BootstrapOptions.fromConfig(appRoot.resolve("config.json"))
        .withStaticContentModules(staticModules)
        .withSceneModules(sceneModules)
        .withRouteModules(routeModules);
BootContext context = new BootstrapService(options).boot(primaryStage);
```

Use `context.resourceConfig().resolveCategoryCodeTables(context.applicationRoot())` when app-owned content modules need to load generic category JSON during startup.
Use `context.resourceConfig().resolveResource(context.applicationRoot(), "displayDefinitions")` with `JsonDisplayContentModule` when app-owned display definitions should be loaded during the static content phase, and use named resources such as `sceneDefinitions` to load JSON-authored scene modules from the application side.

Example/demo code: [`examples/user-manual/04-startup-and-service-wiring/BootstrapDemo.java`](../examples/user-manual/04-startup-and-service-wiring/BootstrapDemo.java)

Additional example/demo code:
- [`examples/user-manual/04-startup-and-service-wiring/ApplicationResourceConfigDemo.java`](../examples/user-manual/04-startup-and-service-wiring/ApplicationResourceConfigDemo.java)
- [`examples/user-manual/04-startup-and-service-wiring/config.demo.json`](../examples/user-manual/04-startup-and-service-wiring/config.demo.json)

## 5. Content, routing, and scenes

Example/demo code: [`examples/user-manual/05-content-routing-and-scenes/SceneFlowDemo.java`](../examples/user-manual/05-content-routing-and-scenes/SceneFlowDemo.java)

Additional example/demo code:
- [`examples/user-manual/05-content-routing-and-scenes/SceneExecutionAndJsonDemo.java`](../examples/user-manual/05-content-routing-and-scenes/SceneExecutionAndJsonDemo.java)
- [`examples/user-manual/05-content-routing-and-scenes/scene-definitions.demo.json`](../examples/user-manual/05-content-routing-and-scenes/scene-definitions.demo.json)

### Static content

Use `ContentRegistry` with content modules when a game needs reusable static definitions registered before runtime state exists. `StaticContentModule` is the extension point for application-provided static content, while `EnginePlaceholderContentModule` supplies reusable placeholder content for the engine/test environment.

Keep authored story content and game-specific data outside this repository. Register that content from the application repository through module interfaces.

### Routes

Use `SceneRouter` to register `RouteModule` implementations and navigate by route ID. A route is described with `RouteDescriptor` metadata and constructed through a `RouteFactory` using a `RouteContext`.

Routes are grouped by `RouteCategory`, which helps UI code distinguish menus, gameplay routes, diagnostics, and other navigation surfaces. `DefaultRouteModule` provides reusable route factories backed by UI-neutral view models for the engine.

### Scene definitions and execution

Use `SceneRegistry` to register `SceneModule` implementations. A scene is described by `SceneDefinition` and consists of ordered `SceneStep` instances. Supported scene flow concepts include:

- dialogue and narration-style steps
- action steps
- choices through `SceneChoice`
- transitions through `SceneTransition`
- transition types such as jump, call, return, complete, and route-style transitions
- resumable state through `SceneFlowState` and `SceneReturnPoint`

Use `SceneExecutor` to execute scene flow and return a `SceneExecutionResult`. Use `ScenePresenter` and view-model classes when JavaFX UI code needs a UI-neutral representation of the current scene and choices.

Use `SceneRegistry.validationReport(...)` when application startup needs diagnostics rather than fail-fast validation. The report contains `SceneGraphSummary` entries plus `SceneValidationProblem` diagnostics for duplicate IDs, missing jump/call targets, unreachable steps, and final-step `NEXT` warnings. Applications can pass `SceneReferenceValidator` instances such as `SceneReferenceValidators.knownSpeakers(...)` or `knownDisplayReferences(...)` to validate app-owned speaker/display IDs without moving those registries into the engine.

### Scene presentation view models

Scene view models are deliberately UI-neutral. They are useful when the engine needs to expose scene execution state to JavaFX screens, tests, debug panels, or application-owned renderers without passing mutable executor objects or JavaFX controls across package boundaries. Use `ScenePresenter` to convert a `SceneExecutionResult` into these models after each scene execution step or choice selection.

- `SceneViewModel` is the top-level scene presentation state. It carries the current execution status, scene id, step id, speaker id, text definition id, display reference, choices, selected choice history, message, dialogue rows, status rows, and effect previews. Use it as the single object handed to a scene renderer such as `SceneFlowView` so the renderer can display scene progress without knowing how scene execution works.
- `SceneChoiceViewModel` represents one rendered choice after availability has already been evaluated. It includes the choice id, choice text definition id, whether the choice is available, an optional disabled reason, whether it was selected earlier, string metadata, and effect previews. Use it to build choice buttons or test choice state without re-running requirement checks in the UI layer.
- `SceneDialogueRowViewModel` represents one dialogue or narration row. It includes the step type, optional speaker id, text definition id, and optional display reference. Use it when a renderer needs a normalized row for dialogue panels instead of inspecting raw `SceneStep` objects.
- `SceneStatusRowViewModel` represents one label/value diagnostic row such as status, active scene, active step, selected choices, pending interruption, or message. Use it for HUD, debug, and manual-test surfaces where scene execution state should be visible in a consistent format.
- `SceneEffectPreviewViewModel` represents preview-only metadata for a scene step or choice. It contains a label and value derived from `preview.*` metadata or a fallback reference. Use it when a prototype UI wants to show what display, effect, or authored metadata would be applied before an application supplies custom rendering.

Use `SceneDefinitionJson` for simple JSON-authored scenes that do not require executable Java `ActionRequirement` or `ActionEffect` instances. JSON scenes can include dialogue/narration text definition IDs, choices, transitions, display references, and string metadata. Register more complex requirements/effects through Java scene modules. In an application, keep the scene JSON path in `ApplicationResourceConfig.resources()` and resolve it with `resolveResource(applicationRoot, "sceneDefinitions")` before loading.

Use `SceneFlowStateJson` to serialize and restore `SceneFlowState` snapshots containing the active scene, step index, call stack, selected choice IDs, and pending UI interruption marker. For future save/load payloads, `SceneFlowStateJson.toSnapshotSection(...)` wraps that JSON as a versioned `SaveSnapshotSection` named `sceneFlowState`, and `fromSnapshotSection(...)` validates the section id and version before loading it.

Use `SceneFlowSnapshotDocuments` when an application save workflow wants the reusable scene-flow section composed into a `SaveSnapshotDocument` alongside app-owned sections. The helper registers the `sceneFlowState` section as required and restores the scene state while leaving the outer save schema under application control.

## 6. UI screens and themes

The `ui` package provides reusable JavaFX surfaces and helpers:

- `ScreenViewModel` describes reusable route screen content without JavaFX control state. It contains the screen title, informational body lines, and route actions. Use it for menu, summary, diagnostics, and placeholder screens when the content is mostly text plus navigation and should be easy to test before JavaFX controls are created.
- `ScreenActionViewModel` describes one route-backed action for a `ScreenViewModel`. It contains the button label, destination route id, and enabled state. Use it when reusable screens need navigation buttons that can be enabled or disabled without coupling the screen model to JavaFX `Button` setup.
- `PreferencesSummaryViewModel` describes the reusable preferences summary screen with typed rows instead of raw strings. Each `PreferencesSummaryRowViewModel` contains a label/value pair such as window size, HUD alpha, input mode, or master volume, which keeps preference summaries easy to extend and test before flattening them into generic screen lines.
- `SaveLoadSummaryViewModel` describes the reusable save/load summary screen with explicit schema metadata fields. It keeps the schema version, save directory, informational note, and actions as named data so save/load diagnostics can evolve without parsing or rebuilding display strings.
- `HudSummaryViewModel` describes the reusable HUD summary screen with a small dedicated model. It currently carries the HUD layer description, opacity, and actions and gives the HUD summary room to grow beyond a couple of text lines without falling back to ad hoc strings.
- `HudStatusContainerViewModel`, `HudStatusGroupViewModel`, and `HudStatusRowViewModel` describe reusable read-only HUD/status overlays with visibility, opacity, stack order, groups, and rows. Applications supply game-specific status values and visibility rules.
- `SnapshotSectionPreviewViewModel` describes one preview row for a `SaveSnapshotSection`. It contains the section id, schema version, and a shortened JSON payload summary. Use it when save/load diagnostics or future save browsers need to show the contents of composed save snapshots without exposing the full payload or requiring custom parsing in the UI.
- `ScreenInventory`, `ScreenInventoryItem`, `ScreenInventorySource`, `ScreenInventoryScanner`, and `ScreenInventoryAssignmentCategory` provide content-neutral inventory models for application-owned screen/style/control migration scanners. Use them to classify source artifacts as route-backed, reusable-control-backed, deferred, deprecated, excluded, or app-owned without hard-coding source-engine names in the engine.
- `ViewModelScreen` renders a `ScreenViewModel` with generic labels and navigation buttons.
- `ScreenShell` wraps screen content in a consistent shell.
- `ScreenNavigation` centralizes navigation callbacks.
- `MainMenuScreen`, `SceneFlowScreen`, `DisplayBindingsScreen`, `HudSummaryScreen`, `SaveLoadSummaryScreen`, and `PreferencesSummaryScreen` provide generic reusable screens or screen models.
- `CaptureTestScreen` supports test/manual capture workflows.
- `UiTheme` loads the reusable stylesheet from `src/main/resources/com/eb/javafx/ui/eb.css`.
- `StartupErrorReporter`, `StartupFailureException`, and `StartupFailureCategory` provide structured startup diagnostics.

Applications can use these screens directly for prototypes or replace route factories with application-specific screens while keeping the same routing and bootstrap contracts. App-specific JavaFX controls should live in application route modules; reusable engine screens should consume view models or generic display contracts.

Example/demo code: [`examples/user-manual/06-ui-screens-and-themes/UiScreenDemo.java`](../examples/user-manual/06-ui-screens-and-themes/UiScreenDemo.java)

Additional example/demo code:
- [`examples/user-manual/06-ui-screens-and-themes/UiScreenCatalogDemo.java`](../examples/user-manual/06-ui-screens-and-themes/UiScreenCatalogDemo.java)

## 7. Display support

Use `ImageDisplayRegistry` as the central registry for reusable visual definitions. It can register and resolve:

- `ImageAssetDefinition` objects for authored image paths and display metadata
- `DisplayLayer` values for render ordering
- `DisplayTransform` values for placement, scale, opacity, and other reusable transform data
- `LayeredCharacterDefinition` values for composed character displays
- `DisplayAnimation` and `DisplayAnimationStep` definitions

Use `DisplayAnimationPlayer` to model animation playback state and interpolation. `DisplayInterpolation` identifies supported interpolation behavior.

The registry can resolve image paths from a checked-out game tree through `GameAssetLocator`, but concrete image assets remain application-owned.

Use `DisplayDefinitionJsonLoader` to load app-owned display JSON into an `ImageDisplayRegistry`, or wrap that loading in `JsonDisplayContentModule` for bootstrap registration. The supported root fields are `transforms`, `images`, and `layeredCharacters`; authored image files and IDs remain outside the engine. Applications can store this JSON path under a named `ApplicationResourceConfig` resource such as `displayDefinitions`.

Example/demo code: [`examples/user-manual/07-display-support/display-definitions.demo.json`](../examples/user-manual/07-display-support/display-definitions.demo.json)

Additional example/demo code:
- [`examples/user-manual/07-display-support/DisplaySupportDemo.java`](../examples/user-manual/07-display-support/DisplaySupportDemo.java)

## 8. Audio support

Use `AudioService` for the current reusable audio boundary. It registers default channels for music, short sounds, reusable effects, and intimate/effect-style channels. It validates `SoundRequest` instances and produces `AudioPlaybackCommand` objects that an application-owned `AudioPlaybackAdapter` can bind to JavaFX media classes.

Audio support currently includes:

- named channel registration through `AudioChannelDefinition`
- per-channel looping capability
- simultaneous sound capacity metadata
- default and mutable channel volume
- master volume from `PreferencesService`
- mute state
- effective volume calculation
- tracking the last playback command per channel
- stop tracking for a channel

`AudioAdapterPolicy`, `AudioAssetResolver`, `AudioFadeRequest`, `AudioCrossfadeRequest`, and `AudioPlaybackLifecycleEvent` document reusable expectations for concrete adapters: app-owned asset lookup, channel lifecycle events, fades, crossfades, optional preloading, and channel-specific player pool sizing. `JavaFxAudioPlaybackAdapter` is the opt-in JavaFX media bridge: applications provide the resolver that maps authored paths to concrete media URIs, and the adapter creates `MediaPlayer` instances for looping commands plus pooled `AudioClip` instances for non-looping commands.

Concrete media files and path resolution remain application-owned. Applications can use `JavaFxAudioPlaybackAdapter` directly or provide their own `AudioPlaybackAdapter` when they need custom player lifecycle, platform handling, or richer fade/crossfade behavior.

Example/demo code: [`examples/user-manual/08-audio-support/AudioServiceDemo.java`](../examples/user-manual/08-audio-support/AudioServiceDemo.java)

## 9. Game support, state, saves, preferences, and random behavior

### Game support

Use `GameSupportService` and `ActionRegistry` to register reusable `GameAction` objects. Actions can use `ActionRequirement` checks, `ActionEffect` outcomes, `ActionContext`, `RequirementResult`, and `ActionResult` to keep generic game-rule plumbing separate from authored domain rules.

Use `CodeTableDefinition` and `CodeDefinition` to define project-supplied code lists such as time slots, roles, goals, postures, positions, duties, or listener types. `GameClock` and `GameDateTime` use a time-slot code table so reusable time progression does not embed a specific game calendar or schedule.

Use `CategoryCodeTableDefinition.load(Path)` when category data should come from authored JSON, typically with the path returned by `ApplicationResourceConfig.resolveCategoryCodeTables(applicationRoot)`. The root object contains a `language` field and a `tables` array; titles in that file are interpreted as text for that language so applications can provide parallel files for later translation:

```json
{
  "language": "en",
  "tables": [
    {
      "id": "roles",
      "title": "Roles",
      "codes": [
        {
          "id": "manager",
          "title": "Manager",
          "sortOrder": 20,
          "tags": ["work"]
        }
      ]
    }
  ]
}
```

Use the immutable helper methods to work with authored category files without hand-editing registry objects in memory:

- `CategoryCodeTableDefinition.load(path)` loads a language-specific file.
- `addTable(...)`, `editTable(...)`, and `removeTable(...)` return updated category sets.
- `CodeTableDefinition.withTitle(...)`, `addCode(...)`, `editCode(...)`, and `removeCode(...)` return updated tables.
- `save(path)` writes the updated category set back to JSON, and `toJson()` returns the same formatted JSON string for previews or tests.

### State

Use `GameStateFactory` to create base `GameState` instances. Keep project-specific state fields and schemas in the application repository unless they are represented by reusable engine abstractions.

### Save/load

Use `SaveLoadService` for reusable save-slot workflows. It supports slot summaries and JSON persistence behavior suitable for engine-level tests and extension by application code. `SaveLoadSummaryScreen` and `SaveLoadSummaryViewModel` expose the current save schema version and configured save directory as reusable diagnostic UI data. Use `SaveSnapshotCodec` and `SaveSnapshotSection` when an application wants to compose engine-owned state slices, such as scene-flow progress, into its own save document; the application still owns the outer save schema and any project-specific state fields.

### Preferences

Use `PreferencesService` for user preferences such as window size, fullscreen state, and master volume. Load preferences before services that depend on them, especially UI theme/window behavior and audio master volume. `PreferencesSummaryScreen` builds a `PreferencesSummaryViewModel` with `PreferencesSummaryRowViewModel` entries so reusable diagnostics can present startup preference state as labeled values instead of raw strings.

### Random

Use `GameRandomService` for reusable random behavior. Initialize it before use so tests and application code can rely on deterministic service state where applicable.

Example/demo code: [`examples/user-manual/09-game-support-state-save-prefs-random/SupportServicesDemo.java`](../examples/user-manual/09-game-support-state-save-prefs-random/SupportServicesDemo.java)

Additional example/demo code:
- [`examples/user-manual/09-game-support-state-save-prefs-random/CategoryCodeTableDefinitionDemo.java`](../examples/user-manual/09-game-support-state-save-prefs-random/CategoryCodeTableDefinitionDemo.java)
- [`examples/user-manual/09-game-support-state-save-prefs-random/category-code-tables.demo.json`](../examples/user-manual/09-game-support-state-save-prefs-random/category-code-tables.demo.json)

## 10. Text and utility helpers

Use `TextTagParser` to tokenize visual-novel-style text with simple styling metadata. Parsed output is represented through `TextToken`, `TextTokenType`, and `TextStyle`.

Use `TextEffect` and `StyledTextSpan` to carry rendering-neutral rich-text metadata such as gradient, kinetic, or glitch parameters before a JavaFX renderer exists. Use `TextTemplateProcessor` with a `TextVariableResolver` for simple app-supplied `{variable}` replacement while preserving unknown markers.

Use utility classes for common engine behavior:

- `Validation` for null, blank, positive, and unit-interval checks.
- `ImmutableCollections` for defensive immutable collection handling.
- `Result` for success/failure-style return values.
- `PathUtils` for repository-relative and asset-path helpers.
- `FontResources` for packaged engine fonts under `/com/eb/javafx/fonts`, including font filename discovery, validated resource paths, resource URLs/streams, and JavaFX `Font` loading.
- `TimeFormatting` for reusable time display formatting.
- `JsonStrings` for JSON string escaping.
- `InitializationGuard` for fail-fast service initialization checks.
- `UtilJavaFx.run(Runnable)` for executing work immediately on the JavaFX application thread or scheduling it with `Platform.runLater(...)` from a background thread.
- `VectorImage` for reusable SVG loading, metadata, sizing, styling, transform, export, and sanitization helpers.

Prefer these helpers over duplicating validation and formatting logic in application code.

When application code needs one of the engine-bundled fonts, prefer `FontResources` over hard-coded classpath strings. It validates known font filenames and centralizes lookups for resources now packaged in `src/main/resources/com/eb/javafx/fonts`.

Example/demo code: [`examples/user-manual/10-text-and-utility-helpers/TextAndUtilityDemo.java`](../examples/user-manual/10-text-and-utility-helpers/TextAndUtilityDemo.java)

## 11. Extension boundaries

This repository is intended to stay reusable. Keep the following in application repositories rather than in `novlfx-engine`:

- application entry points and launcher classes
- authored scenes, dialogue, characters, locations, and routes
- concrete image, audio, and game assets
- source-game scripts and migration metadata
- project-specific save schemas
- domain-specific progression rules that are not expressed as generic interfaces or extension points

When adding new reusable behavior, preserve deterministic validation, initialize services explicitly, and add focused tests for the reusable behavior.

Example/demo code: [`examples/user-manual/11-extension-boundaries/ApplicationRouteModuleDemo.java`](../examples/user-manual/11-extension-boundaries/ApplicationRouteModuleDemo.java)

## 12. Application shell integration

Once the reusable engine layer is in place, the next step is to build an application repository that depends on `novlfx-engine`. The engine already owns the reusable JavaFX foundation; the application should own the launcher, authored content, concrete assets, and project-specific screens.

Build the application shell in this order:

1. Add an application-owned `GameApplication` class that extends `javafx.application.Application`.
2. In `start(Stage primaryStage)`, load authored resource paths with `BootstrapOptions.fromConfig(...)`, then add application static content modules, scene modules, and route modules before calling `new BootstrapService(options).boot(primaryStage)`.
3. Resolve application-owned JSON and asset roots through `ApplicationResourceConfig` so authored scene definitions, display definitions, category tables, and image/audio assets stay outside the engine repository.
4. Implement a concrete `AudioPlaybackAdapter` that turns validated `AudioPlaybackCommand` objects into real `MediaPlayer` or `AudioClip` playback, using `AudioAdapterPolicy` to document asset lookup, fade/crossfade, preload, and pool behavior.
5. Start with reusable engine screens where they fit, then replace individual routes with application-specific JavaFX screens as the game UI becomes concrete.

Keep this repository focused on reusable behavior. The launcher class, authored resources, and concrete media binding should remain application-owned even when they directly use engine APIs.

If a later cleanup goal is a stricter pure-JavaFX stack, treat removal of the remaining Swing/AWT image bridge utilities as a follow-up task after the application shell is running.

Example/demo code:
- [`examples/user-manual/12-application-shell/GameApplicationDemo.java`](../examples/user-manual/12-application-shell/GameApplicationDemo.java)
- [`examples/user-manual/12-application-shell/JavaFxAudioPlaybackAdapterDemo.java`](../examples/user-manual/12-application-shell/JavaFxAudioPlaybackAdapterDemo.java)
