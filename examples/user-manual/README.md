# User Manual Examples

These examples mirror the sections in `docs/USER_MANUAL.md`. Each entry below says what it shows and what output to expect when it is run from the test screen or manually.

App-authored JSON examples live under `../resources/json` with type subdirectories such as `config`, `app-load`, `display`, `scenes`, `conversations`, `screens`, `code-tables`, `map-text`, and `location-text`.

| Example | What it shows | Expected result |
| --- | --- | --- |
| `02-project-setup-and-validation/demo.sh` | Project validation commands, including the Gradle wrapper build. | Prints the validation commands and exits successfully on macOS/Linux, or on Windows when `bash.exe` is available on `PATH`. |
| `03-module-and-package-layout/ModuleUsageExample.java` | How application modules can register engine services. | Prints the registered module/package summary and exits with code 0. |
| `04-startup-and-service-wiring/BootstrapDemo.java` | Bootstrapping core services and startup routing from `config.demo.json` plus `app-load/app-load.json`. | Prints a successful bootstrap report, route information, and resolved JSON resource paths. |
| `04-startup-and-service-wiring/ApplicationResourceConfigDemo.java` | Resolving authored resource paths from `config.demo.json` and reading `app-load/app-load.json`. | Prints resolved category table, image root, JSON resource root, app-load entries, scene/display definitions, and exported config paths. |
| `../resources/json/config/config.demo.json` | Sample resource configuration consumed by the startup demos. | Loaded by `ApplicationResourceConfigDemo.java`; it is not run directly. |
| `../resources/json/app-load/app-load.json` | Sample startup load declaration for JSON resource directories. | Loaded by `ApplicationResourceConfigDemo.java` and `BootstrapOptions.fromConfig(...)`; it is not run directly. |
| `05-content-routing-and-scenes/SceneFlowDemo.java` | Registering content, scenes, choices, and routes. | Prints the registered scene and route IDs. |
| `05-content-routing-and-scenes/SceneExecutionAndJsonDemo.java` | Resolving scene JSON path, loading definitions, restoring flow state, and wrapping scene progress as a save snapshot section. | Prints the scene execution transcript, scene-flow save section metadata, and JSON round-trip length. |
| `05-content-routing-and-scenes/SceneValidationAndSaveDemo.java` | Scene graph validation/reporting and composing scene-flow state into a save snapshot document. | Prints graph summary, validation warnings, and restored scene-flow state. |
| `../resources/json/scenes/scene-definitions.demo.json` | Sample scene definitions consumed by the scene execution demo. | Loaded by `SceneExecutionAndJsonDemo.java`; it is not run directly. |
| `06-ui-screens-and-themes/UiScreenDemo.java` | Creating UI screen view models and theme references. | Prints screen model details and expected theme metadata. |
| `06-ui-screens-and-themes/UiScreenCatalogDemo.java` | Registering and listing screen catalog entries. | Prints registered screen IDs and catalog metadata. |
| `../resources/json/screens/sample-screen-design.json` | Minimal form-style screen design JSON with one block and saved items; fixed text is stored in the sibling `_text.json` file. | Loaded by the screen designer or `ScreenDesignJson.load(...)`; it is not run directly. |
| `../resources/json/screens/main-menu-screen-design.json` | Menu/action-list screen design JSON showing button-style items; fixed text is stored in the sibling `_text.json` file. | Loaded by the screen designer or `ScreenDesignJson.load(...)`; it is not run directly. |
| `../resources/json/screens/quest-log-screen-design.json` | Multi-block screen design JSON for a richer content layout example; fixed text is stored in the sibling `_text.json` file. | Loaded by the screen designer or `ScreenDesignJson.load(...)`; it is not run directly. |
| `../resources/json/screens/gallery-preview-screen-design.json` | Preview/card-oriented screen design JSON example; fixed text is stored in the sibling `_text.json` file. | Loaded by the screen designer or `ScreenDesignJson.load(...)`; it is not run directly. |
| `../resources/json/display/display-definitions.demo.json` | Sample display definitions consumed by the display demo. | Loaded by `DisplaySupportDemo.java`; it is not run directly. |
| `07-display-support/DisplaySupportDemo.java` | Resolving and loading display definitions, then resolving image display metadata. | Prints display definition IDs and image lookup details. |
| `08-audio-support/AudioServiceDemo.java` | Audio channel registration and effective volume calculations. | Prints channel and volume state without playing real audio. |
| `08-audio-support/AudioAdapterPolicyDemo.java` | Audio adapter expectations for asset discovery, fades, crossfades, preload, and player pools. | Prints resolved media URI and adapter policy metadata without playing real audio. |
| `09-game-support-state-save-prefs-random/SupportServicesDemo.java` | Game state, save/load, preferences, random, and clock support. | Prints the saved state, preference, random, and clock demonstration values. |
| `09-game-support-state-save-prefs-random/CategoryCodeTableDefinitionDemo.java` | Resolving and loading localized category code table definitions. | Prints language, category, and code lookup information. |
| `09-game-support-state-save-prefs-random/GenericSupportModulesDemo.java` | Localization, asset catalogs, input maps, event history, progress snapshots, settings, accessibility, and diagnostics. | Prints localized lookup, asset validation, input resolution, event history, progress save metadata, settings, accessibility, and diagnostic summary values. |
| `09-game-support-state-save-prefs-random/GenericStateSystemsDemo.java` | Inventory, characters, relationships, journal state, timeline playback, and debug snapshots. | Prints item quantity, character/relationship values, journal read state, timeline status, and reusable debug rows. |
| `../resources/json/code-tables/category-code-tables.demo.json` | Sample category code tables consumed by support demos. | Loaded by category/support demos; it is not run directly. |
| `../resources/json/map-text/map-text.demo.json` | Sample localized map text definitions with default descriptions. | Loaded by `MapTextDefinition.load(...)`; it is not run directly. |
| `../resources/json/location-text/location-text-town.demo.json` | Sample localized location text variants for a map, including condition strings and `mapId.locId` references. | Loaded by `LocationTextDefinition.load(...)`; it is not run directly. |
| `10-text-and-utility-helpers/TextAndUtilityDemo.java` | Text interpolation plus validation, path, and related utility helpers. | Prints rendered text and utility helper results. |
| `11-extension-boundaries/ApplicationRouteModuleDemo.java` | Application extension boundaries for custom routes. | Prints registered route descriptors and extension metadata. |
| `12-application-shell/GameApplicationDemo.java` | The first application-owned JavaFX launcher built on `BootstrapOptions` and `BootstrapService`. | Boots app-owned modules, auto-loads configured JSON directories from `app-load/app-load.json`, opens an application route, and reports the configured media adapter class. |
| `12-application-shell/JavaFxAudioPlaybackAdapterDemo.java` | A concrete application-side `AudioPlaybackAdapter` using JavaFX media APIs. | Shows how validated playback commands map to `MediaPlayer`/`AudioClip` instances per channel. |

These files are reference snippets for application authors. They live outside `src/main` and `src/test`, so they are not compiled by the Gradle build.
