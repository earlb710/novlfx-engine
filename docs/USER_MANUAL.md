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

The management app is a button-only launcher for manual authoring and diagnostic screens, including default display values, the screen designer, the reloadable JSON screen, and the conversation editor. The **Default App Values** screen now also includes a **Locations** tab for reviewing and formatting bundled `map-text` and `location-text` JSON examples while comparing them with other startup defaults.

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
- `debug`: reusable debug snapshot, panel descriptor, and inspector models for app-owned developer tools.
- `diagnostics`: structured health-check problems, reports, check descriptors, and check registries.
- `display`: image assets, display layers, transforms, layered characters, JSON definition loading, interpolation, and animation playback.
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
- `scene`: scene definitions, JSON import/export, steps, choices, transitions, execution results, flow-state JSON snapshots, and view models.
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

Use `GlobalApiAdapter` when legacy or app-owned code needs a narrow bridge for common global-style operations. It accepts `GlobalRouteRequest` / `GlobalRouteAction` navigation requests, delegates screen visibility to the router/stage layer, delegates sound requests to `AudioService`, and uses `GameRandomService` for reusable random decisions without reintroducing application globals.

Inspect the `BootstrapReport` when startup diagnostics matter. It records completed phases and phase messages so an application can display useful failure or progress information. Use `BootstrapDiagnostics.requireComplete(...)` when an application launcher should fail fast on incomplete startup, or use `BootstrapDiagnostics.viewModel(...)` / `phaseLines(...)` to render content-neutral startup summaries in an app-owned startup or error screen.

Use `BootstrapCompletenessPolicy` when an application has additional reusable startup requirements beyond all phases completing. A policy can require specific `BootstrapPhase` values, route IDs, and content definition IDs, then produce a `BootstrapCompletenessReport` with `BootstrapCompletenessProblem` entries for startup UI or throw through `requireComplete(...)`. `BootstrapDiagnostics.viewModel(...)` returns a `BootstrapDiagnosticsViewModel` with `BootstrapPhaseSummaryViewModel` rows for rendering startup phase status. `ApplicationShellOptions` configures startup route and window-preference behavior, while `ApplicationShellSupport` combines that policy check with content-neutral startup-route opening and optional window-size preference persistence; the application still owns the JavaFX `Application` subclass and concrete route modules.

Do not use guarded services before initialization. Several services use initialization guards and will fail fast if called before bootstrap or explicit initialization.

Applications can also keep an external `config.json` and load it with `ApplicationResourceConfig.load(Path)` when authored resources need to live outside the engine defaults. The config stores a category code-table JSON path, an image asset root, optional default background values for the app, preferences, and save/load screens, and a generic `resources` map for other overrideable files such as themes or image groups:

