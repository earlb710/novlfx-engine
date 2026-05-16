# User Manual

## 1. Contents summary

This manual explains how to use the existing `novlfx-engine` code as a reusable JavaFX foundation for visual novel projects.

Example/demo index: [`examples/user-manual/README.md`](../examples/user-manual/README.md)

- **Project setup and validation**: open the repository, build it, run tests, and launch the manual test screen.
- **Module and package layout**: understand the `com.novlfx.engine` module and exported `com.eb.javafx.*` packages.
- **Startup and service wiring**: initialize the engine with `BootstrapService` and use the resulting `BootContext`.
- **Content, routing, and scenes**: register static content, routes, scene definitions, choices, transitions, JSON-backed scene data, and resumable scene flow state. Includes skip/auto modes, dialogue rollback, conditional step branching, conditional choice visibility, timed choices, loop menus, NVL display mode, visual transitions, image tag resolution, and gallery framework.
- **UI screens and themes**: use reusable JavaFX screens, navigation helpers, startup error reporting, and CSS theme loading.
- **Display support**: define image assets, layers, transforms, layered characters, JSON-backed display definitions, interpolation, and animations.
- **Audio support**: validate channel-based sound requests and produce playback commands for application media adapters.
- **Game support services**: use reusable action, requirement, effect, game-clock, location, localization, asset, input, event, progress, inventory, character, journal, diagnostics, settings, accessibility, timeline, debug, state, save/load, preference, and random utilities.
- **Text and utility helpers**: parse simple styled text tags, record conversation history, and use common validation, collection, path, packaged font, JSON, image, conversion, Unicode, and time helpers.
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

Launch the manual screen designer with:

```bash
./gradlew --no-daemon runScreenDesigner
```

The screen designer edits JSON-backed `ScreenDesignModel` documents with stable editable screen, block, and item ids. Saved items are written to JSON, while temporary preview/test items can be added to a block by id for validation without being included in normal save output.

Launch the management app with:

```bash
./gradlew --no-daemon runManagementApp
```

The management app includes a shared **Working Directory** picker above the launcher buttons and remembers the last chosen folder between launches. That working directory is passed into the manual authoring and diagnostic screens so file pickers, relative screen resources, and related browsing actions start from the same folder, including the screen designer, reloadable JSON screen, conversation editor, code table manager, and file catalog. The **Default App Values** screen also includes a **Locations** tab for reviewing and formatting bundled location JSON examples while comparing them with other startup defaults.

Launch the dialog block demo with:

```bash
./gradlew --no-daemon runDialogBlockTestScreen
```

The dialog block demo opens a `MAIN_APP_LAYOUT` window where the dialog slot is a
`DialogEntriesView` pre-seeded with **ten short conversations** between five distinct characters
(narrator, MC, book girl, random girl, and an old mentor). Each character has its own role colour,
so the demo exercises the column-aligned speaker layout, per-role text tinting, the 50% black
dialog background, and the right-hand scrollbar. Left-click the dialog block to advance the
cursor (fading one more line); right-click to rewind; <kbd>Space</kbd> / <kbd>Backspace</kbd>
mirror the same actions. Use the scrollbar or mouse wheel to browse the full conversation history.

The same demo is also discoverable from the manual **Test Screen** (`runTestScreen`) as a
standalone example: open the test screen and look for
`06-ui-screens-and-themes/DialogBlockDemo.java` under the example tree, then hit **Run** to launch
it in a child JVM. The shim at
[`examples/user-manual/06-ui-screens-and-themes/DialogBlockDemo.java`](../examples/user-manual/06-ui-screens-and-themes/DialogBlockDemo.java)
delegates to `DialogBlockTestScreenApplication.main(...)` so the discoverable example and the
Gradle task share one implementation.

Launch the reusable error screen demo with:

```bash
./gradlew --no-daemon runErrorScreenTestScreen
```

