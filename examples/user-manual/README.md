# User Manual Examples

These examples mirror the sections in `docs/USER_MANUAL.md`. Each entry below says what it shows and what output to expect when it is run from the test screen or manually.

| Example | What it shows | Expected result |
| --- | --- | --- |
| `02-project-setup-and-validation/demo.sh` | Project validation commands, including the Gradle wrapper build. | Prints the validation commands and exits successfully on macOS/Linux, or on Windows when `bash.exe` is available on `PATH`. |
| `03-module-and-package-layout/ModuleUsageExample.java` | How application modules can register engine services. | Prints the registered module/package summary and exits with code 0. |
| `04-startup-and-service-wiring/BootstrapDemo.java` | Bootstrapping core services and startup routing. | Prints a successful bootstrap report and validated route information. |
| `04-startup-and-service-wiring/ApplicationResourceConfigDemo.java` | Resolving authored resource paths from `config.demo.json`. | Prints resolved category table, image root, scene and display definitions, and exported config paths. |
| `04-startup-and-service-wiring/config.demo.json` | Sample resource configuration consumed by the startup demos. | Loaded by `ApplicationResourceConfigDemo.java`; it is not run directly. |
| `05-content-routing-and-scenes/SceneFlowDemo.java` | Registering content, scenes, choices, and routes. | Prints the registered scene and route IDs. |
| `05-content-routing-and-scenes/SceneExecutionAndJsonDemo.java` | Resolving scene JSON path, loading definitions, restoring flow state, and wrapping scene progress as a save snapshot section. | Prints the scene execution transcript, scene-flow save section metadata, and JSON round-trip length. |
| `05-content-routing-and-scenes/scene-definitions.demo.json` | Sample scene definitions consumed by the scene execution demo. | Loaded by `SceneExecutionAndJsonDemo.java`; it is not run directly. |
| `06-ui-screens-and-themes/UiScreenDemo.java` | Creating UI screen view models and theme references. | Prints screen model details and expected theme metadata. |
| `06-ui-screens-and-themes/UiScreenCatalogDemo.java` | Registering and listing screen catalog entries. | Prints registered screen IDs and catalog metadata. |
| `07-display-support/display-definitions.demo.json` | Sample display definitions consumed by the display demo. | Loaded by `DisplaySupportDemo.java`; it is not run directly. |
| `07-display-support/DisplaySupportDemo.java` | Resolving and loading display definitions, then resolving image display metadata. | Prints display definition IDs and image lookup details. |
| `08-audio-support/AudioServiceDemo.java` | Audio channel registration and effective volume calculations. | Prints channel and volume state without playing real audio. |
| `09-game-support-state-save-prefs-random/SupportServicesDemo.java` | Game state, save/load, preferences, random, and clock support. | Prints the saved state, preference, random, and clock demonstration values. |
| `09-game-support-state-save-prefs-random/CategoryCodeTableDefinitionDemo.java` | Resolving and loading localized category code table definitions. | Prints language, category, and code lookup information. |
| `09-game-support-state-save-prefs-random/category-code-tables.demo.json` | Sample category code tables consumed by support demos. | Loaded by category/support demos; it is not run directly. |
| `10-text-and-utility-helpers/TextAndUtilityDemo.java` | Text interpolation, validation, and path helper utilities. | Prints rendered text and utility helper results. |
| `11-extension-boundaries/ApplicationRouteModuleDemo.java` | Application extension boundaries for custom routes. | Prints registered route descriptors and extension metadata. |

These files are reference snippets for application authors. They live outside `src/main` and `src/test`, so they are not compiled by the Gradle build.