```json
{
  "categoryCodeTablesPath": "config/category-code-tables.en.json",
  "imageAssetRoot": "game",
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
Use the route-specific background getters when the application wants shared startup defaults for app-owned shell, preferences, or save/load screen backgrounds.

The management UI includes a **Default App Values** screen for inspecting these startup defaults and related display resources. The **Application Values** tab presents editable application config fields with friendly labels, local **Save** and **Reset** actions, browse buttons for file/path-backed fields, and color pickers for color values.

Next to that, the **Locations** tab embeds two JSON editors for the bundled `map-text.demo.json` and `location-text-town.demo.json` examples. Each editor supports local **Save**, **Format**, and **Reset** actions; **Save** and **Format** validate the JSON through `MapTextDefinition` or `LocationTextDefinition` and rewrite the editor with the engine formatter, while **Reset** restores the staged sample content for the current management-session tab. Use this tab to experiment with localized map labels, per-map location descriptions, and conditional location variants before moving the JSON into an application-owned content tree.

Immediately after that, the **Lookup Variables** tab opens an editable text variable catalog for this management screen session. The catalog uses two fields per row: `name` and `value type`. The value type is limited to `string`, `number`, or `boolean`; use **Add Variable** to append a blank row and **Remove Variable** to delete selected rows, or the last row when nothing is selected. Use the tab-level **Save** and **Reset** buttons to apply or restore the staged catalog rows locally while reviewing startup defaults.

Beneath those fields, the **Application Variables** block provides a multiline table for app-owned variable notes or future app-specific persistence with four fields: `name`, `type`, `value`, and `description`. The type field is limited to `string`, `number`, or `bool`; use **Add Variable** to append a blank row and **Remove Variable** to delete selected rows, or the last row when nothing is selected.

Below that, the **Load Files** block provides a second table for tracking authored startup loads with three fields: `type`, `path`, and `file name`. The type field is limited to `code table` or `conversation`; use **Add Load** to append a blank row and **Remove Load** to delete selected rows, or the last row when nothing is selected. Leave `file name` empty when the intent is to load every file in the specified directory.

```json
{
  "debug": true,
  "categoryCodeTablesPath": "config/category-code-tables.en.json",
  "imageAssetRoot": "game",
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
- [`examples/user-manual/04-startup-and-service-wiring/config.demo.json`](../examples/user-manual/04-startup-and-service-wiring/config.demo.json)

## 5. Content, routing, and scenes

Example/demo code: [`examples/user-manual/05-content-routing-and-scenes/SceneFlowDemo.java`](../examples/user-manual/05-content-routing-and-scenes/SceneFlowDemo.java)

Additional example/demo code:
- [`examples/user-manual/05-content-routing-and-scenes/SceneExecutionAndJsonDemo.java`](../examples/user-manual/05-content-routing-and-scenes/SceneExecutionAndJsonDemo.java)
- [`examples/user-manual/05-content-routing-and-scenes/SceneValidationAndSaveDemo.java`](../examples/user-manual/05-content-routing-and-scenes/SceneValidationAndSaveDemo.java)
- [`examples/user-manual/05-content-routing-and-scenes/scene-definitions.demo.json`](../examples/user-manual/05-content-routing-and-scenes/scene-definitions.demo.json)

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

Use `SceneExecutor` to execute scene flow and return a `SceneExecutionResult` with `SceneExecutionStatus`. Use `ScenePresenter` and view-model classes when JavaFX UI code needs a UI-neutral representation of the current scene and choices.

Use `SceneRegistry.validationReport(...)` when application startup needs diagnostics rather than fail-fast validation. The returned `SceneValidationReport` contains `SceneGraphSummary` entries plus `SceneValidationProblem` diagnostics with `SceneValidationSeverity` levels for duplicate IDs, missing jump/call targets, unreachable steps, and final-step `NEXT` warnings. Applications can pass `SceneReferenceValidator` instances such as `SceneReferenceValidators.knownSpeakers(...)` or `knownDisplayReferences(...)` to validate app-owned speaker/display IDs without moving those registries into the engine.

### Scene presentation view models

Scene view models are deliberately UI-neutral. They are useful when the engine needs to expose scene execution state to JavaFX screens, tests, debug panels, or application-owned renderers without passing mutable executor objects or JavaFX controls across package boundaries. Use `ScenePresenter` to convert a `SceneExecutionResult` into these models after each scene execution step or choice selection.

- `SceneViewModel` is the top-level scene presentation state. It carries the current execution status, scene id, step id, speaker id, text definition id, display reference, choices, selected choice history, message, dialogue rows, status rows, and effect previews. Use it as the single object handed to a scene renderer such as `SceneFlowView` so the renderer can display scene progress without knowing how scene execution works.
- `SceneChoiceViewModel` represents one rendered choice after availability has already been evaluated. It includes the choice id, returned choice value, choice text definition id, whether the choice is available, an optional disabled reason, whether it was selected earlier, string metadata, and effect previews. Use it to build choice buttons or test choice state without re-running requirement checks in the UI layer.
- `SceneDialogueRowViewModel` represents one dialogue or narration row. It includes the step type, optional speaker id, text definition id, and optional display reference. Use it when a renderer needs a normalized row for dialogue panels instead of inspecting raw `SceneStep` objects.
- `SceneStatusRowViewModel` represents one label/value diagnostic row such as status, active scene, active step, selected choices, pending interruption, or message. Use it for HUD, debug, and manual-test surfaces where scene execution state should be visible in a consistent format.
- `SceneEffectPreviewViewModel` represents preview-only metadata for a scene step or choice. It contains a label and value derived from `preview.*` metadata or a fallback reference. Use it when a prototype UI wants to show what display, effect, or authored metadata would be applied before an application supplies custom rendering.

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
- `ScreenLayoutType`, `ScreenLayoutModel`, and `ScreenLayoutSection` define reusable screen layout intent without JavaFX control state. Use them when a screen needs a stable general structure such as a titled panel, two-column layout, sidebar/content layout, HUD/status overlay, dialogue surface, menu/action list, form, or preview/card grid.
- `ScreenDesignModel`, `ScreenDesignBlock`, and `ScreenDesignItem` define editable JSON-backed screen designs with stable screen, block, and item ids. Use `ScreenDesignService.addItemToBlock(...)` or `addTemporaryItemToBlock(...)` when code needs to target a block id directly; temporary items render in preview/test mode but `ScreenDesignJson.save(...)` excludes them from persisted JSON. `ScreenDesignJson` saves/loads documents with top-level `id`, `title`, `layoutType`, `metadata`, ordered `blocks`, and ordered saved `items`. Each block carries `id`, optional `title`, optional block-level `layoutType`, optional `parentBlockId`, optional `styleClass`, and `metadata`; each item carries `id`, `blockId`, `type`, optional `label`, `text`, `value`, `defaultValue`, `styleClass`, and `metadata`.
- `ConversationDefinition`, `ConversationDefinitionJson`, and `JsonConversationContentModule` define JSON-backed conversation documents for authored visual-novel content using the AltLife exported shape. A conversation file has top-level `name`, `language`, and ordered `conversations`; each conversation carries `id`, `description`, and typed `lines`. Line `type` supports `say` by default, `shout` for uppercase bold text, `whisper` for lowercase italic text, and `choice` for player-selectable variants with per-choice values and conditions. `JsonConversationContentModule` projects that document into reusable content definitions and scene definitions when runtime registration is needed.
- `ScreenLayoutContract` loads the machine-readable layout contract from `src/main/resources/com/eb/javafx/ui/layout-contract.json`, which lists engine-provided layout types, the default stylesheet, and stable CSS style hooks applications can target.
- `ScreenInventory`, `ScreenInventoryItem`, `ScreenInventorySource`, `ScreenInventoryScanner`, and `ScreenInventoryAssignmentCategory` provide content-neutral inventory models for application-owned screen/style/control migration scanners. Use them to classify source artifacts as route-backed, reusable-control-backed, deferred, deprecated, excluded, or app-owned without hard-coding source-engine names in the engine.
- `ViewModelScreen` renders a `ScreenViewModel` with generic labels and navigation buttons.
- `ScreenLayoutRenderer` renders a `ScreenLayoutModel` into JavaFX nodes. It keeps route screens thin by letting them gather data, build a UI-neutral model, and delegate JavaFX node creation plus style-class assignment to the shared renderer.
- `ScreenShell` wraps screen content in a consistent shell. Its reusable footer bar is shown by default on titled shells and exposes helpers for visibility, transparency, compact/mobile presentation, icon-only label mode, localized labels/tooltips, and per-option enabled state. Use `FooterOption` ids such as `back`, `history`, `save`, `quick-save`, and `preferences` when application state, localization bundles, or preferences need to customize a specific footer control. Default footer functions also expose standalone SVG icon resources under `src/main/resources/com/eb/javafx/images/icons/` using names like `footer-back.svg`, `footer-save.svg`, and `footer-preferences.svg`; `icons-10x10.svg` remains available in the same directory as the source icon sheet/reference.
- `ScreenNavigation` centralizes navigation callbacks.
- `PreviewSummaryView` creates simple titled preview panels for display, scene, and snapshot summaries.
- `MainMenuScreen`, `SceneFlowScreen`, `DisplayBindingsScreen`, `HudSummaryScreen`, `SaveLoadSummaryScreen`, `PreferencesSummaryScreen`, and `ConversationHistoryScreen` provide generic reusable screens or screen models.
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
- `TEXT` and `TEXT_AREA` are read-only display content and do not keep a label
- `FIELD` and `MULTI_LINE_FIELD` support `label`, `value`, `defaultValue`, and `editable`
- `BUTTON` uses `label` as the rendered button caption

`ScreenDesignLayoutAdapter` converts a `ScreenDesignModel` into a `ScreenLayoutModel` for preview or runtime rendering. It preserves stable block/item ids, converts `parentBlockId` relationships into nested layout sections, maps field-style items to `label: value/defaultValue` lines, sorts block items by optional `sequence` before falling back to authored JSON order, and carries item/block metadata into the layout so renderer-supported visual metadata can be applied consistently. Block `conditions` are preserved in section metadata as a JSON string array, and applications can call the binding overload with a string map so authored text such as `$playerName` or `${playerName}` is resolved during scaffolding. Complex or application-specific controls can still be added programmatically by targeting stable block ids after the JSON scaffold is loaded.

### Editing a screen manually in JSON

Manual JSON editing is the lowest-level authoring path. It is useful when a design is generated by tools, stored in source control, or adjusted outside the Swing designer. Start from one of the sample files in `examples/screen-designs/`:

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
    "dialog": "true",
    "dismissOnClickOutside": "true",
    "dismissOnEscape": "true"
  },
  "blocks": [
    {
      "id": "profile",
      "title": "Profile",
      "layoutType": null,
      "parentBlockId": null,
      "conditions": ["profile.ready"],
      "styleClass": "profile-block",
      "metadata": {}
    }
  ],
  "items": [
    {
      "id": "profile.name",
      "blockId": "profile",
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
    }
  ]
}
```

When editing manually:

- `id`, `title`, `layoutType`, and `blocks` are required at the top level
- `items` may be empty, but every saved item must point to a valid `blockId`
- block ids and item ids must be unique
- nested blocks use `parentBlockId`; root blocks leave it as `null`
- block `conditions` use the same `$name` / `${name}` marker style as conversation conditions and are preserved for application-owned visibility rules
- item `type` must be one of `TEXT`, `FIELD`, `MULTI_LINE_FIELD`, `TEXT_AREA`, or `BUTTON`
- `label` is only meaningful for `FIELD`, `MULTI_LINE_FIELD`, and `BUTTON`
- `text` is used by `TEXT` and `TEXT_AREA`
- `defaultValue` is the fallback text shown by `FIELD` and `MULTI_LINE_FIELD` when `value` is null
- `sequence` is an optional integer ordering hint for items within the same block
- `editable` is only meaningful for field-style items
- `styleClass` is a stable CSS hook; `metadata` is a string map for extra tool/renderer-owned values
- nested containers are represented as blocks with `parentBlockId`; a block-level `layoutType` controls how its child blocks are arranged
- `$name` and `${name}` tokens in titles, labels, text, values, default values, and metadata can be resolved by passing bindings to `ScreenDesignLayoutAdapter`
- `BUTTON` item metadata can include `eventName` or `actionEvent`; when rendered with a `GameEventBus`, clicking the button publishes that named event with the item id as the source id

The designer and JSON format currently expose these style-oriented metadata keys:

- screen metadata: `fontFamily`, `fontSize`, `fontStyle`, `color`, `backgroundColor`, `borderStyle`, `borderCorner`, `borderThickness`, `borderColor`, `dialog`, `dismissOnClickOutside`, `dismissOnEscape`
- block metadata: `fontFamily`, `fontSize`, `fontStyle`, `color`, `backgroundColor`, `backgroundImage`, `backgroundImageTransparency`, `backgroundImagePlacement`, `transparency`, `borderStyle`, `borderCorner`, `borderThickness`, `borderColor`
- item metadata: `displayRole`, `fontFamily`, `fontSize`, `fontStyle`, `color`, `backgroundColor`, `transparency`, `labelFontFamily`, `labelFontSize`, `labelFontStyle`, `labelColor`, `eventName`, `actionEvent`

These keys are string-valued metadata entries in the saved JSON. Leave a key out to inherit the bundled default display configuration from `src/main/resources/com/eb/javafx/ui/display-defaults.json`, or from any edited preview defaults currently loaded in the screen designer.

`fontSize` accepts a number or JavaFX-style size token such as `20`, `20px`, `16pt`, or `1.2em`. `fontStyle` supports `normal`, `bold`, `italic`, and `bold italic`. `color`, `backgroundColor`, and `borderColor` accept simple JavaFX color tokens such as hex colors or named colors. `transparency` is stored as a value from `0` to `1`, where `0` is fully opaque and larger values increase transparency. `backgroundImage` can be a classpath resource path, file URI, or filesystem path; SVG sources are rasterized for preview/rendering. `backgroundImageTransparency` uses the same `0` to `1` scale as `transparency`, but applies only to the block background image layer. `backgroundImagePlacement` supports `fixed top left`, `fixed center`, `fixed bottom right`, or `stretch to fit`; the fixed modes keep the rasterized image at its own size and anchor it within the block, while `stretch to fit` fills the block bounds. `borderStyle` supports `solid`, `dashed`, `dotted`, or `none`; `borderCorner` supports `square`, `rounded`, or `pill`; `borderThickness` accepts the same kind of numeric tokens as `fontSize`. The generic layout renderer currently applies supported screen, block, and item metadata consistently in preview/rendered layout output. Label-specific keys are intended for field-style items, are ignored by non-field items, and are available for future renderer support or application-owned rendering code that wants label-specific styling.

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
    "transparency": "0.15",
    "backgroundImage": "/com/eb/javafx/images/svg/circle2-background.svg",
    "backgroundImageTransparency": "0.5",
    "backgroundImagePlacement": "fixed center",
    "borderStyle": "solid",
    "borderCorner": "rounded",
    "borderThickness": "3",
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

The designer opens a Swing editor backed by `ScreenDesignModel` and starts in `examples/screen-designs`. Its main areas are:

- a navigation tree showing the screen root, blocks, saved items, and temporary items
- a properties panel for the currently selected screen, block, or item
- a live JSON view showing the current saved document
- toolbar actions for editing default display values, validation, JavaFX preview, temporary-field creation, and temporary-item promotion

Typical designer workflow:

1. Use **File > New**, **Load**, **Save**, or **Save As** to manage the JSON document.
   Saving a screen design also writes a sibling localized text file named after the screen JSON, such as
   `main-menu-screen-design_text.json`. The screen JSON stores stable text ids for screen titles, block titles,
   item labels, text areas, field defaults, and buttons; the `_text.json` file stores the actual fixed text under
   a `language` field and a `texts` map. Text values may include runtime lookup variables such as `$name` or
   `${player.name}`.
2. Use **Edit Default Values** to adjust the preview-time display defaults loaded from `display-defaults.json`; this is useful for testing inherited screen/block/item styling without changing every document node.
3. Use **Add Block** to create root or nested blocks.
4. Select a block, then use **Add Item** for a saved item or **Add Temporary Field** for a preview-only field.
5. Select a tree node and edit its properties on the right.
6. Press **Apply Properties** to keep the selection on the current node while applying any renamed ids or moved items/blocks.
7. Use **Validate** to run the screen design validator.
8. Use **Open Preview** to render the current design through `ScreenLayoutRenderer` with the engine theme and the currently edited default display values.
9. Use **Promote Temporary** to convert a temporary field into a saved item when it should be persisted.

The management launcher also includes **Reloadable JSON Screen**, which opens `examples/screen-designs/reloadable-test-screen.json` as a live `ScreenLayoutRenderer` view. Edit that JSON file in an external editor or the screen designer, then press **Reload JSON** in the test window to load the latest saved definition and immediately compare the rendered change.

The property editor adapts to the selected item type:

- screen nodes expose screen id, title, layout type, font family, font size/style, text color, background color, border attributes, dialog/dismiss preview hints, and an extra metadata editor for non-exposed keys
- block nodes expose block id, title, layout type, parent block, style class, conditions, font family, font size/style, text color, background color, optional background image plus image transparency/placement, transparency, border attributes, and an extra metadata editor for non-exposed keys
- field-style items expose block target, item id, style class, type, sequence, label, current value, editable, display role, item font family/size/style/color/background/transparency, label font family/size/style/color, and an extra metadata editor for non-exposed keys
- non-field items hide label/editable controls and only expose the applicable content plus style class, sequence, display role, item font family/size/style/color/background/transparency, and extra metadata
- `TEXT_AREA` and `MULTI_LINE_FIELD` use a multiline content editor

Temporary items are preview/test helpers. They render in the preview window and can be moved between blocks like saved items, but normal JSON save output omits them. This is useful for prototyping form rows or validation cases without committing them to the authored document.

The navigation tree also supports context actions such as adding blocks/items, editing blocks/items, and removing nodes. Removal is structural: deleting a block also removes nested child blocks and their items.

Reusable layout styling is split from layout intent. Engine renderers attach stable semantic style classes such as `layout-content`, `layout-sidebar`, `layout-main-content`, `layout-action-row`, `layout-primary-action`, `layout-secondary-action`, `layout-card`, `layout-form`, `layout-section-title`, and `layout-section-row`; `src/main/resources/com/eb/javafx/ui/default.css` provides the default colors, spacing, borders, and hover behavior. The layout contract can also be read as JSON from `src/main/resources/com/eb/javafx/ui/layout-contract.json` when tools or applications need a data-driven list of supported layout types and stable style hooks. Applications can use these screens directly for prototypes, add an application stylesheet after the engine stylesheet to override those hooks, or replace route factories with application-specific screens while keeping the same routing and bootstrap contracts. App-specific JavaFX controls, authored art, and game-specific visual rules should live in application route modules; reusable engine screens should consume view models or generic display contracts.

Example/demo code: [`examples/user-manual/06-ui-screens-and-themes/UiScreenDemo.java`](../examples/user-manual/06-ui-screens-and-themes/UiScreenDemo.java)

Additional example/demo code:
- [`examples/user-manual/06-ui-screens-and-themes/UiScreenCatalogDemo.java`](../examples/user-manual/06-ui-screens-and-themes/UiScreenCatalogDemo.java)
- [`examples/screen-designs/sample-screen-design.json`](../examples/screen-designs/sample-screen-design.json)
- [`examples/screen-designs/main-menu-screen-design.json`](../examples/screen-designs/main-menu-screen-design.json)
- [`examples/screen-designs/quest-log-screen-design.json`](../examples/screen-designs/quest-log-screen-design.json)
- [`examples/screen-designs/gallery-preview-screen-design.json`](../examples/screen-designs/gallery-preview-screen-design.json)

## 7. Display support

Use `ImageDisplayRegistry` as the central registry for reusable visual definitions. It can register and resolve:

- `ImageAssetDefinition` objects for authored image paths and display metadata
- `DisplayLayer` values for render ordering
- `DisplayTransform` values for placement, scale, opacity, and other reusable transform data
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

Additional example/demo code:
- [`examples/user-manual/08-audio-support/AudioAdapterPolicyDemo.java`](../examples/user-manual/08-audio-support/AudioAdapterPolicyDemo.java)

## 9. Game support, state, saves, preferences, and random behavior

### Game support

Use `GameSupportService` and `ActionRegistry` to register reusable `GameAction` objects. Actions can use `ActionRequirement` checks, `ActionEffect` outcomes, `ActionContext`, `RequirementResult`, and `ActionResult` to keep generic game-rule plumbing separate from authored domain rules.

Use `DefinitionRegistry` and the `IdentifiedDefinition` contract when an application needs a deterministic reusable registry with duplicate-ID checks. Use `GenericDescriptor` and `GenericDescriptorRegistry` for content-neutral descriptors that only need an id, kind, title, tags, and metadata before a dedicated domain type exists. Use `CodeTableDefinition` and `CodeDefinition` to define project-supplied code lists such as time slots, roles, goals, postures, positions, duties, or listener types. `GameClock` and `GameDateTime` use a time-slot code table so reusable time progression does not embed a specific game calendar or schedule.

Use `TimeScheduler`, `TimeScheduledCommand`, `TimeAdvanceHook`, and `TimeAdvanceService` when game time needs reusable advancement hooks and scheduled data-only commands. `TimeSaveSnapshots` serializes `GameDateTime` as a versioned `SaveSnapshotSection` named `gameTime`, leaving the outer save-file schema application-owned.

Use `LocationRegistry`, `LocationDescriptor`, and `LocationOccupancy` for reusable location metadata and per-save character placement. Location descriptors store stable location IDs, display/localization titles, route IDs, optional parent locations, tags, and generic action IDs. `LocationRegistry.validateReferences(...)` checks parent-location and action references after static modules register definitions, while `LocationOccupancy` tracks where generic character IDs are currently placed without owning authored movement rules. Use `MovementValidator`, `MovementValidationResult`, and `LocationMovementService` to layer application-supplied movement checks over reusable occupancy changes.

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
- `CharacterRegistry` and `RelationshipState` provide reusable `CharacterProfile` metadata and numeric relationship state. Use `CharacterTemplate`, `CharacterTemplateRegistry`, `CharacterStatBlock`, and `CharacterState` for template-level base stats plus per-save mutable stats, relationship values, flags, and metadata. `CharacterStatesSnapshotCodec` serializes per-save character state without owning application rules.
- `NotificationState`, `Notification`, `MessageThreadState`, and `MessageEntry` model read/unread notifications and generic message threads without owning app-specific sender semantics or UI layout.
- `OrganizationDescriptor`, `ResourceLedger`, `ProductionOrder`, and `ProductionQueue` provide reusable organization metadata, non-negative resource balances, and tick-based production primitives.
- `JournalState` provides generic unlocked/read state for `JournalEntryDefinition` entries, with `JournalEntryStatus` recording whether each journal, quest, task, or log entry is unlocked and read. `JournalSnapshotCodec` serializes generic journal or quest read/unlocked state.
- `DiagnosticRegistry` combines `DiagnosticCheck` callbacks into `DiagnosticReport` values containing `DiagnosticProblem` rows and `DiagnosticSeverity` levels for startup or debug screens. `DiagnosticDescriptor`, `DebugPanelDescriptor`, and `ContentPackDescriptor` describe app-owned diagnostics, debug panels, and content packs using reusable metadata shapes.
- `SettingsStore` carries `SettingDefinition` entries, `SettingType` metadata, and runtime values above raw preferences. `AccessibilityProfile` carries accessibility choices such as font scale, contrast, motion, captions, and screen-reader labels.
- `TimelineSequence`, `TimelineStep`, and `TimelinePlayer` provide deterministic timing primitives with `TimelineStatus` state that can drive UI, display, text, or audio adapters.
- `DebugRegistry` collects `DebugSnapshot` values from `DebugInspector` callbacks for application-owned developer menus or test screens.
- `ImportValidationReport`, `ImportValidationIssue`, `ImportIssueSeverity`, and `GenericValidation` provide shared helper types for import summaries, missing-reference diagnostics, and known-ID validation.

These modules intentionally store IDs, metadata, and reusable state only. Concrete content, progression rules, screen design, and save-file schemas should stay in application repositories.

Example/demo code:
- [`examples/user-manual/09-game-support-state-save-prefs-random/GenericSupportModulesDemo.java`](../examples/user-manual/09-game-support-state-save-prefs-random/GenericSupportModulesDemo.java)
- [`examples/user-manual/09-game-support-state-save-prefs-random/GenericStateSystemsDemo.java`](../examples/user-manual/09-game-support-state-save-prefs-random/GenericStateSystemsDemo.java)

### State

Use `GameStateFactory` to create base `GameState` instances. `GameState` currently stores the startup route and a reusable `DialogHistory` instance for per-save conversation review. Keep project-specific state fields and schemas in the application repository unless they are represented by reusable engine abstractions.

### Save/load

Use `SaveLoadService` for reusable save-slot workflows. It supports slot summaries and JSON persistence behavior suitable for engine-level tests and extension by application code. `SaveLoadSummaryScreen` and `SaveLoadSummaryViewModel` expose the current save schema version and configured save directory as reusable diagnostic UI data. Use `SaveSnapshotCodec` and `SaveSnapshotSection` when an application wants to compose engine-owned state slices, such as scene-flow progress, into its own save document; the application still owns the outer save schema and any project-specific state fields.

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
- [`examples/user-manual/09-game-support-state-save-prefs-random/category-code-tables.demo.json`](../examples/user-manual/09-game-support-state-save-prefs-random/category-code-tables.demo.json)
- [`examples/user-manual/09-game-support-state-save-prefs-random/map-text.demo.json`](../examples/user-manual/09-game-support-state-save-prefs-random/map-text.demo.json)
- [`examples/user-manual/09-game-support-state-save-prefs-random/location-text-town.demo.json`](../examples/user-manual/09-game-support-state-save-prefs-random/location-text-town.demo.json)

## 10. Text and utility helpers

Use `TextTagParser` to tokenize visual-novel-style text with simple styling metadata. Parsed output is represented through `TextToken`, `TextTokenType`, and `TextStyle`.

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