The demo seeds an `IllegalStateException` and renders it through the reusable
[`ErrorScreen`](#error-screen-error-and-exception-surface) — the heading shows the exception
class name, the read-only monospace text area below shows the full stack trace (selectable for
`Ctrl+C` or one-click via **Copy details**), and the button row exposes both **Continue**
(recoverable-failure flow) and **Exit**. The same demo is discoverable from the manual test
screen as `06-ui-screens-and-themes/ErrorScreenDemo.java`.

Launch the manual conversation editor with:

```bash
./gradlew --no-daemon runConversationEditor
```

The conversation editor edits JSON-backed `ConversationDefinition` documents using the same exported conversation shape as AltLife: top-level `name`, `language`, and `conversations`; each conversation has `id`, `description`, `lines`, and line `variants`.

Each line has `speaker`, `listener`, optional `type`, and one or more `variants`. If `type` is omitted, it defaults to `say`. Supported line types are:

- `say`: normal dialogue text.
- `shout`: converts the selected variant text to uppercase, escapes authored text, and wraps it in `<b></b>`.
- `whisper`: converts the selected variant text to lowercase, escapes authored text, and wraps it in `<i></i>`.
- `choice`: treats each variant as a player-selectable choice. Each choice variant can carry its own `value`; if `value` is empty or omitted, the runtime projection uses the zero-based variant index as the choice value. A variant can also carry its own `conditions` array; the editor builds each condition from a condition type such as `context` or `time of day`, the `=` operand, and a selected value, then stores it as a compact string such as `context=has_key` or `time of day=evening`. The runtime projection stores the choice value and those conditions with the generated scene choice metadata.

Condition values can reference fixed conversation variables with `$name` or `${name}` syntax. Use `${name}` when adding suffix text immediately after the variable. Malformed or unknown `$` variables are rejected when JSON is loaded. Supported variables are:

- `conversation.id`, `conversation.name`, `conversation.language`
- `line.speaker`, `line.listener`, `line.type`
- `variant.text`, `variant.value`, `variant.weight`, `variant.tooltipText`
- `choice.text`, `choice.value`, `choice.tooltipText`

Use `variant.*` when referring to the authored JSON variant fields. Use `choice.*` when referring to the generated runtime choice values for `choice` lines.

Application code can also declare additional variable names in a reusable text variable catalog before loading conversation JSON. The catalog is not conversation-specific and can be reused anywhere else that wants declared variable names plus a resolver. A game can keep a JSON catalog such as:

```json
{
  "variables": [
    {"name": "money", "valueType": "number"},
    {"name": "player.name", "valueType": "string"}
  ]
}
```

Anything not declared in that catalog is treated as an error. For example, a game can declare `money` so authored JSON may use `$money` or `${money}` even if the backing `mc.money` field is only provided by the game later. Load the catalog with `TextVariableCatalog.load(...)`, attach a `TextVariableResolver` with `withResolver(...)`, wrap it in `ConversationConditionVariables.catalog(...)`, then pass that to `ConversationDefinitionJson.load(...)` or `fromJson(...)`. Pass the same `ConversationConditionVariables` instance to `JsonConversationContentModule` when condition variable references should be replaced through the supplied lookup handler during scene projection.

Example choice line:

```json
{
  "speaker": "guide",
  "listener": "",
  "type": "choice",
  "variants": [
    {"text": "Take the left path.", "value": "left", "weight": 1.0, "conditions": ["context=has_key", "time of day=evening"]},
    {"text": "Take the right path.", "value": "", "weight": 1.0, "conditions": []}
  ]
}
```

Example/demo code: [`examples/user-manual/02-project-setup-and-validation/demo.sh`](../examples/user-manual/02-project-setup-and-validation/demo.sh)

## 3. Module and package layout

The engine publishes the Java module `com.novlfx.engine`. It exports reusable packages under `com.eb.javafx.*`:

- `accessibility`: reusable accessibility profile choices such as font scale, contrast, motion, captions, and screen-reader label support.
- `audio`: channel definitions, sound requests, and validated playback commands.
- `assets`: application-owned asset catalogs, preload hints, and existence/path validation reports.
- `bootstrap`: startup phases, bootstrap reporting, and boot context assembly.
- `characters`: generic character profiles, templates, stat blocks, mutable character state, tags, metadata, and relationship values.
- `content`: reusable static content module and registry contracts, content-pack descriptors, and JSON-backed display content modules.
- `debug`: reusable debug snapshot, panel descriptor, and inspector models for app-owned developer tools, plus `DebugScreenInspector` which installs a Ctrl+D shortcut that opens a copyable info dialog (route ID, screen class, JSON source) when the active `ApplicationResourceConfig.debug()` flag is enabled.
- `diagnostics`: structured health-check problems, reports, check descriptors, and check registries.
- `display`: image assets, display layers, transforms with render-order zorder, layered characters, layered image composition, JSON definition loading, interpolation, animation playback, and animation block grouping with event trigger support.
- `events`: lightweight runtime event bus, event queues, listeners, command dispatch, and event history models.
- `gamesupport`: generic action, requirement, effect, descriptor registry, clock, date/time, time scheduling, location, movement, and game-support registry behavior.
- `globalApi`: adapters for global-style navigation, screen visibility, randomness, and sound requests.
- `input`: action definitions, device triggers, bindings, and context-aware input maps.
- `inventory`: generic item definitions, catalogs, quantity state, wearable slots, outfits, and wardrobe state.
- `journal`: generic journal/quest/log definitions and read/unread state.
- `localization`: language-specific text bundles, language selection, lookup, and missing-text diagnostics.
- `messages`: generic notification and message-thread state models.
- `organizations`: generic organization descriptors, resource ledgers, and production queues.
- `prefs`: persisted user preferences such as window size, fullscreen state, and master volume.
- `progress`: reusable flags, counters, milestones, unlocks, game-support bridges, and save snapshot codec.
- `random`: deterministic and non-deterministic random helpers.
- `routing`: route descriptors, route modules, route factories, contexts, and scene router behavior.
- `save`: reusable save/load slot metadata and JSON persistence workflow.
- `scene`: scene definitions, JSON import/export, steps, choices, transitions, hotspot map definitions and registries, execution results, flow-state JSON snapshots, and view models including hotspot map and talking animation cues.
- `settings`: higher-level game setting definitions and runtime value store above raw preferences.
- `state`: basic mutable game-state creation, startup-route state, and conversation-history state.
- `text`: text tag parsing, text tokens, token types, text style metadata, template interpolation, rendering-neutral text effects/spans, and dialog history models.
- `timeline`: generic sequence, timed-step, and playback primitives for UI/display/text/audio timing.
- `ui`: reusable JavaFX screens, navigation shells, screen/background presentation models, preview helpers, theme loading, and startup failure reporting.
- `util`: validation, immutable collection, import validation, result, path, packaged font resource, time, JSON string/parsing, image/SVG, conversion, Unicode/string, and initialization helpers.

Use these packages from an application project instead of copying source-engine-specific global code into the engine.

Example/demo code: [`examples/user-manual/03-module-and-package-layout/ModuleUsageExample.java`](../examples/user-manual/03-module-and-package-layout/ModuleUsageExample.java)

## 4. Startup and service wiring

Use `BootstrapService` when you want the engine to create and initialize the standard reusable services in the correct order. The bootstrap process loads preferences, initializes save/load, random, audio, game-support, and theme services, registers static content and route modules, validates game rules, and creates runtime state.

For application integration, prefer `BootstrapOptions` with the options-based `BootstrapService` constructor. Options group the application root, `ApplicationResourceConfig`, the layered `ResourceRegistry`, static content modules, scene modules, and route modules. When created this way, bootstrap constructs its `ImageDisplayRegistry` from the registry's `IMAGES` category and exposes the application root, resource config, and registry from `BootContext`.

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

Use `GlobalApiAdapter` when legacy or app-owned code needs a narrow bridge for common global-style operations. It accepts `GlobalRouteRequest` / `GlobalRouteAction` navigation requests, delegates screen visibility to the router/stage layer, delegates sound requests to `AudioService`, and uses `GameRandomService` for reusable random decisions without reintroducing application globals.

Inspect the `BootstrapReport` when startup diagnostics matter. It records completed phases and phase messages so an application can display useful failure or progress information. Use `BootstrapDiagnostics.requireComplete(...)` when an application launcher should fail fast on incomplete startup, or use `BootstrapDiagnostics.viewModel(...)` / `phaseLines(...)` to render content-neutral startup summaries in an app-owned startup or error screen.

Use `BootstrapCompletenessPolicy` when an application has additional reusable startup requirements beyond all phases completing. A policy can require specific `BootstrapPhase` values, route IDs, and content definition IDs, then produce a `BootstrapCompletenessReport` with `BootstrapCompletenessProblem` entries for startup UI or throw through `requireComplete(...)`. `BootstrapDiagnostics.viewModel(...)` returns a `BootstrapDiagnosticsViewModel` with `BootstrapPhaseSummaryViewModel` rows for rendering startup phase status. `ApplicationShellOptions` configures startup route and window-preference behavior, while `ApplicationShellSupport` combines that policy check with content-neutral startup-route opening and optional window-size preference persistence; the application still owns the JavaFX `Application` subclass and concrete route modules.

Do not use guarded services before initialization. Several services use initialization guards and will fail fast if called before bootstrap or explicit initialization.

### Library resource manifest and `ResourceRegistry`

The engine ships a `config.json` at the repository root (also bundled into the JAR at `/config.json`). It declares the library's bundled resource directories under a `resourceRoots` map, keyed by `ResourceCategory`:

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

`BootstrapOptions.fromConfig(...)` and `BootstrapOptions.defaults()` build a `ResourceRegistry` before any bootstrap phase runs. The library's bundled `config.json` supplies the library roots; the application's `config.json` (passed to `fromConfig(...)`) supplies the application roots. Application roots are walked first, so an application file at the same relative name wins over the library file.

Each root spec can be either a filesystem path (resolved against the application root) or a classpath path prefixed with `classpath:`. Filesystem and classpath roots work the same when the library ships as a JAR — the registry uses `FileSystems.newFileSystem` for `jar:` URLs so bundled fonts and screen JSON remain discoverable.

Lookups are strictly **category-isolated**: a missing image is not found by falling through to `ui` or any other category. Recursive walks cover all subdirectories so nested files like `ui/screens/main-menu.json` are indexed under their relative name.

Access the registry from `BootContext.resourceRegistry()` and look up files with `registry.find(ResourceCategory.UI, "screens/main-menu.json")` or enumerate all files in a category with `registry.list(ResourceCategory.FONTS)`.

### Application `config.json`

Applications can keep an external `config.json` at their working directory and load it with `BootstrapOptions.fromConfig(Path)`. The schema declares per-category resource roots that layer over the engine's bundled roots, optional default background values for the app/preferences/save-load screens, and a generic `resources` map of named overrides:

```json
{
  "debug": false,
  "resourceRoots": {
    "images":  ["assets/images"],
    "support": ["resources/json"],
    "ui":      ["resources/json/screens"]
  },
  "defaultAppBackgroundColor": "#101820",
  "defaultAppBackgroundImage": "/com/eb/javafx/images/svg/background-gradient-rectangle.svg",
  "defaultAppBackgroundImageTransparency": "0.15",
  "defaultPreferencesScreenBackgroundColor": "#10243b",
  "defaultPreferencesScreenBackgroundImage": "/com/eb/javafx/images/svg/background-gradient-rectangle.svg",
  "defaultPreferencesScreenBackgroundImageTransparency": "0.2",
  "defaultSaveLoadScreenBackgroundColor": "#15283f",
  "defaultSaveLoadScreenBackgroundImage": "/com/eb/javafx/images/svg/background-gradient-rectangle.svg",
  "defaultSaveLoadScreenBackgroundImageTransparency": "0.25",
  "resources": {
    "sceneDefinitions": "content/scene-definitions.json",
    "displayDefinitions": "content/display-definitions.json",
    "uiTheme": "src/main/resources/com/eb/javafx/ui/default.css",
    "backgrounds": "assets/backgrounds"
  }
}
```

Each `resourceRoots` entry is a list of root specs. Filesystem paths are resolved against the directory containing `config.json`; classpath roots use the `classpath:/path/inside/jar` form. Application roots are walked first; the engine's bundled roots are appended after, so an application file at the same relative name wins over the library file.

For overrides that point at a single specific file rather than a category root, use the `resources` map and resolve each entry against an application-chosen base directory through `ApplicationResourceConfig.resolveResource(baseDir, "sceneDefinitions")`. The route-specific background defaults provide shared startup colors and images for app-owned shell, preferences, and save/load screens.

The recommended app-authored JSON layout groups resources by type so translation files can be selected by folder. Map this layout onto `resourceRoots` by pointing `support` at the umbrella directory and (optionally) `ui` at the screens subdirectory:

```text
resources/json/
  code-tables/        (support)
  config/             (support — holds app-load.json)
  conversations/      (support)
  display/            (support)
  location/           (support)
  scenes/             (support)
  screens/            (ui)
```

### `config/app-load.json` and per-startup JSON loading

`config/app-load.json` declares which startup-safe JSON files are loaded automatically by `BootstrapOptions.fromConfig(...)`. The file is discovered through the registry as `support/config/app-load.json`, so any application that includes its `resources/json` tree as a `support` root will pick it up. Supported load types are `display`, `scene`, and `conversation`; omit `fileName` to load every immediate `.json` child of the listed directory in alphabetical order, or set `fileName` to load one specific file:

```json
{
  "loads": [
    {"type": "display", "path": "display"},
    {"type": "scene", "path": "scenes"},
    {"type": "conversation", "path": "conversations", "fileName": "sample-conversation.json"}
  ]
}
```

The `path` field is interpreted as a relative key under the `support` category, so `"path": "display"` matches the keys `display/<file>.json` produced by the registry's recursive walk of every support root.

### Bootstrap call

```java
BootstrapOptions options = BootstrapOptions.fromConfig(appRoot.resolve("config.json"))
        .withStaticContentModules(staticModules)
        .withSceneModules(sceneModules)
        .withRouteModules(routeModules);
BootContext context = new BootstrapService(options).boot(primaryStage);
```

After `boot(...)` returns, downstream code reads files through `BootContext.resourceRegistry()`. For example, an application content module that needs the category code table at startup looks it up by relative key inside its support root:

```java
URL categoryTables = context.resourceRegistry()
        .require(ResourceCategory.SUPPORT, "code-tables/category-code-tables.en.json");
CategoryCodeTableDefinition definition = CategoryCodeTableDefinition.load(categoryTables);
```

The same pattern works for engine-bundled files (the registry returns a `jar:` URL that the loader reads transparently) and for application-supplied overrides (the registry returns a `file:` URL pointing at the app's directory). All four engine JSON loaders — `CategoryCodeTableDefinition.load`, `SceneDefinitionJson.load`, `ConversationDefinitionJson.load`, and `DisplayDefinitionJsonLoader.loadInto` — accept a `URL` alongside the older `Path` overload, so callers can stay registry-native.

Use `config/app-load.json` plus `BootstrapOptions.fromConfig(...)` when app-owned display, scene, or conversation JSON should be loaded automatically during bootstrap. Use `ApplicationResourceConfig.resolveResource(...)` together with explicit modules such as `JsonDisplayContentModule` (which now also accepts a `URL` constructor argument) when the application wants named one-off files instead of directory-driven startup loading.

The management UI includes a **Default App Values** screen for inspecting these startup defaults and related display resources. The **Application Values** tab presents editable application config fields with friendly labels, local **Save** and **Reset** actions, browse buttons for file/path-backed fields, and color pickers for color values.

Next to that, the **Locations** tab embeds two JSON editors for the bundled `location/map-text.demo.json` and `location/location-text-town.demo.json` examples. Each editor supports local **Save**, **Format**, and **Reset** actions; **Save** and **Format** validate the JSON through `MapTextDefinition` or `LocationTextDefinition` and rewrite the editor with the engine formatter, while **Reset** restores the staged sample content for the current management-session tab. Use this tab to experiment with localized map labels, per-map location descriptions, and conditional location variants before moving the JSON into an application-owned content tree.

Immediately after that, the **Lookup Variables** tab opens an editable text variable catalog for this management screen session. The catalog uses two fields per row: `name` and `value type`. The value type is limited to `string`, `number`, or `boolean`; use **Add Variable** to append a blank row and **Remove Variable** to delete selected rows, or the last row when nothing is selected. Use the tab-level **Save** and **Reset** buttons to apply or restore the staged catalog rows locally while reviewing startup defaults.

Beneath those fields, the **Application Variables** block provides a multiline table for app-owned variable notes or future app-specific persistence with four fields: `name`, `type`, `value`, and `description`. The type field is limited to `string`, `number`, or `bool`; use **Add Variable** to append a blank row and **Remove Variable** to delete selected rows, or the last row when nothing is selected.

Below that, the **Load Files** block provides a second table for tracking authored startup loads with three fields: `type`, `path`, and `file name`. The type field is limited to `display`, `scene`, or `conversation`; use **Add Load** to append a blank row and **Remove Load** to delete selected rows, or the last row when nothing is selected. Leave `file name` empty when the intent is to load every `.json` file in the specified directory.

```json
{
  "debug": true,
  "resourceRoots": {
    "images":  ["game"],
    "support": ["resources/json"],
    "ui":      ["resources/json/screens"]
  },
  "resources": {
    "sceneDefinitions": "content/scene-definitions.json",
    "displayDefinitions": "content/display-definitions.json",
    "uiTheme": "src/main/resources/com/eb/javafx/ui/default.css"
  },
  "applicationVariables": [
    {
      "name": "playerName",
      "type": "string",
      "value": "Alex",
      "description": "Default player display name used by app-owned startup content."
    },
    {
      "name": "startingGold",
      "type": "number",
      "value": "100",
      "description": "Initial currency for a new profile."
    },
    {
      "name": "debugHud",
      "type": "bool",
      "value": "false",
      "description": "Whether the application should show an app-owned debug HUD."
    }
  ]
}
```

The **Default CSS** tab opens `src/main/resources/com/eb/javafx/ui/default.css` in an editable text area with **Save** and **Reset** buttons below the text field. The save action applies the edited CSS text locally to the management screen session, while reset reloads the bundled CSS resource. The **Layouts** tab shows `src/main/resources/com/eb/javafx/ui/layout-contract.json` as read-only reference data and does not show CSS save/reset actions.

Example/demo code: [`examples/user-manual/04-startup-and-service-wiring/BootstrapDemo.java`](../examples/user-manual/04-startup-and-service-wiring/BootstrapDemo.java)

Additional example/demo code:
- [`examples/user-manual/04-startup-and-service-wiring/ApplicationResourceConfigDemo.java`](../examples/user-manual/04-startup-and-service-wiring/ApplicationResourceConfigDemo.java)
- [`examples/resources/json/config/config.demo.json`](../examples/resources/json/config/config.demo.json)
- [`examples/resources/json/config/app-load.json`](../examples/resources/json/config/app-load.json)

## 5. Content, routing, and scenes

Example/demo code: [`examples/user-manual/05-content-routing-and-scenes/SceneFlowDemo.java`](../examples/user-manual/05-content-routing-and-scenes/SceneFlowDemo.java)

Additional example/demo code:
- [`examples/user-manual/05-content-routing-and-scenes/SceneExecutionAndJsonDemo.java`](../examples/user-manual/05-content-routing-and-scenes/SceneExecutionAndJsonDemo.java)
- [`examples/user-manual/05-content-routing-and-scenes/SceneValidationAndSaveDemo.java`](../examples/user-manual/05-content-routing-and-scenes/SceneValidationAndSaveDemo.java)
- [`examples/resources/json/scenes/scene-definitions.demo.json`](../examples/resources/json/scenes/scene-definitions.demo.json)

### Static content

Use `ContentRegistry` with content modules when a game needs reusable static definitions registered before runtime state exists. `StaticContentModule` is the extension point for application-provided static content, while `EnginePlaceholderContentModule` supplies reusable placeholder content for the engine/test environment.

Keep authored story content and game-specific data outside this repository. Register that content from the application repository through module interfaces.

### Routes

Use `SceneRouter` to register `RouteModule` implementations and navigate by route ID. A route is described with `RouteDescriptor` metadata and constructed through a `RouteFactory` using a `RouteContext`.

Routes are grouped by `RouteCategory`, which helps UI code distinguish menus, gameplay routes, diagnostics, and other navigation surfaces. `DefaultRouteModule` provides reusable route factories backed by UI-neutral view models for the engine.

### Scene definitions and execution

Use `SceneRegistry` to register `SceneModule` implementations. A scene is described by `SceneDefinition` and consists of ordered `SceneStep` instances. `EnginePlaceholderSceneModule` supplies reusable placeholder definitions for engine tests and demos while application modules provide authored scenes. Supported scene flow concepts include:

- dialogue and narration-style steps identified by `SceneStepType`
- action steps
- choices through `SceneChoice`
- transitions through `SceneTransition`
- transition types such as jump, call, return, complete, and route-style transitions identified by `SceneTransitionType`
- resumable state through `SceneFlowState` and `SceneReturnPoint`
- hotspot map steps through `SceneStepType.HOTSPOT_MAP` and `SceneStep.hotspotMap(id, mapId)`

Use `HotspotDefinition` to define a named clickable region within a hotspot map. Each hotspot stores a stable id, label text key, fractional bounds (`x`, `y`, `width`, `height` all in `0.0–1.0` space), an optional condition expression, and a target scene id. Use `HotspotMapDefinition` to group a background image reference with an ordered list of `HotspotDefinition` entries. Register maps with `HotspotMapRegistry` using `register(...)`, `find(id)`, and `require(id)`.

When a scene executor reaches a `HOTSPOT_MAP` step it pauses with `SceneExecutionStatus.WAITING_FOR_HOTSPOT` and populates `SceneExecutionResult.hotspotMapViewModel()` with a resolved `HotspotMapViewModel`. Call `SceneExecutor.selectHotspot(context, state, hotspotId)` when the player clicks a region; this jumps to the selected hotspot's target scene and returns the next `SceneExecutionResult`. Wire a `HotspotMapRegistry` into the executor via its 3-arg or 4-arg constructor; the 1- and 2-arg constructors create an empty registry.

Use `SceneExecutor` to execute scene flow and return a `SceneExecutionResult` with `SceneExecutionStatus`. `SceneExecutionResult.canRollback()` indicates whether the player can step back to the previous pause point. Use `ScenePresenter` and view-model classes when JavaFX UI code needs a UI-neutral representation of the current scene and choices.

Use `SceneRegistry.validationReport(...)` when application startup needs diagnostics rather than fail-fast validation. The returned `SceneValidationReport` contains `SceneGraphSummary` entries plus `SceneValidationProblem` diagnostics with `SceneValidationSeverity` levels for duplicate IDs, missing jump/call targets, unreachable steps, and final-step `NEXT` warnings. Applications can pass `SceneReferenceValidator` instances such as `SceneReferenceValidators.knownSpeakers(...)` or `knownDisplayReferences(...)` to validate app-owned speaker/display IDs without moving those registries into the engine.

### Scene step and choice enhancements

**NVL display mode.** `SceneStep.withDisplayMode(SceneDisplayMode)` stores a presentation hint in step metadata. `SceneDisplayMode.ADV` is the default (single-dialogue-box mode); `SceneDisplayMode.NVL` signals the adapter to accumulate lines in a full-screen text block. Read the hint with `SceneStep.displayMode()`. The executor is unaffected; mode is purely presentational.

**Conditional steps.** Use `SceneStep.conditional(id, expression, thenTransition, elseTransition)` for invisible branching without a visible choice menu. The expression is a `SceneConditionExpression` string DSL: `flag:id`, `!flag:id`, `unlock:id`, `counter:id>=N`, `counter:id>N`, `counter:id<N`, `counter:id==N`. Construct a `SceneConditionEvaluator` with a `ProgressTracker` and pass it to the `SceneExecutor(SceneRegistry, SceneConditionEvaluator)` constructor.

**Conditional choice visibility.** Attach a condition expression and a `ConditionPolicy` to any `SceneChoice` with `choice.withCondition(expression, policy)`. `ConditionPolicy.HIDE` removes the choice from the resolved list when the condition is false. `ConditionPolicy.GREY` keeps the choice in the list but marks it disabled. Read the stored values with `conditionExpression()` and `conditionPolicy()`. The executor evaluates conditions automatically at CHOICE step time when a `SceneConditionEvaluator` is wired in.

**Timed choices.** Use `SceneStep.withChoiceTimeout(timeoutMs, defaultChoiceId)` to mark a choice menu as time-limited. Read with `choiceTimeoutMs()` (returns `null` when no timeout is set) and `choiceTimeoutDefaultId()`. The UI adapter drives the countdown timer and calls `SceneExecutor.selectChoice(...)` with the default ID on expiry; the executor has no built-in timer.

**Menu captions.** Use `SceneStep.withMenuCaption(captionTextKey)` to attach a prompt label above a choice menu. Read with `menuCaptionTextKey()` (returns `null` when no caption is set). The adapter looks up and renders the text; the executor is unaffected.

**Per-choice captions.** Use `SceneChoice.withCaption(captionTextKey)` to attach a flavour-text hint label to a single choice (e.g. a cost or tooltip). Read with `captionTextKey()` (returns `null` by default). Adapter renders the label near the choice button; the executor is unaffected.

**Loop menus.** Use `SceneStep.withMenuLoop()` to make a choice menu re-present itself after the player picks a non-exit option. Mark a choice as non-exiting with `SceneChoice.asMenuReturn()`; any choice without that marker exits the menu normally. Read the flags with `menuLoop()` and `exitsMenu()`. The executor handles the loop in `selectChoice` — no extra state is needed.

### Skip mode and auto mode

Use `SeenStepTracker` to track which dialogue and narration steps the player has already read. Call `markSeen(sceneId, stepId)` when a step is displayed and `hasSeen(sceneId, stepId)` to query it. `SeenStepSnapshot` snapshots the full set for save/load; `SeenStepSnapshotCodec` serializes and deserializes it as a versioned save section under the section id `seenSteps`.

Use `SceneExecutor.advanceSkipping(context, state, seenSteps)` to run in skip mode: it fast-forwards past already-seen DIALOGUE and NARRATION steps and always pauses on unseen steps and on CHOICE steps. Combine this with a `ScenePlaybackMode` value (`NORMAL`, `SKIP`, `AUTO`) stored by the adapter to control playback behavior:

- `NORMAL` — advance only on explicit user input; call `advanceUntilPause` or `continueFromText`.
- `SKIP` — call `advanceSkipping` instead of `continueFromText` to bypass seen text automatically.
- `AUTO` — advance text steps automatically after a delay; adapter drives a timer and calls `continueFromText` on expiry; CHOICE steps always pause.

### Dialogue rollback

Use `RollbackBuffer` to let players step back through previously displayed lines. The buffer is a fixed-capacity ring of `RollbackEntry` snapshots, each recording the `SceneFlowState` at a pause point plus captured values from registered `RollbackContributor<T>` instances.

Construct a `RollbackBuffer(capacity)` and pass it to `SceneExecutor(SceneRegistry, SceneConditionEvaluator, RollbackBuffer)`. The executor snapshots the state automatically before each DIALOGUE, NARRATION, and CHOICE pause. Call `executor.rollback(context)` to return to the previous pause point; it throws `IllegalStateException` when there is nothing to roll back to. `SceneExecutionResult.canRollback()` tells the adapter whether to show the back button.

Register application-owned state contributors with `buffer.register(id, contributor)`. `RollbackContributor<T>` is a two-method interface: `T capture()` and `void restore(T snapshot)`. Contributors that also need to persist across save/load should implement `PersistableRollbackContributor<T>`, which extends both `RollbackContributor<T>` and the existing `SaveSnapshotCodec<T>`.

`RollbackSnapshotCodec` persists the buffer's scene flow positions as a versioned save section under the section id `rollback`. Contributor state values are not included in the rollback section; they are restored from the main save data when the save is loaded.

### Visual scene transitions

Use `VisualTransitionRegistry` to register named `VisualTransitionDefinition` instances at startup. Each definition carries a `SceneTransitionEffect` (DISSOLVE, FADE_BLACK, WIPE_LEFT, WIPE_RIGHT, MOVE_IN_LEFT, MOVE_IN_RIGHT, NONE) and a duration in milliseconds. Scene steps that reference a named transition produce a `VisualTransitionRequest` in `SceneExecutionResult`; adapters read this request and play the visual effect between scene changes.

### Image tag resolution

Use `DisplayTagRegistry` to register `DisplayTagDefinition` instances that map semantic tag strings to target display IDs, optional layer overrides, and transform preset names. Use `DisplayTagResolver` to resolve a tag string plus an optional position hint to a concrete `DisplayTagResolution` containing the display ID, layer, and transform. Unknown tags return a descriptive error rather than null.

### Gallery framework

Use `GalleryRegistry` to register `GalleryDefinition` instances at startup. Each definition has an id, title text key, and an ordered list of `GalleryEntry` objects. Each entry carries an id, image reference, caption text key, and a required unlock id. The registry validates all image references against `ImageDisplayRegistry` at startup.

Use `GalleryService` to query entries for display. It returns a list of `GalleryEntryViewModel` objects: unlocked entries expose the full image reference, locked entries return `unlocked = false` with no image reference exposed.

### Scene presentation view models

Scene view models are deliberately UI-neutral. They are useful when the engine needs to expose scene execution state to JavaFX screens, tests, debug panels, or application-owned renderers without passing mutable executor objects or JavaFX controls across package boundaries. Use `ScenePresenter` to convert a `SceneExecutionResult` into these models after each scene execution step or choice selection.

- `SceneViewModel` is the top-level scene presentation state. It carries the current execution status, scene id, step id, speaker id, text definition id, display reference, choices, selected choice history, message, dialogue rows, status rows, and effect previews. Use it as the single object handed to a scene renderer such as `SceneFlowView` so the renderer can display scene progress without knowing how scene execution works.
- `SceneChoiceViewModel` represents one rendered choice after availability has already been evaluated. It includes the choice id, returned choice value, choice text definition id, whether the choice is available, an optional disabled reason, whether it was selected earlier, string metadata, and effect previews. Use it to build choice buttons or test choice state without re-running requirement checks in the UI layer.
- `SceneDialogueRowViewModel` represents one dialogue or narration row. It includes the step type, optional speaker id, text definition id, and optional display reference. Use it when a renderer needs a normalized row for dialogue panels instead of inspecting raw `SceneStep` objects.
- `SceneStatusRowViewModel` represents one label/value diagnostic row such as status, active scene, active step, selected choices, pending interruption, or message. Use it for HUD, debug, and manual-test surfaces where scene execution state should be visible in a consistent format.
- `SceneEffectPreviewViewModel` represents preview-only metadata for a scene step or choice. It contains a label and value derived from `preview.*` metadata or a fallback reference. Use it when a prototype UI wants to show what display, effect, or authored metadata would be applied before an application supplies custom rendering.
- `HotspotOptionViewModel` represents one evaluated hotspot region ready for UI rendering. It carries the hotspot id, label text key, fractional bounds (x, y, width, height), and an enabled flag that reflects whether the hotspot's condition was satisfied. Use it to build clickable overlay buttons or test hotspot state without re-evaluating conditions in the UI layer.
- `HotspotMapViewModel` carries the resolved background image reference and an immutable list of `HotspotOptionViewModel` entries ready for overlay rendering. Retrieved from `SceneExecutionResult.hotspotMapViewModel()` when the result status is `WAITING_FOR_HOTSPOT`.
- `TalkingAnimationCue` signals which talking animation should play while a speaker delivers dialogue. It pairs a `speakerId` with a `talkingAnimationId` resolved from the speaker's `CharacterTemplate` metadata. Retrieved from `SceneExecutionResult.talkingCue()` when the result is a `DIALOGUE` step and the speaker's template declares a `talkingAnimationId` metadata key. Absent for narration steps and dialogue steps whose speaker has no registered template or no `talkingAnimationId` metadata.

`SceneFlowView.createContent(...)` renders choice buttons that call the supplied choice handler with the `SceneChoiceViewModel.value()`, not the internal choice id. `SceneFlowView.displayAndWaitForChoice(...)` can display a conversation-style `SceneViewModel` through an application-supplied display handler, block until a choice button is pressed, and return that choice value. If the displayed view model has no choices, it displays the content and returns `null`.

Use `SceneDefinitionJson` for simple JSON-authored scenes that do not require executable Java `ActionRequirement` or `ActionEffect` instances. JSON scenes can include dialogue/narration text definition IDs, choices, transitions, display references, and string metadata. Register more complex requirements/effects through Java scene modules. In an application, keep the scene JSON path in `ApplicationResourceConfig.resources()` and resolve it with `resolveResource(applicationRoot, "sceneDefinitions")` before loading.

Use `SceneFlowStateJson` to serialize and restore `SceneFlowState` snapshots containing the active scene, step index, call stack, selected choice IDs, and pending UI interruption marker. For future save/load payloads, `SceneFlowStateJson.toSnapshotSection(...)` wraps that JSON as a versioned `SaveSnapshotSection` named `sceneFlowState`, and `fromSnapshotSection(...)` validates the section id and version before loading it.

Use `SceneFlowSnapshotDocuments` when an application save workflow wants the reusable scene-flow section composed into a `SaveSnapshotDocument` alongside app-owned sections. The helper registers the `sceneFlowState` section as required and restores the scene state while leaving the outer save schema under application control.

## 6. UI screens and themes

The `ui` package provides reusable JavaFX surfaces and helpers:

- `ScreenViewModel` describes reusable route screen content without JavaFX control state. It contains the screen title, informational body lines, and route actions. Use it for menu, summary, diagnostics, and placeholder screens when the content is mostly text plus navigation and should be easy to test before JavaFX controls are created.
- `ScreenActionViewModel` describes one route-backed action for a `ScreenViewModel`. It contains the button label, destination route id, and enabled state. Use it when reusable screens need navigation buttons that can be enabled or disabled without coupling the screen model to JavaFX `Button` setup.
- `PreferencesSummaryViewModel` describes the reusable preferences screen with typed rows for the currently editable settings summary. Each `PreferencesSummaryRowViewModel` contains a label/value pair for editable preferences such as audio levels, theme color, or footer display mode, which keeps the route summary easy to test while the JavaFX screen renders dedicated controls instead of a large read-only preference dump.
- `SaveLoadSummaryViewModel` describes the reusable save/load summary screen with explicit schema metadata fields. It keeps the schema version, save directory, informational note, and actions as named data so save/load diagnostics can evolve without parsing or rebuilding display strings.
- `SaveSlotPresentation` is a reusable save-slot view contract for already-renderable slot titles, details, and compatibility state when an application wants a custom save browser above engine snapshot validation.
- `HudSummaryViewModel` describes the reusable HUD summary screen with a small dedicated model. It currently carries the HUD layer description, opacity, and actions and gives the HUD summary room to grow beyond a couple of text lines without falling back to ad hoc strings. Use `HudSummaryRow` for generic label/value HUD summary rows that are already formatted by the caller.
- `HudStatusContainerViewModel`, `HudStatusGroupViewModel`, and `HudStatusRowViewModel` describe reusable read-only HUD/status overlays with visibility, opacity, stack order, groups, and rows. Applications supply game-specific status values and visibility rules.
- `SnapshotSectionPreviewViewModel` describes one preview row for a `SaveSnapshotSection`. It contains the section id, schema version, and a shortened JSON payload summary. Use it when save/load diagnostics or future save browsers need to show the contents of composed save snapshots without exposing the full payload or requiring custom parsing in the UI.
- `ConversationHistoryViewModel`, `ConversationHistoryEntryViewModel`, `ConversationHistoryRowViewModel`, and `ConversationHistoryColumnViewModel` describe recorded conversations from `GameState.conversationHistory()` for review screens. `ConversationHistoryScreen` renders those models as a reusable route.
- `MainMenuEntry` is the content-neutral contract for one main-menu route/action entry, and `InformationalScreenModels.backToMainMenu(...)` builds simple placeholder or error screen models with a standard main-menu action.
- `DisplayPreviewBinding` carries image id, source path, layer, and asset-resolution state for display diagnostics and app-owned previews.
- `ScreenBackgroundFit` names reusable background sizing modes: stretch or center-crop.
- `ScreenLayoutType`, `ScreenLayoutModel`, and `ScreenLayoutSection` define reusable screen layout intent without JavaFX control state. Use them when a screen needs a stable general structure such as a titled panel, two-column layout, sidebar/content layout, HUD/status overlay, dialogue surface, menu/action list, form, preview/card grid, or main app layout. The `MAIN_APP_LAYOUT` value names the app-frame composition described under "Main app layout"; it is rendered through `MainAppLayoutRenderer` rather than the standard `ScreenLayoutRenderer`.
- `ScreenDesignModel`, `ScreenDesignBlock`, and `ScreenDesignItem` define editable JSON-backed screen designs with stable screen, block, and item ids. Use `ScreenDesignService.addItemToBlock(...)` or `addTemporaryItemToBlock(...)` when code needs to target a block id directly; temporary items render in preview/test mode but `ScreenDesignJson.save(...)` excludes them from persisted JSON. `ScreenDesignJson` saves/loads documents with top-level `id`, `title`, `layoutType`, `metadata`, and ordered `blocks`. Each block carries `id`, optional `title`, optional block-level `layoutType`, optional `parentBlockId`, optional `styleClass`, `metadata`, and an inline `items` array containing the items that belong to that block. Each item carries `id`, `type`, optional `label`, `text`, `value`, `defaultValue`, `styleClass`, and `metadata`; `blockId` is derived from the containing block and is not required in the JSON wire format. Known boolean metadata keys (`dialog`, `dismissOnClickOutside`, `dismissOnEscape`, `showTicks`, `showLabels`, `editable`) and numeric keys (`borderThickness`, `transparency`, `min`, `max`, `step`) are serialized as JSON primitives rather than quoted strings. The parser also accepts older flat-format files that carry a top-level `items` array with explicit `blockId` fields, quoted numbers, and comma-separated `options` strings, so existing documents continue to load without conversion.
- `ConversationDefinition`, `ConversationDefinitionJson`, and `JsonConversationContentModule` define JSON-backed conversation documents for authored visual-novel content using the AltLife exported shape. A conversation file has top-level `name`, `language`, and ordered `conversations`; each conversation carries `id`, `description`, and typed `lines`. Line `type` supports `say` by default, `shout` for uppercase bold text, `whisper` for lowercase italic text, and `choice` for player-selectable variants with per-choice values and conditions. `JsonConversationContentModule` projects that document into reusable content definitions and scene definitions when runtime registration is needed.
- `ScreenLayoutContract` loads the machine-readable layout contract from `src/main/resources/com/eb/javafx/ui/layout-contract.json`, which lists engine-provided layout types, the default stylesheet, and stable CSS style hooks applications can target.
- `ScreenInventory`, `ScreenInventoryItem`, `ScreenInventorySource`, `ScreenInventoryScanner`, and `ScreenInventoryAssignmentCategory` provide content-neutral inventory models for application-owned screen/style/control migration scanners. Use them to classify source artifacts as route-backed, reusable-control-backed, deferred, deprecated, excluded, or app-owned without hard-coding source-engine names in the engine.
- `ViewModelScreen` renders a `ScreenViewModel` with generic labels and navigation buttons.
- `ScreenLayoutRenderer` renders a `ScreenLayoutModel` into JavaFX nodes. It keeps route screens thin by letting them gather data, build a UI-neutral model, and delegate JavaFX node creation plus style-class assignment to the shared renderer.
- `ScreenShell` wraps screen content in a consistent shell. Its reusable footer bar is shown by default on titled shells and exposes helpers for visibility, transparency, compact/mobile presentation, icon-only label mode, localized labels/tooltips, and per-option enabled state. Use `FooterOption` ids such as `back`, `history`, `save`, `quick-save`, and `preferences` when application state, localization bundles, or preferences need to customize a specific footer control. Default footer functions also expose standalone SVG icon resources under `src/main/resources/com/eb/javafx/images/icons/` using names like `footer-back.svg`, `footer-save.svg`, and `footer-preferences.svg`; `icons-10x10.svg` remains available in the same directory as the source icon sheet/reference.
- `ScreenNavigation` centralizes navigation callbacks.
- `PreviewSummaryView` creates simple titled preview panels for display, scene, and snapshot summaries.
- `MainMenuScreen`, `SceneFlowScreen`, `DisplayBindingsScreen`, `HudSummaryScreen`, `SaveLoadSummaryScreen`, `PreferencesSummaryScreen`, and `ConversationHistoryScreen` provide generic reusable screens or screen models.
- `DialogEntriesView` is the reusable widget for the `MAIN_APP_LAYOUT` dialog slot. It owns a `DialogHistory`, renders rich text via `TextFlow`, and exposes `addEntry`, `setEntries`, `say`, `shout`, `whisper`, `startConversation`, `endConversation`, `clear`, `goBack`, `goForward`, `bindToFooter`, and `installKeyboardShortcuts`. The cursor decides which entry sits at the bottom of the panel at full opacity; earlier entries are faded to `0.5` opacity. Public sealed `Entry` permits — `PlainEntry`, `SpokenEntry`, `ConversationStart`, `ConversationEnd` — model the four visible row kinds. See "Main app layout → Dialog entries widget" below.
- `CaptureTestScreen` supports test/manual capture workflows; its `CaptureFormModel` stores validated capture form values.
- `UiTheme` loads the reusable stylesheet from `src/main/resources/com/eb/javafx/ui/default.css`.
- `StartupErrorReporter`, `StartupFailureException`, and `StartupFailureCategory` provide structured startup diagnostics.

### SVG button styles

`ButtonVisuals` provides two reusable SVG-backed button styles:

- `applySvgArtwork(Button)` renders the standard pill button using `button-pill-long.svg`.
- `applyBevelSvgArtwork(Button)` renders the rectangular beveled button using `button-bevel.svg`.

Both styles rasterize the SVG artwork to the requested button size, support pressed-state gradient reversal, and keep the button text centered horizontally and vertically, including multiline labels. The text node exposes the `svg-button-artwork-text` style class, so applications can set the caption color in CSS with `-fx-fill`, for example by overriding `.svg-button-artwork-text` or state selectors such as `.svg-button:hover .svg-button-artwork-text`. The SVG artwork fill/gradient itself comes from the SVG resource; use a different SVG resource or engine extension if an application needs to recolor the button body rather than the caption.

### SVG screen backgrounds

`ScreenShell` provides SVG background helpers for whole-screen artwork. Use `ScreenShell.setBackgroundSvg(screen, "/path/background.svg")` or `ScreenShell.withBackgroundSvg(...)` to wrap an existing screen region in a `StackPane` with the SVG behind the content. The SVG background is not clickable, has no border or padding, and resizes with the returned root; use that returned root as the scene root when the artwork should cover the full screen.

When an application needs the SVG artwork to blend with underlying content or to reveal a chosen canvas color through transparent regions, use the `ScreenShell.backgroundSvg(String svgResourcePath, double opacity, Color canvasBackgroundColor)` overload. The `opacity` argument applies to the rendered SVG image (`0.0` to `1.0`), and the canvas color is painted behind the SVG before the rest of the screen content is layered above it.

Pass two or more resource paths to layer SVG backgrounds in order:

```java
StackPane root = ScreenShell.setBackgroundSvg(
        screen,
        "/com/example/images/background-base.svg",
        "/com/example/images/background-overlay.svg");
```

The first SVG is the back-most layer, each additional SVG is placed above it, and the screen content is placed above all background layers.

Use the configurable overload directly when you need a single background node with transparency or a non-transparent canvas fill:

```java
Region background = ScreenShell.backgroundSvg(
        "/com/example/images/background-overlay.svg",
        0.60,
        Color.web("#08111f"));
```

The manual `SvgBackgroundTestScreen` includes controls for switching the packaged SVG, adjusting transparency, and choosing the canvas color so applications can verify how transparent artwork composes with their screen content.

### Screen layouts, reusable screens, and editable screen designs

Use `ScreenViewModel` when the screen can be described as a title, a small number of informational lines, and route-backed actions. Use `ScreenLayoutModel` when the screen needs a more explicit structure with named sections, form-style rows, menu/action lists, dialogue/status panels, preview cards, or a sidebar/content split. `ScreenLayoutRenderer` turns that UI-neutral layout data into JavaFX nodes and applies the stable engine style hooks from `default.css`.

Use `ScreenDesignModel` when you want the same reusable layout idea to be editable as JSON or through the screen designer. A screen design contains:

- one screen id and title
- one `ScreenLayoutType`
- ordered `blocks`
- ordered saved `items`
- optional screen/block/item metadata maps for tool- or app-owned extra data, including optional screen-level dialog hints such as `dialog`, `dismissOnClickOutside`, and `dismissOnEscape`

Blocks and items are stable editable records:

- each `ScreenDesignBlock` has `id`, optional `title`, optional block-level `layoutType`, optional `parentBlockId`, optional conversation-style `conditions`, optional `styleClass`, and `metadata`
- each `ScreenDesignItem` has `id`, `blockId`, `type`, optional `label`, optional `text`, optional `value`, optional `defaultValue`, optional `sequence`, optional `styleClass`, and `metadata`
- all item types support an optional `label`; TEXT and TEXT_AREA store the label on the model but the generic renderer uses it only as a section annotation, not as a rendered caption
- `FIELD` and `MULTI_LINE_FIELD` support `label`, `value`, `defaultValue`, and `editable`; the label is rendered above the input control
- `BUTTON` uses `label` as the rendered button caption
- `POPLIST` renders a non-editable dropdown; put choices as a JSON string array in `options`, e.g. `["Option A", "Option B"]`
- `COMBO_BOX` renders an editable or non-editable combo box; `editable: true` lets the user type; put choices as a JSON string array in `options`, e.g. `["Choice 1", "Choice 2"]`
- `SLIDER` renders a range slider; use `metadata.min`, `metadata.max`, and `metadata.step` to configure the range (numeric values); `editable: true` makes the slider interactive; `metadata.showTicks` and `metadata.showLabels` (boolean values) show tick marks and labels
- `RADIO_GROUP` renders a group of mutually exclusive radio buttons; put choices as a JSON string array in `options`, e.g. `["Red", "Green", "Blue"]`; `metadata.orientation` of `horizontal` or `vertical` (default) controls layout; `editable: true` makes the buttons clickable

`ScreenDesignLayoutAdapter` converts a `ScreenDesignModel` into a `ScreenLayoutModel` for preview or runtime rendering. It preserves stable block/item ids, converts `parentBlockId` relationships into nested layout sections, maps field-style items to `label: value/defaultValue` lines, carries field metadata needed for JavaFX preview/runtime input controls (including editable state), sorts block items by optional `sequence` before falling back to authored JSON order, and carries item/block metadata into the layout so renderer-supported visual metadata can be applied consistently. Block `conditions` are preserved in section metadata as a JSON string array, and applications can call the binding overload with a string map so authored text such as `$playerName` or `${playerName}` is resolved during scaffolding. Complex or application-specific controls can still be added programmatically by targeting stable block ids after the JSON scaffold is loaded.

### Main app layout

The engine ships a higher-level scaffolding for the typical visual-novel app frame: a background layer, a central frame split between a story area and a smaller dialog area with the standard footer below it, and any number of HUD overlay screens layered on top of the story area. The scaffolding is authored as a `ScreenDesignModel` whose `layoutType` is `MAIN_APP_LAYOUT`; the structural parts are pulled from the screen metadata, and each HUD overlay maps to one block on that screen.

#### Frame structure

```
+-------------------------------------------+
|  background (image + colour)              |
|  +-------------------------------------+  |
|  |  story area (scrolls when overflow) |  |  <- HUD overlays live here
|  |  +-------+   +---------+            |  |
|  |  |status |   | log     |            |  |
|  |  +-------+   +---------+            |  |
|  |                                     |  |
|  +-------------------------------------+  |
|  |  dialog area (pinned, fixed height) |  |
|  +-------------------------------------+  |
|  |  footer (ScreenShell.footerBar)     |  |
|  +-------------------------------------+  |
+-------------------------------------------+
```

- The **story area** fills the available height and grows a vertical scrollbar when its content is taller than the slot. HUD overlays are parented into the story area, so anchors like `BOTTOM_RIGHT` or `100%`-height placements land on the story slot's edge — overlays never overlap the dialog or footer.
- The **dialog area** is pinned at the bottom (vertical orientation) or right (horizontal orientation). Its size is `(1 − storyDialogRatio) × frame.size`. Dialog and footer remain visible even when the story area is too short to show all its content.
- The **footer** is the standard `ScreenShell.footerBar()`; turn it off with `showFooter: false`.

#### Screen metadata

`MainAppLayoutPlan.from(ScreenDesignModel)` parses a design into a UI-neutral plan. Supported screen-level metadata keys:

- `storyScreenId` (required) — id of the screen rendered in the story area
- `dialogScreenId` (optional) — id of the screen rendered in the dialog area; when omitted the story area fills the entire central frame
- `storyDialogRatio` (default `0.5`) — story-area share of the central frame, `0.0`–`1.0` (so the dialog occupies the remaining half by default)
- `appLayoutOrientation` (default `vertical`) — `vertical` (story on top, dialog on bottom) or `horizontal` (side-by-side)
- `showFooter` (default `true`) — render the standard `ScreenShell` footer below the central frame
- `storyInsets` / `dialogInsets` (default `0`) — CSS-style shorthand padding inside the corresponding slot. Accepts 1, 2, or 4 comma-separated numbers (`"10"`, `"8, 12"`, `"4, 12, 4, 12"`)
- `appLayoutBackgroundImage`, `appLayoutBackgroundFit` (`STRETCH` or `CROP_CENTER`), `appLayoutBackgroundTransparency`, `appLayoutBackgroundColor` — background layer configuration

#### HUD overlay metadata

Each block in a `MAIN_APP_LAYOUT` design represents one HUD overlay. Supported block-level metadata keys:

- `overlayScreenId` (required) — id of the screen rendered inside the overlay
- `overlayPlacement` (default `alignment`) — `alignment`, `pixels`, `percent`, or `relative`
- `overlayAnchor` — for `alignment` (default `TOP_LEFT`), accepts JavaFX-style anchors (`TOP_RIGHT`, `BOTTOM_CENTER`, `CENTER`, …; short aliases `TOP` / `BOTTOM` are accepted). For `relative` (default `RIGHT`), accepts `ABOVE`, `BELOW`, `LEFT`, or `RIGHT`
- `overlayAnchorField` — required for `relative`: id of the sibling overlay block this one is anchored to (for example `"status"` to place "left of the status block"); ignored otherwise
- `overlayOffsetX`, `overlayOffsetY` — pixel offsets from the anchor (`alignment` / `relative`), absolute pixel coordinates (`pixels`), or fractional `0.0`–`1.0` coordinates (`percent`)
- `overlayWidth`, `overlayHeight` (optional) — preferred overlay size in pixels
- `overlayTransparency` (default `0.0`) — overlay transparency, `0.0` (opaque) to `1.0` (invisible)
- `overlayVisible` (default `true`) — hides the overlay when `false` (the wrapper is still parsed but excluded from the JavaFX tree)

#### Placement modes

| Mode | When to use | What `overlayOffsetX` / `overlayOffsetY` mean |
|---|---|---|
| `alignment` | Anchor the overlay to one of the story slot's nine grid positions | Pixel offsets *from* the anchor (positive offsetX pushes the overlay away from the anchored edge) |
| `pixels` | Absolute coordinates inside the story slot | Pixel coordinates from the story slot's top-left |
| `percent` | Coordinates that scale with the story slot's size | Fractions of story-slot width / height (0.0 = left/top, 1.0 = right/bottom) |
| `relative` | Anchor against *another overlay block* — for example "left of the status block" | Pixel offsets added on top of the relative anchor position |

For `relative` mode the renderer wires the overlay's position to its anchor sibling's `boundsInParent`, so the position keeps following the anchor when the window resizes, the anchor moves, or the anchor changes size. If the referenced anchor block is missing or hidden, the dependent overlay falls back to the story-slot top-left rather than failing.

#### Rendering and the screen resolver

`MainAppLayoutRenderer.render(plan, resolver, resourceRoot)` turns the plan into a JavaFX `StackPane`. The renderer never resolves screens itself; the supplied `MainAppScreenResolver` is asked once per slot — story, dialog, and each HUD overlay — to return a `Node` for the requested id. Applications typically back the resolver with their `ScreenInventory`, `ScreenDesignService`, or another screen factory. Returning `null` from the resolver leaves the corresponding slot empty rather than failing.

The screen designer's live preview short-circuits to `MainAppLayoutRenderer` with a labelled-placeholder resolver, so authors see the proportions and overlay placements without wiring real story / dialog / HUD screens.

#### Example

A minimal main-app-layout design with a background image, a story / dialog split at the default ratio, and three overlays demonstrating `alignment`, `percent`, and `relative` placement:

```json
{
  "id": "test.main-app-layout",
  "title": "screen.title",
  "layoutType": "MAIN_APP_LAYOUT",
  "metadata": {
    "appLayoutBackgroundImage": "/com/eb/javafx/images/svg/background-gradient-rectangle.svg",
    "appLayoutBackgroundFit": "STRETCH",
    "appLayoutBackgroundTransparency": "0.2",
    "appLayoutBackgroundColor": "#0d1b2d",
    "storyScreenId": "story",
    "dialogScreenId": "dialog",
    "storyInsets": "8",
    "dialogInsets": "12, 16, 12, 16"
  },
  "blocks": [
    {
      "id": "hud.status",
      "title": "block.hud.status.title",
      "metadata": {
        "overlayScreenId": "hud-status",
        "overlayPlacement": "alignment",
        "overlayAnchor": "TOP_LEFT",
        "overlayOffsetX": "16",
        "overlayOffsetY": "16",
        "overlayWidth": "260"
      }
    },
    {
      "id": "hud.minimap",
      "title": "block.hud.minimap.title",
      "metadata": {
        "overlayScreenId": "hud-minimap",
        "overlayPlacement": "percent",
        "overlayOffsetX": "0.82",
        "overlayOffsetY": "0.06",
        "overlayWidth": "180",
        "overlayHeight": "180"
      }
    },
    {
      "id": "hud.tooltip",
      "title": "block.hud.tooltip.title",
      "metadata": {
        "overlayScreenId": "hud-tooltip",
        "overlayPlacement": "relative",
        "overlayAnchor": "RIGHT",
        "overlayAnchorField": "hud.status",
        "overlayOffsetX": "8"
      }
    }
  ]
}
```

Wiring it up at runtime is the responsibility of the application; the resolver hands the engine a `Node` for each id used in the design (story, dialog, and the three HUD screen ids):

```java
ScreenDesignModel design = ScreenDesignJson.load(designPath);
MainAppLayoutPlan plan = MainAppLayoutPlan.from(design);

MainAppScreenResolver resolver = screenId -> switch (screenId) {
    case "story"        -> storyFactory.createNode();
    case "dialog"       -> dialogFactory.createNode();
    case "hud-status"   -> hudStatusFactory.createNode();
    case "hud-minimap"  -> hudMinimapFactory.createNode();
    case "hud-tooltip"  -> hudTooltipFactory.createNode();
    default             -> null; // unknown id; slot stays empty
};

StackPane root = MainAppLayoutRenderer.render(plan, resolver, designPath.getParent());
Scene scene = new Scene(root, 1280, 720);
```

The packaged design at [`examples/resources/json/screens/main-app-layout-test-screen.json`](../examples/resources/json/screens/main-app-layout-test-screen.json) exercises every supported screen metadata key and all four overlay placement modes; its `_text.json` sidecar holds the localized titles. Load it in the screen designer to see the live preview.

#### Dialog entries widget

`DialogEntriesView` is the engine's reusable widget for the dialog slot. It extends `ScrollPane`
(with a right-hand vertical scrollbar that appears as soon as the rendered entries exceed the
viewport height), owns a sealed `Entry` model and a `DialogHistory`, renders entries as wrapping
labels in a column-aligned `HBox` inside the scroll content, and offers say / shout / whisper /
start-conversation / end-conversation helpers that mirror writes into both the visible stack and
the history.

The current entry sits at the bottom of the panel at full opacity, in the larger default font and
bright white; earlier entries are stacked above it at `0.5` opacity and a smaller font so the
player sees recent context fading away. Long messages wrap to multiple lines automatically. After
every append the viewport auto-scrolls to the bottom so the newest entry stays visible — the
player can still scroll up through the full backlog via the right-hand scrollbar (or mouse
wheel) without changing the cursor.

Because the widget is a `ScrollPane`, callers that need the rendered entry node list should call
`entryNodes()` rather than `getChildren()` (which now returns the ScrollPane skin's children, not
the entries). `entryNodes()` returns the inner `VBox`'s children in stacking order — the topmost
entry first, the current entry last.

Entry kinds (the sealed `DialogEntriesView.Entry` hierarchy):

- `PlainEntry` — bare narration string, rendered as a wrapping `Label`.
- `SpokenEntry` — `LineType.SAY` / `SHOUT` / `WHISPER` line with optional speaker, rendered as an
  `HBox` with a fixed-width speaker column (so message bodies align across entries) plus a
  wrapping body `Label`. The speaker label is **right-aligned** within its column so the name
  and colon sit flush against the body text, giving each row a clean left edge for reading.
  When `DialogSpeaker.textColor()` is set, that colour is applied as an inline style to *both*
  the speaker label and the message body, so a different role colour can be used for narrator,
  MC, book girl, random girl, etc. by simply constructing each `DialogSpeaker` with the
  appropriate colour. Shout uppercases its body and applies bold via the `.dialog-entry-shout`
  rule; whisper lowercases and italicises via `.dialog-entry-whisper`.
- `ConversationStart` — divider listing the conversation's participants (and the start timestamp
  when supplied). Rendered as `── Conversation: Alice, Bob ──` with horizontal lines on either
  side. Only the *start* divider acts as the visual separator between conversations — the start
  of a new conversation already implies the previous one has ended.
- `ConversationEnd` — model record kept for backward compatibility, but `endConversation(...)` no
  longer appends one to the visible entries (the underlying `DialogHistory` still records the
  end timestamp). Callers that need a custom closing marker can still construct a
  `ConversationEnd` and append it manually if desired.

Cursor navigation (`goBack()` / `goForward()`, the primary/secondary mouse clicks, and the
Space/Backspace shortcuts) **auto-skips divider entries** so the cursor only ever lands on a
spoken or plain line. Dividers remain visible above the cursor as section markers, but they are
not "click-through positions". The `canGoBackProperty()` / `canGoForwardProperty()` flags reflect
the same skip-aware rule, so the footer back/forward affordances grey out correctly when only
dividers stand between the cursor and an end of the entry list.

API in `com.eb.javafx.ui`:

| Method | Purpose |
|---|---|
| `addEntry(String)` | Append a `PlainEntry` and move the cursor to it. |
| `setEntries(Collection<String>)` | Replace history with plain narration entries. |
| `say(speaker, text)` / `say(text)` | Append a `SAY` `SpokenEntry`; mirrors into `DialogHistory` if a conversation is open. |
| `shout(speaker, text)` / `shout(text)` | Append a `SHOUT` `SpokenEntry` (bold + uppercase body). |
| `whisper(speaker, text)` / `whisper(text)` | Append a `WHISPER` `SpokenEntry` (italic + lowercase body). |
| `startConversation(participants...)` | Append a `ConversationStart` divider and call `history.beginDialog(...)` with an auto-generated id + default timestamp. Overloads accept a `GameClock` or an explicit `(dialogId, GameDateTime, participants...)`. |
| `endConversation()` / `endConversation(GameClock)` / `endConversation(GameDateTime)` | Append a `ConversationEnd` divider and call `history.endDialog(...)`. No-op when no conversation is open. |
| `clear()` | Clear the visible stack (the `DialogHistory` keeps its accumulated entries). |
| `goBack()` / `goForward()` | Move the cursor one entry; both clamp at the ends. |
| `setMaxVisibleEntries(int)` | Cap how many entries (current + previous) render at once. |
| `entries()` | Plain-text representation of each entry (useful for tests / serialization). |
| `dialogEntries()` | The structured `Entry` list. |
| `history()` | The owned (or caller-supplied via `new DialogEntriesView(history)`) `DialogHistory`. |
| `canGoBackProperty()` / `canGoForwardProperty()` | `ReadOnlyBooleanProperty` signals for greying out footer affordances. |
| `setSpeakerColumnWidth(double)` / `speakerColumnWidth()` | Configure / read the fixed speaker column width (default `DEFAULT_SPEAKER_COLUMN_WIDTH` = 160px). Bump it up for apps with longer speaker names; shrink it for compact layouts. |
| `bindToFooter(Node)` | Wire the standard `ScreenShell.footerBar()` back / forward labels to drive `goBack()` / `goForward()`. Accepts either the footer `HBox` itself or any ancestor that contains it. **Auto-called by `MainAppLayoutRenderer`** when the dialog slot is a `DialogEntriesView`; idempotent on repeat calls. |
| `bindHistoryToggle(Node, Node, double)` | Wire the footer history (◷) button so clicking it collapses the story slot and expands the dialog to full height. **Auto-called by `MainAppLayoutRenderer`** using `1.0 - plan.storyDialogRatio()`; idempotent on repeat calls. |
| `installKeyboardShortcuts(Scene)` | Install `Backspace` / `Space` event filters that mirror the default footer back / forward shortcuts. **Auto-installed** as soon as the view is added to a scene; idempotent on repeat calls. |

A typical wiring in the main app layout's dialog slot — the renderer auto-wires the footer
back/forward labels, the history (◷) toggle, and the Space/Backspace keyboard shortcuts, so the
application only has to build speakers, seed conversations, and hand the view to the resolver:

```java
DialogEntriesView dialog = new DialogEntriesView();
DialogSpeaker alice = DialogSpeaker.iconText("alice", "Alice", "alice-portrait", "#ffaaff");
DialogSpeaker bob   = DialogSpeaker.text("bob", "Bob");

dialog.startConversation(alice, bob);   // divider + history.beginDialog
dialog.say(alice, "Hello, Bob!");        // "Alice: Hello, Bob!"
dialog.shout(bob, "Stop!");              // "Bob: STOP!" (bold)
dialog.whisper(alice, "Don't tell anyone."); // "Alice: don't tell anyone." (italic)
dialog.endConversation();                // closes history; no visible end-divider

MainAppScreenResolver resolver = screenId -> switch (screenId) {
    case "story"  -> storyFactory.createNode();
    case "dialog" -> dialog;
    default       -> null;
};

StackPane root = MainAppLayoutRenderer.render(plan, resolver, resourceRoot);
// No need for bindToFooter / bindHistoryToggle / installKeyboardShortcuts — render() detects
// the DialogEntriesView in the dialog slot and wires all three automatically.
Scene scene = new Scene(root, 1280, 720);
```

Multi-line messages — both styles work out of the box. The body label has `wrapText` enabled, so
long single-line messages wrap naturally inside the dialog block; embedded `\n` characters render
as hard line breaks:

```java
// Hard line breaks (poem / multi-paragraph stanza):
dialog.say(narrator,
        "Sarah was already there.\n"
        + "Same chair, same window.\n"
        + "Half-hidden behind a tower of books.");

// Natural wrap (long sentence that will wrap to multiple lines based on the dialog width):
dialog.say(narrator,
        "It was a cold morning at the old library — the kind of morning where the radiators "
        + "clanked twice an hour and the dust in the slanted light moved like slow snow, "
        + "undisturbed by anyone who had not yet learned to be careful with quiet rooms.");
```

Speaker column width — the column is fixed-width so message bodies align across rows. The default
(`DEFAULT_SPEAKER_COLUMN_WIDTH` = 160px) suits most names; configure it when your cast has
unusually long labels (e.g. job titles, full names) or when you want a compact layout:

```java
DialogEntriesView dialog = new DialogEntriesView();
dialog.setSpeakerColumnWidth(220);   // wider column for "Old Mentor From Beyond the Hills"
// ...or for one-letter speaker tags:
dialog.setSpeakerColumnWidth(48);    // tight column when names are very short
```

History expansion (◷) — the footer's history button collapses the story slot and lets the dialog
block fill the entire centre, so the player can scroll through the full conversation record in one
panel. The renderer wires this automatically when the dialog slot is a `DialogEntriesView`. For
headless tests or non-`MAIN_APP_LAYOUT` containers you can drive it directly:

```java
// Programmatic: jump straight into the full-height history view.
dialog.setHistoryMode(true);
assert dialog.isHistoryMode();

// Manual wiring (skip when MainAppLayoutRenderer renders the dialog — already wired):
dialog.bindHistoryToggle(footerOrAncestor, storyNode, 1.0 - plan.storyDialogRatio());
```

Note: `say`/`shout`/`whisper` only persist into `DialogHistory` while a conversation is open
(between `startConversation` and `endConversation`). Calling them without an open conversation
still updates the visible widget; the history side is just skipped.

To tint each role differently — for example narrator / MC / book girl / random girl — construct
`DialogSpeaker`s with role-specific colours; the view will use that colour for both the speaker
label and the message body:

```java
DialogSpeaker narrator   = new DialogSpeaker("narrator",    "Narrator",   null, "#a0b0c0");
DialogSpeaker mc         = new DialogSpeaker("mc",          "Hero",       null, "#88ddff");
DialogSpeaker bookGirl   = new DialogSpeaker("book-girl",   "Sarah",      null, "#ffaaff");
DialogSpeaker randomGirl = new DialogSpeaker("random-girl", "Stranger",   null, "#aaffaa");
```

Style hooks (overridable in application stylesheets):

| Style class | Applied to |
|---|---|
| `.dialog-entries-view` | The outer `ScrollPane` — carries the 50% black background and the right-hand scrollbar styling |
| `.dialog-entries-container` | The inner `VBox` that actually holds the rendered entry nodes |
| `.dialog-entry` | Every entry node (label / row / divider) |
| `.dialog-entry-current` | The newest visible (bottom) entry — bright white, large font |
| `.dialog-entry-previous` | Earlier entries (faded to `0.5` opacity, smaller font) |
| `.dialog-entry-speaker` | The fixed-width speaker `Label` column inside a `SpokenEntry` — right-aligned so the name+colon sit flush against the body |
| `.dialog-entry-body` | The wrapping body `Label` inside a `SpokenEntry` |
| `.dialog-entry-say` / `.dialog-entry-shout` / `.dialog-entry-whisper` | Applied to the `SpokenEntry` row and to its body label — used for bold (shout) and italic (whisper) declarations |
| `.layout-main-app-dialog` | The dialog slot wrapper produced by the main app layout renderer; carries the default 50% black background |
| `.dialog-entry-divider` | The `HBox` for a `ConversationStart` / `ConversationEnd` |
| `.dialog-entry-divider-line` | The thin horizontal lines on either side of a divider |
| `.dialog-entry-divider-label` | The participants / "End" label in the middle of a divider |

#### Error screen (error and exception surface) <a id="error-screen-error-and-exception-surface"></a>

`com.eb.javafx.ui.ErrorScreen` is the engine's reusable screen for surfacing an exception or
non-fatal failure. It is styled in a dark-red theme so the player immediately recognises that
something has gone wrong, and it ships with a copyable details block plus distinct
**Continue** / **Exit** buttons.

The screen is built from an `ErrorScreen.Options` record:

| Field | Purpose |
|---|---|
| `title` | Heading shown at the top — typically the exception class name or a category string. Null or blank falls back to `ErrorScreen.DEFAULT_TITLE` ("Something went wrong"). |
| `message` | Optional short user-facing description; rendered as a single label above the details area. Null or blank hides the label. |
| `details` | The long technical body — full stack trace, validation report, etc. Rendered into a read-only, selectable monospace `TextArea` so the player can `Ctrl+A` / `Ctrl+C` the content. Null is normalised to the empty string. |
| `continueAction` | Invoked when the player clicks **Continue**. When `null`, the Continue button is hidden — signalling that the failure is fatal. |
| `exitAction` | Invoked when the player clicks **Exit**. Required — an error screen without an exit path would trap the player. |

API entry points (all in `com.eb.javafx.ui`):

| Method | Purpose |
|---|---|
| `ErrorScreen.createScene(Options, width, height)` | Build a `Scene` hosting the error screen at the requested dimensions. |
| `ErrorScreen.buildRoot(Options)` | Build only the `Parent` node so the caller can swap it into an existing `Scene`. |
| `ErrorScreen.Options.ofException(Throwable, exitAction)` | Convenience factory — populates `title` from the exception class name, `message` from `getMessage()`, and `details` from the full stack trace. Produces a fatal error (no Continue). |
| `ErrorScreen.Options.ofException(Throwable, continueAction, exitAction)` | Same as above with a Continue path — use for recoverable failures. |
| `ErrorScreen.stackTraceText(Throwable)` | Returns the throwable's stack trace as a single string. |

A typical fatal-error wiring at startup:

```java
try {
    bootstrap.run();
} catch (RuntimeException error) {
    ErrorScreen.Options options = ErrorScreen.Options.ofException(error, Platform::exit);
    Scene scene = ErrorScreen.createScene(options, 800, 600);
    scene.getStylesheets().add(uiTheme.stylesheet());
    primaryStage.setScene(scene);
    primaryStage.show();
}
```

For a recoverable failure (e.g. a save-slot read error), pass both actions:

```java
ErrorScreen.Options options = ErrorScreen.Options.ofException(
        error,
        () -> sceneRouter.open(SceneRouter.MAIN_MENU_ROUTE),
        Platform::exit);
```

Style hooks (all carried in the runtime theme generated by `UiTheme.stylesheet()` and in the
contract `default.css`):

| Style class | Applied to |
|---|---|
| `.error-screen` | The outer `BorderPane` — dark-red panel + 2px dark-red border |
| `.error-screen-title` | The heading `Label` (large bold light-red text) |
| `.error-screen-message` | The optional short message `Label` |
| `.error-screen-details` | The read-only monospace `TextArea` with the long technical body |
| `.error-screen-actions` | The `HBox` button row at the bottom |
| `.error-screen-copy-button` | The **Copy details** button (transparent + dark-red border) |
| `.error-screen-continue-button` | The **Continue** button (light-red outline, no fill) |
| `.error-screen-exit-button` | The **Exit** button (filled dark-red with white text) |

### Editing a screen manually in JSON

Manual JSON editing is the lowest-level authoring path. It is useful when a design is generated by tools, stored in source control, or adjusted outside the Swing designer. Start from one of the sample files in `examples/resources/json/screens/`:

- `sample-screen-design.json`
- `reloadable-test-screen.json`
- `main-menu-screen-design.json`
- `quest-log-screen-design.json`
- `gallery-preview-screen-design.json`

`main-menu-screen-design.json` includes the binding token `$chapterTitle`; pass a binding such as `Map.of("chapterTitle", "Chapter 3: Rooftop Garden")` to `ScreenDesignLayoutAdapter` when previewing or rendering that scaffold.

The top-level JSON shape is:

```json
{
  "id": "settings.profile",
  "title": "Profile Settings",
  "layoutType": "FORM",
  "metadata": {
    "description": "Example screen design document",
    "dialog": true,
    "dismissOnClickOutside": true,
    "dismissOnEscape": true
  },
  "blocks": [
    {
      "id": "profile",
      "title": "Profile",
      "layoutType": null,
      "parentBlockId": null,
      "conditions": ["profile.ready"],
      "styleClass": "profile-block",
      "metadata": {
        "borderStyle": "solid",
        "borderCorner": "rounded",
        "borderThickness": 1,
        "borderColor": "#4f86c6"
      },
      "items": [
        {
          "id": "profile.name",
          "type": "FIELD",
          "label": "Name",
          "text": null,
          "value": null,
          "defaultValue": "Player",
          "sequence": 10,
          "editable": true,
          "styleClass": null,
          "metadata": {
            "fontSize": "20",
            "fontStyle": "bold",
            "color": "#ffffff",
            "labelFontSize": "18",
            "labelFontStyle": "italic",
            "labelColor": "#66c1e0"
          }
        },
        {
          "id": "profile.difficulty",
          "type": "POPLIST",
          "label": "Difficulty",
          "text": null,
          "value": null,
          "defaultValue": null,
          "sequence": 20,
          "editable": false,
          "styleClass": null,
          "options": ["Easy", "Normal", "Hard"],
          "metadata": {}
        }
      ]
    }
  ]
}
```

Items are nested inside their parent block's `items` array. The `blockId` field is not required in the JSON — the parser derives it from the containing block. Selection items (`POPLIST`, `COMBO_BOX`, `RADIO_GROUP`) use a top-level `options` field containing a JSON string array instead of a metadata string. Known boolean and numeric metadata keys are written as JSON primitives (`true`/`false`, numbers), not quoted strings.

When editing manually:

- `id`, `title`, `layoutType`, and `blocks` are required at the top level
- items are placed inside their parent block's `items` array; `blockId` on each item is optional in JSON and is derived from the containing block when absent
- block ids and item ids must be unique across the entire document
- nested blocks use `parentBlockId`; root blocks leave it as `null`
- block `conditions` use the same `$name` / `${name}` marker style as conversation conditions and are preserved for application-owned visibility rules
- item `type` must be one of `TEXT`, `FIELD`, `MULTI_LINE_FIELD`, `TEXT_AREA`, `BUTTON`, `POPLIST`, `COMBO_BOX`, `SLIDER`, or `RADIO_GROUP`
- `label` is supported on all item types; for `FIELD`, `MULTI_LINE_FIELD`, `POPLIST`, `COMBO_BOX`, `SLIDER`, and `RADIO_GROUP` it is rendered above the control; for `BUTTON` it is the button caption
- `text` is the display content used by `TEXT` and `TEXT_AREA`
- `defaultValue` is the fallback text shown by `FIELD`, `MULTI_LINE_FIELD`, `POPLIST`, `COMBO_BOX`, `SLIDER`, and `RADIO_GROUP` when `value` is null
- `sequence` is an optional integer ordering hint for items within the same block
- `editable` can be written as a JSON boolean (`true`/`false`) or a quoted string; it is only meaningful for field-style items
- `styleClass` is a stable CSS hook; `metadata` is a string map for extra tool/renderer-owned values
- known numeric metadata keys (`borderThickness`, `transparency`, `backgroundImageTransparency`, `min`, `max`, `step`) can be written as JSON numbers; known boolean metadata keys (`showTicks`, `showLabels`, `dialog`, `dismissOnClickOutside`, `dismissOnEscape`) can be written as JSON booleans — quoted string forms are also accepted for backward compatibility
- `POPLIST`, `COMBO_BOX`, and `RADIO_GROUP` items carry a top-level `options` field holding a JSON string array, e.g. `["Easy", "Normal", "Hard"]`; legacy comma-separated strings in `metadata.options` are also accepted when loading older documents
- nested containers are represented as blocks with `parentBlockId`; a block-level `layoutType` controls how its child blocks are arranged
- `$name` and `${name}` tokens in titles, labels, text, values, default values, and metadata can be resolved by passing bindings to `ScreenDesignLayoutAdapter`
- `BUTTON` item metadata can include `eventName` or `actionEvent`; when rendered with a `GameEventBus`, clicking the button publishes that named event with the item id as the source id
- `ScreenDesignItem.options()` returns the decoded list of option strings for selection-type items; it reads from the canonical `options` metadata key regardless of whether the source was a JSON array or legacy CSV string

The designer and JSON format currently expose these style-oriented metadata keys:

- screen metadata: `fontFamily`, `fontSize`, `fontStyle`, `color`, `backgroundColor`, `borderStyle`, `borderCorner`, `borderThickness`, `borderColor`, `dialog`, `dismissOnClickOutside`, `dismissOnEscape`
- block metadata: `fontFamily`, `fontSize`, `fontStyle`, `color`, `backgroundColor`, `backgroundImage`, `backgroundImageTransparency`, `backgroundImagePlacement`, `transparency`, `borderStyle`, `borderCorner`, `borderThickness`, `borderColor`
- item metadata: `displayRole`, `fontFamily`, `fontSize`, `fontStyle`, `color`, `backgroundColor`, `transparency`, `labelFontFamily`, `labelFontSize`, `labelFontStyle`, `labelColor`, `eventName`, `actionEvent`; POPLIST/COMBO_BOX/RADIO_GROUP: top-level `options` field (JSON string array, e.g. `["A", "B", "C"]`); SLIDER: `min`, `max`, `step` (numeric), `showTicks`, `showLabels` (boolean); RADIO_GROUP: `orientation` (`horizontal` or `vertical`)

These keys are string-valued metadata entries in the saved JSON. Leave a key out to inherit the bundled default display configuration from `src/main/resources/com/eb/javafx/ui/display-defaults.json`, or from any edited preview defaults currently loaded in the screen designer.

`fontSize` accepts a number or JavaFX-style size token such as `20`, `20px`, `16pt`, or `1.2em`. `fontStyle` supports `normal`, `bold`, `italic`, and `bold italic`. `color`, `backgroundColor`, and `borderColor` accept simple JavaFX color tokens such as hex colors or named colors. `transparency` is stored as a value from `0` to `1`, where `0` is fully opaque and larger values increase transparency. `backgroundImage` can be a classpath resource path, file URI, or filesystem path; SVG sources are rasterized for preview/rendering. `backgroundImageTransparency` uses the same `0` to `1` scale as `transparency`, but applies only to the block background image layer. `backgroundImagePlacement` supports `fixed top left`, `fixed center`, `fixed bottom right`, or `stretch to fit`; the fixed modes keep the rasterized image at its own size and anchor it within the block, while `stretch to fit` fills the block bounds. `borderStyle` supports `solid`, `dashed`, `dotted`, or `none`; `borderCorner` supports `square`, `rounded`, or `pill`; `borderThickness` accepts the same kind of numeric tokens as `fontSize`. The generic layout renderer currently applies supported screen, block, and item metadata consistently in preview/rendered layout output, including rendering field-style items as JavaFX text inputs that honor the authored `editable` flag. Label-specific keys are intended for field-style items, are ignored by non-field items, and now style field labels in the generic renderer as well as application-owned rendering code.

To save and load manually-authored screen designs in code, use `ScreenDesignJson.load(path)` and `ScreenDesignJson.save(path, design)`. Save output intentionally excludes temporary preview items, so only the ordered `items` array is persisted in JSON.

### Example: screen and block background images

Screen background images are application-owned. A common pattern is to place a background region behind the engine-rendered layout and make the layout root transparent so the authored art stays visible:

```java
StackPane root = new StackPane();
Region screenBackground = ScreenShell.backgroundSvg(
        "/com/eb/javafx/images/svg/circle-background.svg",
        0.5,
        Color.web("#08111f"));
BorderPane content = ScreenLayoutRenderer.createRoot(model);
content.setStyle("-fx-background-color: transparent;");
if (content.getCenter() instanceof Region contentPanel) {
    contentPanel.setStyle("-fx-background-color: transparent;");
}
root.getChildren().addAll(screenBackground, content);
```

Block background images are metadata-driven. Add `backgroundImage`, `backgroundImageTransparency`, and (optionally) `backgroundImagePlacement` to the block metadata:

```json
{
  "id": "left-block",
  "title": "Left block",
  "layoutType": null,
  "parentBlockId": null,
  "conditions": [],
  "styleClass": "block-background-image-demo-section",
  "metadata": {
    "backgroundColor": "#203a67",
    "transparency": 0.15,
    "backgroundImage": "/com/eb/javafx/images/svg/circle2-background.svg",
    "backgroundImageTransparency": 0.5,
    "backgroundImagePlacement": "fixed center",
    "borderStyle": "solid",
    "borderCorner": "rounded",
    "borderThickness": 3,
    "borderColor": "#d7e7ff"
  }
}
```

Use `stretch to fit` when the image should fill the entire block. Use `fixed top left`, `fixed center`, or `fixed bottom right` when the image should keep its rasterized size and stay pinned to a specific anchor inside the clipped block.

### Editing a screen in the screen designer app

Launch the designer with:

```bash
./gradlew --no-daemon runScreenDesigner
```

The designer opens a Swing editor backed by `ScreenDesignModel` and starts in `examples/resources/json/screens`. Its main areas are:

- a navigation tree showing the screen root, blocks, saved items, and temporary items
- a properties panel for the currently selected screen, block, or item
- a live JSON view showing the current saved document
- toolbar actions for editing default display values, validation, jumping to the first validation issue, JavaFX preview, temporary-field creation, and temporary-item promotion
- an always-visible docked JavaFX live preview in a right-side third column that refreshes after property applies, create/load/save actions, and preview-default edits

Typical designer workflow:

1. Use **File > New**, **Load**, **Save**, or **Save As** to manage the JSON document.
   Saving a screen design also writes a sibling localized text file named after the screen JSON, such as
   `main-menu-screen-design_text.json`. The screen JSON stores stable text ids for screen titles, block titles,
   item labels, text areas, field defaults, and buttons; the `_text.json` file stores the actual fixed text under
   a `language` field and a `texts` map. Text values may include runtime lookup variables such as `$name` or
   `${player.name}`.
   Engine-owned runtime screens follow the same split using bundled files under
   `src/main/resources/com/eb/javafx/ui/screens`: every runtime screen has a screen JSON document and a sibling
   `_text.json` sidecar. Keep fixed player-facing screen text in those sidecars, keep fixed selectable value
   vocabularies such as footer display modes, theme choices, and slider position descriptions in code tables, and
   keep reusable dialog/status/error strings in the bundled `sys_message` code table. Manual management and
   authoring applications under `src/test/java/com/eb/javafx/testscreen` are diagnostics and may remain English.
   To avoid hardcoded runtime text, do not place player-facing English strings directly in reusable Java route or UI
   classes. Put screen-owned text such as titles, block labels, item labels, item body text, and field defaults in
   the screen JSON plus sibling `_text.json`; put fixed selectable vocabularies such as poplist values, theme names,
   and slider-position descriptions in code tables; and put reusable shared labels, empty states, status text, dialog
   support text, and generic errors in `sys_message`. Java should provide bindings, current values, event handling,
   and navigation only.
2. Use **Edit > Edit Default Values** to adjust the preview-time display defaults loaded from `display-defaults.json`; this is useful for testing inherited screen/block/item styling without changing every document node. The docked live preview refreshes after those defaults are saved.
3. Use **Add Block** to create root or nested blocks, or right-click a screen/block/item and choose a quick-add block action for common form, menu/action-list, or preview-grid sections.
4. Select a block, then use **Add Item** for a saved item or the tree context menu **Add Temporary Field** action for a preview-only field.
5. Select a tree node and edit its properties on the right.
6. Press **Apply Properties** to keep the selection on the current node while applying any renamed ids or moved items/blocks; the docked preview refreshes automatically.
7. Use **Edit > Validate** to run the screen design validator, or **Edit > Go To First Issue** to select the first affected screen, block, item, or temporary item. Validation messages also appear near the selected property panel and warning markers appear in the tree.
8. Drag blocks or items in the navigation tree to reorder siblings or move them into another compatible block.
9. Use **Edit > Open Preview** when you want a separate preview stage in addition to the docked live preview.
10. Use the temporary-item tree context menu **Promote Temporary** action to convert a preview-only field into a saved item when it should be persisted.

The management launcher also includes **Reloadable JSON Screen**, which opens the shared working-directory copy of `reloadable-test-screen.json` when one is present, otherwise it falls back to `examples/resources/json/screens/reloadable-test-screen.json`. Edit that JSON file in an external editor or the screen designer, then press **Reload JSON** in the test window to load the latest saved definition and immediately compare the rendered change. Relative screen resources are resolved from the active working directory, and when the screen designer is launched from the management app it shows that working directory above the navigation tree without changing it when other files are loaded.

The property editor adapts to the selected item type:

- screen nodes expose screen id, title, layout type, font family, font size/style, text color, background color, border attributes, dialog/dismiss preview hints, and an advanced metadata section for uncommon key/value pairs
- block nodes expose block id, title, layout type, parent block, style class, conditions, font family, font size/style, text color, background color, optional background image plus image transparency/placement, transparency, border attributes, and an advanced metadata section for uncommon key/value pairs
- field-style items expose block target, item id, style class, type, sequence, label, current value, editable, display role, item font family/size/style/color/background/hover-background/pressed-background/transparency, action event name/value metadata, label font family/size/style/color, and an advanced metadata section for uncommon key/value pairs
- non-field items hide label/editable controls and only expose the applicable content plus style class, sequence, display role, item font family/size/style/color/background/hover-background/pressed-background/transparency, action event name/value metadata, and advanced metadata
- `TEXT_AREA` and `MULTI_LINE_FIELD` use a multiline content editor

Temporary items are preview/test helpers. They render in the preview window and can be moved between blocks like saved items, but normal JSON save output omits them. This is useful for prototyping form rows or validation cases without committing them to the authored document.

Use **File > New From Template** to start from generic form, menu/action-list, or preview-grid structures seeded with reusable engine metadata and no game-specific content.

The navigation tree also supports context actions such as adding blocks/items, quick-adding common layout blocks, editing blocks/items, duplicating nodes, moving blocks/items up or down, copying/pasting style metadata, and removing nodes. Drag/drop can reparent nested blocks, move saved or temporary items between blocks, and reorder siblings. Removal is structural: deleting a block also removes nested child blocks and their items.

Reusable layout styling is split from layout intent. Engine renderers attach stable semantic style classes such as `layout-content`, `layout-sidebar`, `layout-main-content`, `layout-action-row`, `layout-primary-action`, `layout-secondary-action`, `layout-card`, `layout-form`, `layout-section-title`, and `layout-section-row`; `src/main/resources/com/eb/javafx/ui/default.css` provides the default colors, spacing, borders, and hover behavior. The layout contract can also be read as JSON from `src/main/resources/com/eb/javafx/ui/layout-contract.json` when tools or applications need a data-driven list of supported layout types and stable style hooks. Applications can use these screens directly for prototypes, add an application stylesheet after the engine stylesheet to override those hooks, or replace route factories with application-specific screens while keeping the same routing and bootstrap contracts. App-specific JavaFX controls, authored art, and game-specific visual rules should live in application route modules; reusable engine screens should consume view models or generic display contracts.

Example/demo code: [`examples/user-manual/06-ui-screens-and-themes/UiScreenDemo.java`](../examples/user-manual/06-ui-screens-and-themes/UiScreenDemo.java)

Additional example/demo code:
- [`examples/user-manual/06-ui-screens-and-themes/UiScreenCatalogDemo.java`](../examples/user-manual/06-ui-screens-and-themes/UiScreenCatalogDemo.java)
- [`examples/resources/json/screens/sample-screen-design.json`](../examples/resources/json/screens/sample-screen-design.json)
- [`examples/resources/json/screens/main-menu-screen-design.json`](../examples/resources/json/screens/main-menu-screen-design.json)
- [`examples/resources/json/screens/quest-log-screen-design.json`](../examples/resources/json/screens/quest-log-screen-design.json)
- [`examples/resources/json/screens/gallery-preview-screen-design.json`](../examples/resources/json/screens/gallery-preview-screen-design.json)

### Screen snapshot tool

The screen snapshot tool renders a screen JSON design to an image file without opening the interactive designer. It applies the same rendering pipeline as the designer's live preview — loads design and text sidecar, resolves display defaults from the active theme, renders via `ScreenLayoutRenderer.createPreviewRoot`, and applies the theme stylesheet — then writes the result to a PNG, JPEG, or BMP file.

```bash
./gradlew --no-daemon runScreenSnapshot \
    -Pscreen=<screen.json> \
    -Pout=<output-image> \
    [-Pwidth=N] [-Pheight=N]
```

| Parameter | Required | Description |
|-----------|----------|-------------|
| `-Pscreen` | yes | Path to the screen JSON design file |
| `-Pout` | yes | Output image path — file extension determines format |
| `-Pwidth` | no | Scene width in pixels (default: preferences window width, capped at 800) |
| `-Pheight` | no | Scene height in pixels (default: preferences window height, capped at 600) |

Supported output formats (by file extension): `.png` (default, preserves transparency), `.jpg` / `.jpeg` (alpha composited on white), `.bmp`.

Running the task without `-Pscreen` and `-Pout` prints usage help and exits cleanly.

Example — render the bundled main-menu design to a PNG:

```bash
./gradlew --no-daemon runScreenSnapshot \
    -Pscreen=examples/resources/json/screens/main-menu-screen-design.json \
    -Pout=out/main-menu.png
```

Example — render at a fixed size:

```bash
./gradlew --no-daemon runScreenSnapshot \
    -Pscreen=examples/resources/json/screens/main-menu-screen-design.json \
    -Pout=out/main-menu-800x600.png -Pwidth=800 -Pheight=600
```

The tool reads the user's saved theme preference from `java.util.prefs` so the snapshot reflects the same palette used when the designer or test screen was last run. To produce a snapshot with a specific theme, temporarily set the theme preference via the preferences screen before running the tool, or override the preference key `ui.themeFamily` / `ui.themeVariant` directly.

## 7. Display support

Use `ImageDisplayRegistry` as the central registry for reusable visual definitions. It can register and resolve:

- `ImageAssetDefinition` objects for authored image paths and display metadata
- `DisplayLayer` values for render ordering
- `DisplayTransform` values for placement, scale, opacity, render-order `zorder`, and other reusable transform data. The `zorder` defaults to `0`; use the 7-arg constructor or `withZorder(int)` to create a copy with a different value. Adapters are expected to sort render lists ascending by `zorder`.
- `DisplayTagDefinition` values mapping a semantic author tag to a concrete display ID, layer, optional transform preset, and render-order `zorder`. The 4-arg constructor defaults `zorder` to `0`; use the 5-arg constructor or `withZorder(int)` to copy with a new value.
- `LayeredCharacterDefinition` values for composed character displays
- `DisplayAnimation` and `DisplayAnimationStep` definitions, including authored ATL-style animation scripts

Use `DisplayAnimationPlayer` to model animation playback state and interpolation. `DisplayInterpolation` identifies supported interpolation behavior.

Authored animations can be loaded from display JSON through the `animations` root field. Each animation has a stable `id`, optional `repeatCount` (`1` by default or `"indefinite"`), optional `autoReverse`, and either explicit `steps` or a compact `script`. Script commands are line-oriented data only; they do not evaluate application code, filesystem paths, callbacks, or expressions. Supported commands are:

- `pause <durationMillis>`
- `fade <durationMillis> opacity <0..1> [linear|ease_in|ease_out|ease_both|discrete]`
- `move <durationMillis> translateX <x> translateY <y> [interpolation]`
- `scale <durationMillis> <uniformScale> [interpolation]`
- `scale <durationMillis> scaleX <x> scaleY <y> [interpolation]`
- `rotate <durationMillis> <degrees> [interpolation]`
- `rotate <durationMillis> rotate <degrees> [interpolation]`
- `clip <durationMillis> x <x> y <y> width <width> height <height> [interpolation]`
- `viewport <durationMillis> x <x> y <y> width <width> height <height> [interpolation]` for `ImageView` nodes
- `blur <durationMillis> <radius> [interpolation]`
- `blur <durationMillis> radius <radius> [interpolation]`
- `dropShadow <durationMillis> radius <radius> offsetX <x> offsetY <y> [interpolation]`
- `colorAdjust <durationMillis> [hue <-1..1>] [saturation <-1..1>] [brightness <-1..1>] [contrast <-1..1>] [interpolation]`
- `step <durationMillis> [pauseBefore <millis>] [opacity <0..1>] [scaleX <x>] [scaleY <y>] [translateX <x>] [translateY <y>] [rotate <degrees>] [clipX <x>] [clipY <y>] [clipWidth <width>] [clipHeight <height>] [viewportX <x>] [viewportY <y>] [viewportWidth <width>] [viewportHeight <height>] [blurRadius <radius>] [shadowRadius <radius>] [shadowOffsetX <x>] [shadowOffsetY <y>] [hue <-1..1>] [saturation <-1..1>] [brightness <-1..1>] [contrast <-1..1>] [interpolation]`

Standalone script resources can also be supplied through `animationScripts`, where each block starts with `animation <id>`, can include `repeat <count|indefinite>` and `autoreverse <true|false>`, and ends with `end`. The reusable JavaFX player also has room for capabilities that are not exposed in this ATL subset yet, including blend modes, cache/rendering hints, transform origins/pivots, 3D rotation axes and depth transforms, path motion, custom timelines, and event/callback hooks. Advanced Ren'Py ATL features such as arbitrary Python expressions, conditional blocks, events, callbacks, parallel composition, anchor math, and custom warpers are intentionally outside this reusable engine boundary for now.

The registry can resolve image paths from a checked-out game tree through `GameAssetLocator`, but concrete image assets remain application-owned.

Use `DisplayDefinitionJsonLoader` to load app-owned display JSON into an `ImageDisplayRegistry`, or wrap that loading in `JsonDisplayContentModule` for bootstrap registration. The supported root fields are `transforms`, `images`, `layeredCharacters`, `animations`, and `animationScripts`; authored image files and IDs remain outside the engine. Applications can store this JSON path under a named `ApplicationResourceConfig` resource such as `displayDefinitions`.

### Layered image composition

Use layered image composition when a character or display tag should be assembled from multiple conditional image layers at runtime — for example combining a base portrait with an expression layer and an outfit layer.

- `LayeredImageVariant` pairs an `imageRef` with an optional condition expression string. Condition expression is nullable; a null variant is the unconditional fallback for its layer.
- `LayeredImageLayer` groups an ordered list of `LayeredImageVariant` entries under a named layer. The first variant whose condition is satisfied is selected; if no conditioned variant matches, the first unconditioned variant is used.
- `LayeredImageDefinition` groups a stable `id`, a `displayTagId` (the display tag this composition targets), and an ordered list of `LayeredImageLayer` entries.
- `LayeredImageRegistry` stores definitions by `id` with `register(...)`, `find(id)`, and `require(id)`.
- `LayeredCompositionEntry` pairs a `layerName` with the resolved `imageRef` for that layer.
- `LayeredImageComposition` bundles the source `definitionId` with the resolved list of `LayeredCompositionEntry` values.
- `LayeredImageResolver.resolve(definition, conditionEval)` iterates the layers, picks the first matching variant per layer using a `Predicate<String>` condition evaluator, and returns a `LayeredImageComposition`. Layers with no matching variant produce no entry in the result.

```java
LayeredImageDefinition def = new LayeredImageDefinition(
    "hero",
    "tag_hero",
    List.of(
        new LayeredImageLayer("base", List.of(
            new LayeredImageVariant("hero_base.png", null))),
        new LayeredImageLayer("expression", List.of(
            new LayeredImageVariant("hero_smile.png", "flag:happy"),
            new LayeredImageVariant("hero_neutral.png", null)))));

LayeredImageComposition result = new LayeredImageResolver()
    .resolve(def, condition -> gameState.flags().contains(condition.replace("flag:", "")));
// result.entries() → [("base", "hero_base.png"), ("expression", "hero_smile.png")] or fallback
```

### Animation block grouping and event triggers

Use `AnimationBlock` to associate a `DisplayAnimation` with a semantic category and optional event trigger.

`AnimationBlockType` classifies how the block plays:

- `IDLE` — continuous looping animation played while a display tag is idle
- `TALKING` — mouth-sync animation played while the character speaks
- `EVENT` — one-shot animation fired in response to a scene event

`AnimationEventTrigger` names the scene event that fires an `EVENT` block:

- `SHOW` — display tag becomes visible
- `HIDE` — display tag is removed from the scene
- `CLICK` — display tag is clicked by the player

`AnimationBlock` stores a stable `id`, an `AnimationBlockType`, a `DisplayAnimation`, and an optional `AnimationEventTrigger`. Use `block.trigger()` to retrieve it as an `Optional<AnimationEventTrigger>`.

```java
AnimationBlock idleBlock = new AnimationBlock(
    "hero-idle", AnimationBlockType.IDLE, idleAnimation, null);

AnimationBlock showBlock = new AnimationBlock(
    "hero-show", AnimationBlockType.EVENT, fadeInAnimation, AnimationEventTrigger.SHOW);
```

`DisplayAnimation` now carries an optional `AnimationEventTrigger` alongside its existing fields:

- `animation.trigger()` returns `Optional<AnimationEventTrigger>`.
- `animation.withTrigger(trigger)` returns a copy with the given trigger; the original is unchanged.
- `DisplayAnimation.forTrigger(animations, trigger)` filters a list to those matching a specific trigger. Use it to find all animations that should fire when a display tag is shown, hidden, or clicked.

```java
DisplayAnimation fadeIn = new DisplayAnimation("fade-in", steps, 1, false)
    .withTrigger(AnimationEventTrigger.SHOW);

List<DisplayAnimation> showAnims =
    DisplayAnimation.forTrigger(allAnimations, AnimationEventTrigger.SHOW);
```

Example/demo code: [`examples/resources/json/display/display-definitions.demo.json`](../examples/resources/json/display/display-definitions.demo.json)

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

Additional example/demo code:
- [`examples/user-manual/08-audio-support/AudioAdapterPolicyDemo.java`](../examples/user-manual/08-audio-support/AudioAdapterPolicyDemo.java)

## 9. Game support, state, saves, preferences, and random behavior

### Game support

Use `GameSupportService` and `ActionRegistry` to register reusable `GameAction` objects. Actions can use `ActionRequirement` checks, `ActionEffect` outcomes, `ActionContext`, `RequirementResult`, and `ActionResult` to keep generic game-rule plumbing separate from authored domain rules.

Use `DefinitionRegistry` and the `IdentifiedDefinition` contract when an application needs a deterministic reusable registry with duplicate-ID checks. Use `GenericDescriptor` and `GenericDescriptorRegistry` for content-neutral descriptors that only need an id, kind, title, tags, and metadata before a dedicated domain type exists. Use `CodeTableDefinition` and `CodeDefinition` to define project-supplied code lists such as time slots, roles, goals, postures, positions, duties, listener types, fixed poplist choices, and slider positions with stable descriptions. `GameClock` and `GameDateTime` use a time-slot code table so reusable time progression does not embed a specific game calendar or schedule. Bundled `SystemCodeTables` also include UI support tables for `sys_message`, footer shortcut display modes, theme families, theme variants, and default volume levels; prefer `sys_message` for reusable dialog, status, and error messages that are not owned by one screen JSON sidecar.

Use `TimeScheduler`, `TimeScheduledCommand`, `TimeAdvanceHook`, and `TimeAdvanceService` when game time needs reusable advancement hooks and scheduled data-only commands. `TimeSaveSnapshots` serializes `GameDateTime` as a versioned `SaveSnapshotSection` named `gameTime`, leaving the outer save-file schema application-owned.

Use `LocationRegistry`, `LocationDescriptor`, and `LocationOccupancy` for reusable location metadata and per-save character placement. Location descriptors store dot-free local location IDs, optional dot-free map IDs, display/localization titles, route IDs, optional parent location paths, tags, and generic action IDs. Map-aware descriptors are referenced by their unique path, such as `main.lab.lobby` or `main.lab.lobby.bathroom.stall`, so the same local ID can be reused under different parents while each full path remains unique. `LocationRegistry.validateReferences(...)` checks parent-location and action references after static modules register definitions, while `LocationOccupancy` tracks where generic character IDs are currently placed without owning authored movement rules. Use `MovementValidator`, `MovementValidationResult`, and `LocationMovementService` to layer application-supplied movement checks over reusable occupancy changes.

Use `MapTextDefinition` for localized map labels. Load authored files with `MapTextDefinition.load(path)` or `loadResource(...)`, inspect/update them in memory, resolve a stored description with `mapDescription(mapId)`, and persist normalized output with `save(path)` or `toJson()`. The JSON root stores `language` and a `maps` array. Each map entry stores `mapId` and optional `description`; omitted descriptions default to `Main Map`.

```json
{
  "language": "en",
  "maps": [
    {"mapId": "town", "description": "Town Map"},
    {"mapId": "main"}
  ]
}
```

Use `LocationTextDefinition` for localized location descriptions within one map. Load authored files with `LocationTextDefinition.load(path)` or `loadResource(...)`, normalize them with `toJson()` / `save(path)`, and resolve descriptions either by `locId` with `locationDescription(...)` or by `mapId.locId` reference with `locationDescriptionByReference(...)`. When multiple variants exist, pass the active condition strings to select the first matching conditional text; otherwise the helper falls back to the first unconditional variant, then the first authored variant. The JSON root stores `language`, `mapId`, and a `locations` array. Each location stores `locId` and a `description` array of variants. Variants store `text` and optional string `conditions`, such as `time of day=night`; application code can resolve locations by `mapId.locId` references such as `town.square`.

```json
{
  "language": "en",
  "mapId": "town",
  "locations": [
    {
      "locId": "square",
      "description": [
        {"text": "The market square is busy.", "conditions": ["time of day=day"]},
        {"text": "The market square is quiet after dark.", "conditions": ["time of day=night"]},
        {"text": "The market square is open.", "conditions": []}
      ]
    }
  ]
}
```

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

### Generic support modules

Use the generic support packages when an application needs reusable game systems without moving authored content into the engine:

- `LocalizationService` and `LocalizedTextBundle` select a language, resolve stable text IDs, and report missing translations.
- `AssetCatalog` stores app-owned `AssetDefinition` entries with `AssetType`, preload hints, and deterministic `AssetValidationReport` / `AssetValidationProblem` diagnostics for missing files or paths that escape the configured asset root.
- `InputMap` stores context-scoped `InputAction` values and `InputBinding` trigger bindings. Triggers combine an `InputDevice` and `InputTrigger` value so menu, dialogue, gameplay, and debug controls can be rebindable without hard-coding UI controls.
- `GameEventBus` publishes lightweight runtime events and keeps a deterministic history for diagnostics, tests, or save-related inspection. Use `GameEventQueue` for FIFO deferred event processing, `GameEventListener` for listener adapters, and `GameCommandDispatcher` with `GameCommandHandler` for type-keyed command dispatch that can emit events back to the bus.
- `ProgressTracker`, `ProgressSupport`, and `ProgressSnapshotCodec` model reusable flags, counters, milestones, unlocks, action requirements/effects, and save snapshot sections.
- `InventoryCatalog` and `InventoryState` provide generic `InventoryItemDefinition` metadata and stack quantities while authored item data remains application-owned. Use `WearableSlotDefinition`, `WearableDefinition`, `WardrobeCatalog`, `OutfitState`, and `WardrobeState` for generic wearable slots, equipped item maps, unlocked wearables, and named outfit snapshots. `InventorySnapshotCodec` and `WardrobeSnapshotCodec` serialize those reusable state slices into application-owned save documents.
- `CharacterRegistry` and `RelationshipState` provide reusable `CharacterProfile` metadata and numeric relationship state. Use `CharacterTemplate`, `CharacterTemplateRegistry`, `CharacterStatBlock`, and `CharacterState` for template-level base stats plus per-save mutable stats, relationship values, flags, and metadata. `CharacterStatesSnapshotCodec` serializes per-save character state without owning application rules. `CharacterTemplate` exposes two metadata-backed accessors for animation wiring: `talkingAnimationId()` returns `Optional<String>` from the `"talkingAnimationId"` metadata key and is used by `SceneExecutor` to populate `SceneExecutionResult.talkingCue()` on dialogue steps; `idleAnimationId()` returns `Optional<String>` from the `"idleAnimationId"` metadata key for application-owned idle animation setup. Register these IDs in the template's `metadata` map; they are not record components.
- `NotificationState`, `Notification`, `MessageThreadState`, and `MessageEntry` model read/unread notifications and generic message threads without owning app-specific sender semantics or UI layout.
- `OrganizationDescriptor`, `ResourceLedger`, `ProductionOrder`, and `ProductionQueue` provide reusable organization metadata, non-negative resource balances, and tick-based production primitives.
- `JournalState` provides generic unlocked/read state for `JournalEntryDefinition` entries, with `JournalEntryStatus` recording whether each journal, quest, task, or log entry is unlocked and read. `JournalSnapshotCodec` serializes generic journal or quest read/unlocked state.
- `DiagnosticRegistry` combines `DiagnosticCheck` callbacks into `DiagnosticReport` values containing `DiagnosticProblem` rows and `DiagnosticSeverity` levels for startup or debug screens. `DiagnosticDescriptor`, `DebugPanelDescriptor`, and `ContentPackDescriptor` describe app-owned diagnostics, debug panels, and content packs using reusable metadata shapes.
- `SettingsStore` carries `SettingDefinition` entries, `SettingType` metadata, and runtime values above raw preferences. `AccessibilityProfile` carries accessibility choices such as font scale, contrast, motion, captions, and screen-reader labels.
- `TimelineSequence`, `TimelineStep`, and `TimelinePlayer` provide deterministic timing primitives with `TimelineStatus` state that can drive UI, display, text, or audio adapters.
- `DebugRegistry` collects `DebugSnapshot` values from `DebugInspector` callbacks for application-owned developer menus or test screens.
- `DebugScreenInspector` records the active route ID on each navigated scene and, when the `ApplicationResourceConfig.debug()` flag is enabled, wires the `Ctrl+D` (Cmd+D on macOS) shortcut so it opens a modal dialog showing the current screen name, screen class, and JSON source path in copyable text fields. Shortcut dispatch is performed by `ScreenShell.installFooterShortcuts(Scene, Map<String, Runnable>)` next to the other footer-shortcut configuration, so engine code and apps share one consistent shortcut-parsing pipeline (`ScreenShell.matchesShortcut(KeyEvent, String)` accepts `Ctrl`/`Cmd`/`Meta`, `Shift`, `Alt`/`Option`, plus named keys like `Space`, `Backspace`, `Tab`, `Enter`, `Escape`). Screens opt in to richer metadata by calling `DebugScreenInspector.setScreenClass(scene, MyScreen.class)` and `DebugScreenInspector.setJsonFilePath(scene, path)`; the route ID is populated automatically by `RouteContext.navigateTo` and `ApplicationShellSupport.openStartupRoute`. The dialog dismisses via the Close button (focused by default) or the Escape key.
- `ImportValidationReport`, `ImportValidationIssue`, `ImportIssueSeverity`, and `GenericValidation` provide shared helper types for import summaries, missing-reference diagnostics, and known-ID validation.

These modules intentionally store IDs, metadata, and reusable state only. Concrete content, progression rules, screen design, and save-file schemas should stay in application repositories.

Example/demo code:
- [`examples/user-manual/09-game-support-state-save-prefs-random/GenericSupportModulesDemo.java`](../examples/user-manual/09-game-support-state-save-prefs-random/GenericSupportModulesDemo.java)
- [`examples/user-manual/09-game-support-state-save-prefs-random/GenericStateSystemsDemo.java`](../examples/user-manual/09-game-support-state-save-prefs-random/GenericStateSystemsDemo.java)

### State

Use `GameStateFactory` to create base `GameState` instances. `GameState` currently stores the startup route and a reusable `DialogHistory` instance for per-save conversation review. Keep project-specific state fields and schemas in the application repository unless they are represented by reusable engine abstractions.

### Save/load

Use `SaveLoadService` for reusable save-slot workflows. It supports slot summaries and JSON persistence behavior suitable for engine-level tests and extension by application code. `SaveLoadSummaryScreen` and `SaveLoadSummaryViewModel` expose the current save schema version and configured save directory as reusable diagnostic UI data. Use `SaveSnapshotCodec` and `SaveSnapshotSection` when an application wants to compose engine-owned state slices, such as scene-flow progress, into its own save document; the application still owns the outer save schema and any project-specific state fields.

The following engine-provided `SaveSnapshotCodec` implementations are available for composition into application save documents:

- `SeenStepSnapshotCodec` — persists the set of scene step keys the player has already read (section id `seenSteps`, schema version 1). Used with `SeenStepTracker` for skip-mode support.
- `RollbackSnapshotCodec` — persists the scene flow positions stored in a `RollbackBuffer` (section id `rollback`, schema version 1). Construct with an optional capacity argument to control the maximum number of entries restored on load.

Use `ReusableGameplaySnapshot` and `ReusableGameplaySnapshotDocuments` for the reusable vertical-slice save contract: scene-flow state, scene checkpoint history, game time, generic progress, inventory, wardrobe, character state, journal/quest state, and location occupancy. The helper validates those required engine-owned sections while preserving additional application-owned sections for AltLife or other ports. Snapshot values such as `InventorySnapshot`, `WardrobeSnapshot`, `CharacterStatesSnapshot`, `JournalSnapshot`, and `LocationOccupancySnapshot` expose hydration helpers that rebuild the matching mutable engine state after an application validates its outer save document.

`SaveLoadService.SaveSchema` reports the current save schema version and directory, while `SaveLoadService.SaveSlotSummary` summarizes one slot number and whether it currently has data. Use `SaveSnapshotRegistry` to register required or optional snapshot sections and validate composed `SaveSnapshotDocument` objects. If an application needs to load older section payloads, register a `SaveSnapshotSectionMigration` so the registry can migrate sections to the current version during compose/decompose.

### Preferences

Use `PreferencesService` for user preferences such as window size, fullscreen state, footer label visibility, and master volume. Load preferences before services that depend on them, especially UI theme/window behavior and audio master volume. `PreferencesSummaryScreen` now focuses the reusable preferences route on the settings users can currently change directly: master/music/sound audio, theme color, and footer shortcut display, with changes applied immediately and a single Close action at the bottom. `PreferencesSummaryScreen` still builds a `PreferencesSummaryViewModel` with `PreferencesSummaryRowViewModel` entries so tests and reusable diagnostics can describe the editable preferences as labeled values. Call `ScreenShell.applyFooterPreferences(...)` when a screen footer should honor the user's selected footer shortcut display mode.

### Random

Use `GameRandomService` for reusable random behavior. Initialize it before use so tests and application code can rely on deterministic service state where applicable.

Example/demo code: [`examples/user-manual/09-game-support-state-save-prefs-random/SupportServicesDemo.java`](../examples/user-manual/09-game-support-state-save-prefs-random/SupportServicesDemo.java)

Additional example/demo code:
- [`examples/user-manual/09-game-support-state-save-prefs-random/CategoryCodeTableDefinitionDemo.java`](../examples/user-manual/09-game-support-state-save-prefs-random/CategoryCodeTableDefinitionDemo.java)
- [`examples/user-manual/09-game-support-state-save-prefs-random/GenericSupportModulesDemo.java`](../examples/user-manual/09-game-support-state-save-prefs-random/GenericSupportModulesDemo.java)
- [`examples/user-manual/09-game-support-state-save-prefs-random/GenericStateSystemsDemo.java`](../examples/user-manual/09-game-support-state-save-prefs-random/GenericStateSystemsDemo.java)
- [`examples/resources/json/code-tables/category-code-tables.demo.json`](../examples/resources/json/code-tables/category-code-tables.demo.json)
- [`examples/resources/json/location/map-text.demo.json`](../examples/resources/json/location/map-text.demo.json)
- [`examples/resources/json/location/location-text-town.demo.json`](../examples/resources/json/location/location-text-town.demo.json)

## 10. Text and utility helpers

Use `TextTagParser` to tokenize visual-novel-style text with simple styling metadata. Parsed output is represented through `TextToken`, `TextTokenType`, and `TextStyle`.

`TextTokenType` values and their corresponding inline tags:

| Type | Tag | Meaning |
|---|---|---|
| `TEXT` | (plain text) | Styled text span |
| `ICON` | `{icon=id}` | Inline image marker |
| `PAUSE` | `{w=2.5}` | Timed pause (duration in seconds) |
| `PARAGRAPH` | `{p}` | Paragraph break |
| `WAIT_CLICK` | `{w}` | Pause until player clicks to continue |
| `NO_WAIT` | `{nw}` | Suppress end-of-line pause; advance immediately |
| `SET_CPS` | `{cps=N}` | Set typewriter speed to N characters per second; read with `token.cps()` |
| `FAST_FORWARD` | `{fast}` | Skip remaining typewriter animation and show full text immediately |

Adapters receive these tokens in the parsed list alongside text and styling tokens. Pacing tokens (`WAIT_CLICK`, `NO_WAIT`, `SET_CPS`, `FAST_FORWARD`) are hints for the typewriter/animation layer; the engine does not act on them.

Use `JavaFxRichTextRenderer` when parsed tokens should be displayed directly in JavaFX. It renders text, paragraph, and inline `{icon=image.id}` tokens into a `TextFlow`, skips pause tokens for timeline/typewriter code to consume separately, and applies parsed `{gradient=...}`, `{kinetic=...}`, and `{glitch=...}` metadata to JavaFX `Text` nodes. Inline icons resolve through registered `ImageDisplayRegistry` image IDs, use a standard inline size/baseline offset, and fall back to plain `[image.id]` text when the image ID is unknown or the asset file is missing. Kinetic animations are attached to node properties by default so callers can start them when the JavaFX toolkit is active; use the autoplay constructor in live UI code when immediate playback is desired.

Use `TextEffect` and `StyledTextSpan` to carry rendering-neutral rich-text metadata such as gradient, kinetic, or glitch parameters in non-JavaFX adapters. Use `TextTemplateProcessor` with a `TextVariableResolver` for simple app-supplied `{variable}` replacement while preserving unknown markers.

Use `LocalizationTextExtractor` to collect scene dialogue, narration, and choice text definition IDs before checking `LocalizationService.missingTextIds(...)`, to create a skeleton `LocalizedTextBundle` where every ID maps to itself, or to turn parsed dialog messages back into plain source strings for application-owned translation export.

Use `DialogHistory` when a save needs reusable conversation review data. Start a dated `DialogHistoryEntry` with a `GameDateTime` or `GameClock`, append `DialogMessage` rows, and end it with a closing timestamp. Speaker messages use `DialogSpeaker` plus the standard message column, while multi-column dialog can store explicit `DialogColumn` values for app-owned renderers.

Use utility classes for common engine behavior:

- `Validation` for null, blank, positive, and unit-interval checks.
- `ImmutableCollections` for defensive immutable collection handling.
- `Result` for success/failure-style return values.
- `PathUtils` for repository-relative and asset-path helpers.
- `FontResources` for packaged engine fonts under `/com/eb/javafx/fonts`, including font filename discovery, validated resource paths, resource URLs/streams, and JavaFX `Font` loading.
- `TimeFormatting` for reusable time display formatting.
- `JsonStrings` for JSON string escaping and parsing quoted strings; `JsonStrings.ParsedString` reports the parsed value and end index for callers that parse larger documents.
- `SimpleJson` and `JsonData` for small engine-owned JSON configuration documents that need object, array, string, boolean, null, and numeric value handling without an additional dependency.
- `InitializationGuard` for fail-fast service initialization checks.
- `UtilConvert`, `UtilString`, `UtilUnicode`, and `ECharMappings` for small conversion, string, Unicode, and character-mapping helpers retained for reusable engine code.
- `UtilImage` for image conversion helpers used by reusable JavaFX/AWT bridge code.
- `UtilJavaFx.run(Runnable)` for executing work immediately on the JavaFX application thread or scheduling it with `Platform.runLater(...)` from a background thread.
- `VectorImage` and `VectorImage.ViewBox` for reusable SVG loading, metadata, sizing, styling, transform, export, and sanitization helpers.

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
