# Change summary: USER_MANUAL section 4 + demo + management-screen cleanup

Follow-up to #62 and #63. Brings the documentation, examples, and management screen in line with the registry-driven configuration model.

## USER_MANUAL section 4

- Tightened the `BootstrapOptions` overview to call out that the registry is built before any boot phase and exposed on `BootContext`.
- Replaced the legacy "external `config.json`" subsection with one that documents the new schema: `resourceRoots` map keyed by `ResourceCategory`, app roots layered before library roots, classpath vs filesystem root specs, and the `resources` map for named single-file overrides.
- Reframed the recommended `resources/json/` layout as a mapping onto `resourceRoots.support` / `resourceRoots.ui`.
- Rewrote the `config/app-load.json` discussion: discovery is now `support/config/app-load.json` via the registry; `path` is interpreted as a relative key under `support`.
- Added a code snippet showing how to read a category code table at startup with `BootContext.resourceRegistry().require(ResourceCategory.SUPPORT, ...)` plus a note that all four engine JSON loaders (`CategoryCodeTableDefinition.load`, `SceneDefinitionJson.load`, `ConversationDefinitionJson.load`, `DisplayDefinitionJsonLoader.loadInto`) accept `URL` alongside `Path`.
- Removed the bullet list of removed helpers (`resolveCategoryCodeTables`, `resolveImageAssetRoot`, `resolveJsonResourceRoot`).
- Updated the management-screen example JSON block to drop the deleted `categoryCodeTablesPath` / `imageAssetRoot` / `resources.jsonResourceRoot` keys and add a `resourceRoots` example.

## Examples

- `BootstrapDemo`: replaced the `resolveJsonResourceRoot` print with `resourceConfig.resourceRoots(SUPPORT)`.
- `DisplaySupportDemo`: now constructs `ImageDisplayRegistry(ResourceRegistry)` and resolves an asset URL through `registry.find(ResourceCategory.IMAGES, ...)` instead of the deleted `GameAssetLocator(repoRoot, imageAssetRoot)` and `resolveImageAssetRoot` paths.
- `CategoryCodeTableDefinitionDemo`: looks up the demo table by SUPPORT key through the registry and uses the new `CategoryCodeTableDefinition.load(URL)` overload.

## Management screen (`DefaultDisplayValuesApplication`)

- Dropped the dead `categoryCodeTablesPath` / `imageAssetRoot` / `resources.jsonResourceRoot` cases from `applicationConfigFieldLabel`.
- Removed the bespoke `DIRECTORY` editor branches keyed off the deleted field names; the remaining `Path/Image/resources.*` heuristic continues to mark file-style fields.
- `applicationConfigFields()` skips `resourceRoots` (rendered separately by the registry surface) plus any `_comment` documentation key in the bundled config.

## Tests

- Updated `DefaultDisplayValuesApplicationTest` assertions for the new label list, the migrated intro-text resource path, and the removed legacy field keys.
- `./gradlew --no-daemon compileJava testClasses` and the `com.eb.javafx.resources.*` / `com.eb.javafx.bootstrap.*` / `com.eb.javafx.testscreen.DefaultDisplayValuesApplicationTest` test set are green.
